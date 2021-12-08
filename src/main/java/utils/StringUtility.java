package utils;

/**
 * Utility class for operation with {@link String}s.
 */
public class StringUtility {

    /**
     * Split a camel-case string into space separated stings (case preserved).
     *
     * @param camelCasedString The camel-cased string.
     * @return The space separated stings (case preserved) for the given input parameter.
     */
    public static String splitCamelCase(String camelCasedString) {
        return camelCasedString.replaceAll(
                String.format("%s|%s|%s",
                        "(?<=[A-Z])(?=[A-Z][a-z])",
                        "(?<=[^A-Z])(?=[A-Z])",
                        "(?<=[A-Za-z])(?=[^A-Za-z])"
                ),
                " "
        );
    }

    /**
     * @param string The input string.
     * @return the same input string but with only the first character to upper case.
     */
    public static String toUpperCaseOnlyTheFirstChar(String string) {
        String tmpString = "";
        if (string.length() > 0) {
            tmpString =
                    String.valueOf(Character.toUpperCase(string.charAt(0)));
            if (string.length() > 1) {
                tmpString += string.substring(1).toLowerCase();
            }
        }
        return tmpString;
    }

}
