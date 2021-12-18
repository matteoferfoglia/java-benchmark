package benchmark;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
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
     * @throws ClassNotFoundException    If the class containing the method (including
     *                                   the methods to be executed before and after
     *                                   each iteration) is not found.
     * @throws NoSuchMethodException     If the method (including the methods to be executed
     *                                   before and after each iteration) is not found.
     */
    public BenchmarkInstance(Method methodToBenchmark) throws InvocationTargetException, IllegalAccessException, ClassNotFoundException, NoSuchMethodException {
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

        @Nullable
        Method toBeExecutedBeforeEachIteration = getMethodFromName(annotationOfMethod.beforeEach());
        @Nullable
        Method toBeExecutedAfterEachIteration = getMethodFromName(annotationOfMethod.afterEach());

        List<Long> executionTimesInNanos = benchmarkAndGetExecutionTimesForStatistics(
                methodToBenchmark, toBeExecutedBeforeEachIteration, toBeExecutedAfterEachIteration);

        durationOfFastestExecutionInNanoseconds = executionTimesInNanos.stream().reduce((a, b) -> a < b ? a : b).orElseThrow(NoSuchElementException::new);
        durationOfSlowestExecutionInNanoseconds = executionTimesInNanos.stream().reduce((a, b) -> a > b ? a : b).orElseThrow(NoSuchElementException::new);
        averageDurationOfEachExecutionInNanoseconds = executionTimesInNanos.stream().reduce(Long::sum).orElseThrow(NoSuchElementException::new) / executionTimesInNanos.size();
        testEndedAt = Instant.now();
    }

    /**
     * @param methodName The canonical name of a method, starting with the class name
     *                   and without neither parenthesis nor return type nor parameters.
     * @return the {@link Method} or null if the input parameter is blank.
     * The eventually non-null returned method is already made accessible (e.g., in case of
     * private methods).
     * @throws ClassNotFoundException If there are problems with the class name.
     * @throws NoSuchMethodException  If the specified method name is not blank but does not exist.
     */
    @Nullable
    private Method getMethodFromName(String methodName) throws ClassNotFoundException, NoSuchMethodException {
        @Nullable Method methodToBeInvokedBeforeEachIteration;
        if (methodName.trim().length() > 0) {
            Class<?> classContainingTheMethod = Class.forName(methodName
                    .substring(0, methodName.lastIndexOf('.')));
            Method method = classContainingTheMethod
                    .getDeclaredMethod(methodName.substring(methodName.lastIndexOf('.') + 1/*method name starts with the character next to the last dot*/));
            method.setAccessible(true);
            return method;
        } else {
            return null;
        }
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
     * @param beforeEach        The method to be executed before each iteration or null if no methods has to be executed.
     * @param afterEach         The method to be executed after each iteration or null if no methods has to be executed.
     * @return The list of execution times (in nanoseconds) valid for benchmark statistics
     * (warmup and teardown iterations are excluded)
     * @throws InvocationTargetException If errors occur when invoking the method.
     * @throws IllegalAccessException    If errors occur when invoking the method.
     */
    private List<Long> benchmarkAndGetExecutionTimesForStatistics(
            @NotNull Method methodToBenchmark, @Nullable Method beforeEach, @Nullable Method afterEach)
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
                if (beforeEach != null) {
                    beforeEach.invoke(null);
                }
                startTime = System.nanoTime();
                methodToBenchmark.invoke(null);
                endTime = System.nanoTime();
                executionTimesInNanoseconds.add(endTime - startTime);
                if (afterEach != null) {
                    afterEach.invoke(null);
                }
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