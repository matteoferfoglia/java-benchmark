package benchmark;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.lang.reflect.InvocationTargetException;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BenchmarkInstanceTest {

    private BenchmarkInstance benchmarkInstance;

    @BeforeEach
    void setUp() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        ByteArrayOutputStream fakeStdOut = new ByteArrayOutputStream();
        System.setOut(new PrintStream(fakeStdOut));
        benchmarkInstance = new BenchmarkInstance(
                ClassWithDummyMethodsForTestingPurposes.class.getDeclaredMethod(
                        ClassWithDummyMethodsForTestingPurposes.NAME_OF_PUBLIC_STATIC_METHOD_WITHOUT_PARAMETERS));
    }

    @Test
    void testToStringToReturnAsMuchLinesAsTheNumberOfClassFieldsPlus3ForHeadings() {
        final int NUMBER_OF_LINES_OF_HEADING_PROVIDED_BY_TO_STRING_METHOD = 3;
        assertEquals(
                BenchmarkInstance.class.getDeclaredFields().length + NUMBER_OF_LINES_OF_HEADING_PROVIDED_BY_TO_STRING_METHOD,
                benchmarkInstance.toString().split(System.lineSeparator()).length+1/*last empty string is excluded by .split()*/);
    }
}