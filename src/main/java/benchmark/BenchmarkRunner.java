package benchmark;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
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
     * List of benchmark results.
     */
    private List<BenchmarkInstance> results = new ArrayList<>();    // initialized to empty list

    /**
     * {@link Instant} at which this test started (not created, but started).
     */
    private Instant startTimeOfTests;   // null if tests is not started

    /**
     * @return true if this instance of test is started, false otherwise.
     */
    private boolean isTestStarted() {
        return startTimeOfTests != null;
    }

    @Override
    public String toString() {
        return "===================================================================================" + System.lineSeparator() +
                "====================             BENCHMARK SUMMARY             ====================" + System.lineSeparator() +
                "===================================================================================" + System.lineSeparator() + System.lineSeparator() +

                (isTestStarted() ?
                        results.size() + " methods benchmarked" + System.lineSeparator() + System.lineSeparator() +
                                "Test started at: " + startTimeOfTests + System.lineSeparator() + System.lineSeparator() +
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
     * Benchmarks all methods in the project annotated with {@link Benchmark}.
     *
     * @return The list with results.
     */
    public List<BenchmarkInstance> benchmarkAllAnnotatedMethodsAndGetListOfResults() {
        startTimeOfTests = Instant.now();
        results = getAllMethodsWithAnnotationInPackage(Benchmark.class)
                .stream()
                .map(method -> {
                    Throwable eventuallyThrown = null;
                    StringBuilder eventuallyErrorMessage = new StringBuilder();
                    try {
                        try {
                            return new BenchmarkInstance(method);
                        } catch (NullPointerException e) {
                            eventuallyThrown = e;
                            eventuallyErrorMessage.append(ERROR_MESSAGE_IF_TRYING_TO_BENCHMARK_NOT_STATIC_METHOD);
                            return null;
                        } catch (IllegalArgumentException e) {
                            eventuallyThrown = e;
                            eventuallyErrorMessage.append(ERROR_MESSAGE_IF_TRYING_TO_BENCHMARK_METHOD_WITH_PARAM);
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
        return results;
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
     * @param annotation The class of the annotation.
     * @return all methods in the given package annotated with the given annotation.
     */
    private static List<Method> getAllMethodsWithAnnotationInPackage(
            Class<? extends Annotation> annotation) {
        return getAllClasses()
                .stream().unordered().parallel()
                .flatMap(aClass -> Arrays.stream(aClass.getDeclaredMethods()))
                .filter(method -> method.isAnnotationPresent(annotation))
                .peek(method -> method.setAccessible(true))
                .collect(Collectors.toList());
    }

    /**
     * Scans all classes accessible from the context class loader which belong
     * to the given package and subpackages.
     *
     * @return The classes
     */
    private static List<Class<?>> getAllClasses() {
        List<Class<?>> classes = new ArrayList<>();
        for (File directory : Objects.requireNonNull(getRootDirectoryOfProject().listFiles())) {
            try {
                classes.addAll(findClasses(directory));
            } catch (IOException e) {
                logSevereInLoggerOfThisClass(e);
            }
        }
        return classes;
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
    private static List<Class<?>> findClasses(File directory) throws IOException {
        List<Class<?>> classes = new ArrayList<>();
        if (!directory.exists()) {
            return classes;
        }
        final String rootOfProjectDirectoryOfClasses = getRootDirectoryOfProject().getCanonicalPath();
        String classExtension = ".class";
        if (directory.isDirectory()) {
            for (File file : Objects.requireNonNull(directory.listFiles())) {
                classes.addAll(findClasses(file));
            }
        } else {
            @SuppressWarnings("UnnecessaryLocalVariable") File file = directory;  // it is a file and not a directory
            if (file.getName().endsWith(classExtension) && !file.getName().startsWith("module-info")) {
                try {
                    final String ESCAPED_FILE_SEPARATOR = "\\" + File.separator;    // correctly escaped
                    classes.add(
                            Class.forName(
                                    file.getCanonicalPath()
                                            .substring(rootOfProjectDirectoryOfClasses.length() + 1,
                                                    file.getCanonicalPath().length() - classExtension.length())
                                            .replace("target" + File.separator + "classes" + File.separator, "")        // remove folder names till the project classes
                                            .replace("target" + File.separator + "test-classes" + File.separator, "")   // remove folder names till the project classes
                                            .replaceAll(ESCAPED_FILE_SEPARATOR, ".")
                            )
                    );
                } catch (ClassNotFoundException ignored) {
                    // When production classes try to use test classes
                }
            }
        }
        return classes;
    }

}