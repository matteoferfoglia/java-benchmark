package benchmark;

@SuppressWarnings({"unused", "EmptyMethod"}) // dummy methods used for tests
class ClassWithDummyMethodsForTestingPurposes {

    final static String NAME_OF_PUBLIC_STATIC_METHOD_WITHOUT_PARAMETERS = "publicStaticMethodWithoutParameters";

    @Benchmark
    private static void staticMethodWithoutParameters() {
    }

    @Benchmark
    public static void publicStaticMethodWithoutParameters() {
    }

    @Benchmark
    private void notStaticMethod() {
    }

    @Benchmark
    private void notStaticMethodWithParameter(String s) {
    }

    @Benchmark
    private static void staticMethodWithParameter(String s) {
    }
}
