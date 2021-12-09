package benchmark;

import utils.StringUtility;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.time.Instant;
import java.util.*;
import java.util.function.Predicate;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Class to save the result of a benchmark test.
 */
@SuppressWarnings({"FieldCanBeLocal", "unused"})    // accessed with reflections
public class BenchmarkInstance implements Comparable<BenchmarkInstance> {

    /**
     * The method under benchmarking.
     */
    private final Method testedMethod;
    /**
     * {@link Instant} at which the test begun.
     */
    private final Instant testStartedAt;
    /**
     * {@link Instant} at which the test ended.
     */
    private final Instant testEndedAt;
    /**
     * Number of warm up iterations, excluded from statistics.
     */
    private final int warmUpIterationsExcludedFromBenchmarkStatistics;
    /**
     * Number of iterations for statistics.
     */
    private final int iterationsOfTest;
    /**
     * Number of tear down iterations, excluded from statistics.
     */
    private final int tearDownIterationsExcludedFromBenchmarkStatistics;
    /**
     * Duration of the fastest execution.
     */
    private final long durationOfFastestExecutionInNanoseconds;
    /**
     * Duration of the slowest execution.
     */
    private final long durationOfSlowestExecutionInNanoseconds;
    /**
     * Average duration of each execution.
     */
    private final long averageDurationOfEachExecutionInNanoseconds;
    /**
     * Comment to print in the report of this instance.
     */
    private final String commentToReport;

    /**
     * Constructor.
     *
     * @param methodToBenchmark The method to be benchmarked.
     * @throws InvocationTargetException If errors occur when invoking the method.
     * @throws IllegalAccessException    If errors occur when invoking the method.
     */
    public BenchmarkInstance(Method methodToBenchmark) throws InvocationTargetException, IllegalAccessException {
        testStartedAt = Instant.now();
        testedMethod = Objects.requireNonNull(methodToBenchmark);
        Benchmark annotationOfMethod = methodToBenchmark.getAnnotation(Benchmark.class);

        warmUpIterationsExcludedFromBenchmarkStatistics = annotationOfMethod.warmUpIterations();
        iterationsOfTest = annotationOfMethod.iterations();
        tearDownIterationsExcludedFromBenchmarkStatistics = annotationOfMethod.tearDownIterations();
        if (warmUpIterationsExcludedFromBenchmarkStatistics < 0
                || iterationsOfTest < 0
                || tearDownIterationsExcludedFromBenchmarkStatistics < 0) {
            throw new IllegalArgumentException("Number of iterations cannot be negative.");
        }
        commentToReport = annotationOfMethod.commentToReport().length() > 0 ? annotationOfMethod.commentToReport() : null;

        List<Long> executionTimesInNanos = benchmarkAndGetExecutionTimesForStatistics(methodToBenchmark);
        durationOfFastestExecutionInNanoseconds = executionTimesInNanos.stream().reduce((a, b) -> a < b ? a : b).orElseThrow(NoSuchElementException::new);
        durationOfSlowestExecutionInNanoseconds = executionTimesInNanos.stream().reduce((a, b) -> a > b ? a : b).orElseThrow(NoSuchElementException::new);
        averageDurationOfEachExecutionInNanoseconds = executionTimesInNanos.stream().reduce(Long::sum).orElseThrow(NoSuchElementException::new) / executionTimesInNanos.size();
        testEndedAt = Instant.now();
    }

    @Override
    public String toString() {
        Predicate<Field> fieldWithNonNullValue = field -> {
            try {
                return field.get(this) != null;
            } catch (IllegalAccessException e) {
                return false;
            }
        };
        return "BENCHMARK SUMMARY" + System.lineSeparator() + "\t"
                + Arrays.stream(getClass().getDeclaredFields())
                .peek(field -> field.setAccessible(true))
                .filter(fieldWithNonNullValue)
                .map(field -> {
                    try {
                        String fieldNameHumanReadable = StringUtility.toUpperCaseOnlyTheFirstChar(
                                StringUtility.splitCamelCase(field.getName()));
                        return fieldNameHumanReadable + ": " + field.get(this);
                    } catch (IllegalAccessException e) {
                        Logger.getLogger(getClass().getCanonicalName()).log(Level.SEVERE, e.getMessage(), e);
                        return "";
                    }
                })
                .collect(Collectors.joining(System.lineSeparator() + "\t"))
                + System.lineSeparator();
    }

    /**
     * Perform the benchmark tests.
     *
     * @param methodToBenchmark The method to be benchmarked.
     * @return The list of execution times (in nanoseconds) valid for benchmark statistics
     * (warmup and teardown iterations are excluded)
     * @throws InvocationTargetException If errors occur when invoking the method.
     * @throws IllegalAccessException    If errors occur when invoking the method.
     */
    private List<Long> benchmarkAndGetExecutionTimesForStatistics(Method methodToBenchmark)
            throws InvocationTargetException, IllegalAccessException {
        PrintStream realStdOut = System.out;
        PrintStream realStdErr = System.err;
        System.setOut(new PrintStream(new ByteArrayOutputStream())); // ignore stdout during benchmark
        System.setErr(new PrintStream(new ByteArrayOutputStream())); // ignore stderr during benchmark
        List<Long> executionTimesInNanoseconds = new ArrayList<>();
        long startTime;
        long endTime;
        try {
            for (int i = 0; i < warmUpIterationsExcludedFromBenchmarkStatistics + iterationsOfTest + tearDownIterationsExcludedFromBenchmarkStatistics; i++) {
                startTime = System.nanoTime();
                methodToBenchmark.invoke(null);
                endTime = System.nanoTime();
                executionTimesInNanoseconds.add(endTime - startTime);
            }
        } finally {
            System.setOut(realStdOut); // restore stdout
            System.setErr(realStdErr); // restore stderr
        }
        return executionTimesInNanoseconds.subList(
                warmUpIterationsExcludedFromBenchmarkStatistics,
                warmUpIterationsExcludedFromBenchmarkStatistics + iterationsOfTest);
    }

    /**
     * Getter for {@link #testedMethod}.
     *
     * @return the current {@link #testedMethod}.
     */
    public Method getTestedMethod() {
        return testedMethod;
    }

    @Override
    public int compareTo(BenchmarkInstance o) {
        return testedMethod.toString().compareTo(o.testedMethod.toString());
    }
}