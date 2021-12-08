package utils;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

class StringUtilityTest {

    @ParameterizedTest
    @CsvSource({
            "foo, foo",
            "Foo, Foo",
            "FooBar, Foo Bar",
            "FOO, FOO",
            "FOOBar, FOO Bar",
            "Foo00, Foo 00",
            "0Foo, 0 Foo"
    })
    void splitCamelCase(String camelCasedInput, String expectedSplit) {
        assertEquals(expectedSplit, StringUtility.splitCamelCase(camelCasedInput));
    }

    @ParameterizedTest
    @CsvSource({
            "Foo, Foo",
            "foo, Foo",
            "FooBar, Foobar",
            "Foo Bar, Foo bar",
            "0Foo, 0foo"
    })
    void toUpperCaseOnlyTheFirstChar(String camelCasedInput, String expectedOutput) {
        assertEquals(expectedOutput, StringUtility.toUpperCaseOnlyTheFirstChar(camelCasedInput));
    }
}