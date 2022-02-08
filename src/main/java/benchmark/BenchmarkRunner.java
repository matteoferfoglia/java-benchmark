package benchmark;

import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Class with methods for benchmarking.
 * All methods annotated with {@link Benchmark} will be benchmarked.
 */
public class BenchmarkRunner {

    /**
     * The {@link Logger} of current class.
     */
    private static final Logger LOGGER_OF_THIS_CLASS =
            Logger.getLogger(BenchmarkRunner.class.getCanonicalName());
    /**
     * The error message showed when trying to benchmark a non-static method.
     */
    private static final String ERROR_MESSAGE_IF_TRYING_TO_BENCHMARK_NOT_STATIC_METHOD =
            "Only static methods allowed for benchmarking.";
    /**
     * The error message showed when trying to benchmark a method with parameters.
     */
    private static final String ERROR_MESSAGE_IF_TRYING_TO_BENCHMARK_METHOD_WITH_PARAM =
            "Methods with parameters are not allowed for benchmarking.";
    /**
     * The error message showed when there are problems with the methods to be executed
     * before/after each iteration.
     */
    private static final String ERROR_MESSAGE_IF_PROBLEMS_WITH_METHODS_TO_BE_EXECUTED_BEFORE_OR_AFTER_EACH_ITERATION =
            "Problems with methods to be executed before or after each iteration.";
    /**
     * Flag set to true if the progress of benchmarking must be printed
     * to {@link System#out}, false otherwise. Default value is false.
     */
    private final boolean printProgress;
    /**
     * List of benchmark results.
     */
    private List<BenchmarkInstance> results = new ArrayList<>();    // initialized to empty list
    /**
     * {@link Instant} at which this test started (not created, but started).
     */
    private Instant startTimeOfTests;   // null if test is not started
    /**
     * {@link Instant} at which this test ended.
     */
    private Instant endTimeOfTests;   // null if test is not ended

    /**
     * Default constructor. Progress of benchmarking will not be printed
     * to {@link System#out}.
     * See {@link #BenchmarkRunner(boolean)}.
     */
    public BenchmarkRunner() {
        this(false);
    }

    /**
     * Constructor.
     *
     * @param printProgress true if the progress of benchmarking must be printed
     *                      to {@link System#out}, false otherwise.
     */
    public BenchmarkRunner(boolean printProgress) {
        this.printProgress = printProgress;
    }

    /**
     * Log as {@link Level#SEVERE} the given {@link Throwable} in the {@link Logger} of this class.
     *
     * @param e The throwable instance to be logged.
     */
    private static void logSevereInLoggerOfThisClass(final Throwable e) {
        logSevereInLoggerOfThisClass(e, e.getMessage() == null ? "" : e.getMessage());
    }

    /**
     * Log as {@link Level#SEVERE} the given {@link Throwable} in the {@link Logger} of this class.
     *
     * @param e            The throwable instance to be logged.
     * @param errorMessage The custom error message to log.
     */
    private static void logSevereInLoggerOfThisClass(@NotNull final Throwable e, @NotNull final String errorMessage) {
        LOGGER_OF_THIS_CLASS.log(
                Level.SEVERE, Objects.requireNonNull(errorMessage) +
                        System.lineSeparator() +
                        Arrays.stream(Objects.requireNonNull(e).getStackTrace())
                                .map(StackTraceElement::toString)
                                .map(string -> "\t" + string)
                                .collect(Collectors.joining(System.lineSeparator())),
                e);
    }

    /**
     * Scans all classes accessible from the context class loader which belong
     * to the given package and subpackages.
     *
     * @return The classes
     */
    private static List<String> getAllClassNames() {
        List<String> classNames = new ArrayList<>();
        for (File directory : Objects.requireNonNull(getRootDirectoryOfProject().listFiles())) {
            try {
                classNames.addAll(findClassNames(directory));
            } catch (IOException e) {
                logSevereInLoggerOfThisClass(e);
            }
        }
        return classNames;
    }

