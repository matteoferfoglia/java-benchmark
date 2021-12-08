package benchmark;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BenchmarkRunnerTest {

    private static final PrintStream realStdErr = System.err;
    private static final PrintStream realStdOut = System.out;
    private ByteArrayOutputStream fakeStdErr;
    private final static ByteArrayOutputStream fakeStdOut = new ByteArrayOutputStream();

    private static Stream<Method> methodSupplier() {
        return Arrays.stream(ClassWithDummyMethodsForTestingPurposes.class.getDeclaredMethods());
    }

    private static Stream<Arguments> nonStaticMethodsSupplier() {
        return methodSupplier()
                .filter(method -> !Modifier.isStatic(method.getModifiers()))
                .map(Arguments::of);
    }

    private static Stream<Arguments> withParametersMethodsSupplier() {
        return methodSupplier()
                .filter(method -> method.getParameterCount() > 0)
                .map(Arguments::of);
    }

    private static Stream<Arguments> staticAndWithoutParamsMethodsSupplier() {
        return methodSupplier()
                .filter(method -> Modifier.isStatic(method.getModifiers()) && method.getParameterCount() == 0)
                .map(Arguments::of);
    }

    @BeforeAll
    static void dontUseStdOut() {
        System.setOut(new PrintStream(fakeStdOut));
    }

    @AfterAll
    static void restoreStdOut() {
        System.setOut(realStdOut);
    }

    @BeforeEach
    void setUp() {
        fakeStdErr = new ByteArrayOutputStream();
        System.setErr(new PrintStream(fakeStdErr));
    }

    @AfterEach
    void tearDown() {
        System.setErr(realStdErr);
    }

    @ParameterizedTest
    @MethodSource("nonStaticMethodsSupplier")
    void signalErrorIfTryingToBenchmarkNonStaticMethod(Method method) {
        new BenchmarkRunner().benchmarkAllAnnotatedMethodsAndGetListOfResults();
        assertTrue(fakeStdErr.toString().contains(method.toString()));
    }

    @ParameterizedTest
    @MethodSource("withParametersMethodsSupplier")
    void signalErrorIfTryingToBenchmarkMethodWithParameters(Method method) {
        new BenchmarkRunner().benchmarkAllAnnotatedMethodsAndGetListOfResults();
        assertTrue(fakeStdErr.toString().contains(method.toString()));
    }

    @ParameterizedTest
    @MethodSource("staticAndWithoutParamsMethodsSupplier")
    void benchmarkIfTryingToBenchmarkStaticMethodWithoutParameters(Method method) {
        new BenchmarkRunner().benchmarkAllAnnotatedMethodsAndGetListOfResults();
        assertFalse(fakeStdErr.toString().contains(method.toString()));
    }

}