    /**
     * Get the root of the project.
     *
     * @return The file corresponding to the root directory of classes (*.class)
     */
    private static File getRootDirectoryOfProject() {
        return new File(System.getProperty("user.dir"));
    }

    /**
     * Recursive method used to find all classes in a given directory and
     * subdirectories.
     *
     * @param directory The base directory
     * @return {@link List} of classes
     * @throws IOException if I/O errors occur.
     */
    private static List<String> findClassNames(File directory) throws IOException {
        List<String> classNames = new ArrayList<>();
        if (!directory.exists()) {
            return classNames;
        }
        final String rootOfProjectDirectoryOfClasses = getRootDirectoryOfProject().getCanonicalPath();
        String classExtension = ".class";
        if (directory.isDirectory()) {
            for (File file : Objects.requireNonNull(directory.listFiles())) {
                classNames.addAll(findClassNames(file));
            }
        } else {
            @SuppressWarnings("UnnecessaryLocalVariable") File file = directory;  // it is a file and not a directory
            if (file.getName().endsWith(classExtension) && !file.getName().startsWith("module-info")) {
                try {
                    final String ESCAPED_FILE_SEPARATOR = "\\" + File.separator;    // correctly escaped
                    classNames.add(
                            Class.forName(
                                    file.getCanonicalPath()
                                            .substring(rootOfProjectDirectoryOfClasses.length() + 1,
                                                    file.getCanonicalPath().length() - classExtension.length())
                                            .replace("target" + File.separator + "classes" + File.separator, "")        // remove folder names till the project classes
                                            .replace("target" + File.separator + "test-classes" + File.separator, "")   // remove folder names till the project classes
                                            .replaceAll(ESCAPED_FILE_SEPARATOR, "."),
                                    false,  // if false: avoid expensive time-consuming class initialization (e.g., static blocks)
                                    BenchmarkRunner.class.getClassLoader()
                            ).getCanonicalName());
                } catch (ClassNotFoundException ignored) {
                }
            }
        }
        return classNames;
    }

    /**
     * @return true if this instance of test is started, false otherwise.
     */
    private boolean isTestStarted() {
        return startTimeOfTests != null;
    }

    /**
     * @return true if this instance of test is ended, false otherwise.
     */
    private boolean isTestEnded() {
        return endTimeOfTests != null;
    }

    /**
     * @return true if this instance of test is started, false otherwise.
     * @throws IllegalStateException if test is not started or ended.
     */
    private String getTestDuration() {
        if (!isTestStarted()) {
            throw new IllegalStateException("Test not started.");
        }
        if (!isTestEnded()) {
            throw new IllegalStateException("Test not ended.");
        }
        Duration duration = Duration.between(startTimeOfTests, endTimeOfTests);
        long totalDurationInMilliseconds = duration.toMillis();

        // conversion to HH mm ss millis
        final double HOURS_TO_MILLIS_FACTOR = 60 * 60 * 1000.0;
        final double MINUTES_TO_MILLIS_FACTOR = 60 * 1000.0;
        final double SECONDS_TO_MILLIS_FACTOR = 1000.0;
        long remainingMillis = totalDurationInMilliseconds;
        final long HOURS = (long) (remainingMillis / HOURS_TO_MILLIS_FACTOR);
        remainingMillis -= HOURS * HOURS_TO_MILLIS_FACTOR;
        final long MINUTES = (long) (totalDurationInMilliseconds / MINUTES_TO_MILLIS_FACTOR);
        remainingMillis -= MINUTES * MINUTES_TO_MILLIS_FACTOR;
        final long SECONDS = (long) (remainingMillis / SECONDS_TO_MILLIS_FACTOR);
        remainingMillis -= SECONDS * MINUTES_TO_MILLIS_FACTOR;
        final long MILLIS = remainingMillis;
        return HOURS + ":" + MINUTES + ":" + SECONDS + "." + MILLIS + "\t(HH:mm:ss.SSS)";
    }

    @Override
    public String toString() {
        return "===================================================================================" + System.lineSeparator() +
                "====================             BENCHMARK SUMMARY             ====================" + System.lineSeparator() +
                "===================================================================================" + System.lineSeparator() +
                System.lineSeparator() +

                (isTestStarted() ?
                        results.size() + " methods benchmarked" + System.lineSeparator() + System.lineSeparator() +
                                "Test started at:\t" + startTimeOfTests + System.lineSeparator() +
                                "Test ended at:\t\t" + endTimeOfTests + System.lineSeparator() +
                                "Test duration:\t\t" + getTestDuration() + System.lineSeparator() +
                                System.lineSeparator() +
                                "Benchmarked method" + (results.size() > 1 ? "s" : "") + ": " + System.lineSeparator() +
                                IntStream.range(0, results.size())
                                        .mapToObj(i -> "\t" + (i + 1) + ")\t" + results.get(i).getTestedMethod())
                                        .collect(Collectors.joining(System.lineSeparator())) + System.lineSeparator() + System.lineSeparator() +
                                "-----------------------------------------------------------------------------------" + System.lineSeparator() +
                                IntStream.range(0, results.size())
                                        .sequential()
                                        .mapToObj(i -> System.lineSeparator() + (i + 1) + ") " + results.get(i).toString())
                                        .collect(Collectors.joining(System.lineSeparator())) :
                        "No test performed.");
    }

    /**
     * Benchmarks all methods in the project annotated with {@link Benchmark}.
     *
     * @return The list with results.
     */
    public List<BenchmarkInstance> benchmarkAllAnnotatedMethodsAndGetListOfResults() {
        startTimeOfTests = Instant.now();
        results = getAllClassNames()
                .stream().sequential()
                .filter(Objects::nonNull)
                .flatMap(className -> {
                            try {
                                return Arrays.stream(Class.forName(className).getDeclaredMethods())
                                        .filter(method -> method.isAnnotationPresent(Benchmark.class))
                                        .peek(method -> method.setAccessible(true));
                            } catch (ClassNotFoundException ignore) {
                                return null;
                            }
                        }
                )
                .filter(Objects::nonNull)
                .peek(method -> System.out.println("Benchmarking method " + method.getName()))
                .map(method -> {
                    Throwable eventuallyThrown = null;
                    StringBuilder eventuallyErrorMessage = new StringBuilder();
                    try {
                        try {
                            return new BenchmarkInstance(method, printProgress ? System.out : null);
                        } catch (NullPointerException e) {
                            eventuallyThrown = e;
                            eventuallyErrorMessage.append(ERROR_MESSAGE_IF_TRYING_TO_BENCHMARK_NOT_STATIC_METHOD);
                            return null;
                        } catch (IllegalArgumentException e) {
                            eventuallyThrown = e;
                            eventuallyErrorMessage.append(ERROR_MESSAGE_IF_TRYING_TO_BENCHMARK_METHOD_WITH_PARAM);
                            return null;
                        } catch (ClassNotFoundException | NoSuchMethodException e) {
                            eventuallyThrown = e;
                            eventuallyErrorMessage.append(ERROR_MESSAGE_IF_PROBLEMS_WITH_METHODS_TO_BE_EXECUTED_BEFORE_OR_AFTER_EACH_ITERATION);
                            return null;
                        } finally {
                            if (eventuallyThrown != null) {
                                System.err.println(eventuallyErrorMessage
                                        .append(System.lineSeparator())
                                        .append("\tInvalid method: ")
                                        .append(method));
                            }
                        }
                    } catch (IllegalAccessException | InvocationTargetException e) {
                        logSevereInLoggerOfThisClass(e);
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .sorted()
                .collect(Collectors.toList());
        endTimeOfTests = Instant.now();
        if (printProgress) {
            System.out.println(System.lineSeparator() + System.lineSeparator());
        }
        return results;
    }

}