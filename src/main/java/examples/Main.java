package examples;

import benchmark.Benchmark;
import benchmark.BenchmarkRunner;

/**
 * Class to show how to use the Benchmark framework proposed by this project.
 */
@SuppressWarnings({"ResultOfMethodCallIgnored", "UnusedReturnValue"})   // illustrative examples
public class Main {

    /**
     * Main methods used to show how to use this benchmarking framework.
     * @param args Command line arguments.
     */
    public static void main(String[] args) {
        BenchmarkRunner benchmarkRunner = new BenchmarkRunner();
        benchmarkRunner.benchmarkAllAnnotatedMethodsAndGetListOfResults();
        System.out.println(benchmarkRunner);
    }

    /**
     * Sample method to be benchmarked which computes and returns the sum 1+2+...+9+10.
     *
     * @return the result of the sum 1+2+...+10
     */
    @Benchmark
    static int sumFirst10PositiveIntegers() { // NOTE: must be static method without parameters.
        int sum = 0;
        for (int i = 1; i <= 10; i++) {
            sum += i;
        }
        return sum;
    }

    /**
     * Sample method to be benchmarked which computes and prints the sum 1+2+...+9+10.
     */
    @Benchmark
    static void printTheSumFirst10PositiveIntegers() { // NOTE: must be static method without parameters.
        int sum = 0;
        for (int i = 1; i <= 10; i++) {
            sum += i;
        }
        System.out.println("The sum is: " + sum);
    }

    /**
     * Sample method to be benchmarked which computes and prints the sum 1+2+...+9+10.
     * Only 5 iterations performed for statistics.
     */
    @Benchmark(iterations = 5)
    static void sumFirst10PositiveIntegersWith5Iterations() { // NOTE: must be static method without parameters.
        sumFirst10PositiveIntegers();
    }

    /**
     * Sample method to be benchmarked which computes and prints the sum 1+2+...+9+10.
     * No warmup iterations in benchmark.
     */
    @Benchmark(warmUpIterations = 0)
    static void sumFirst10PositiveIntegersWithoutWarmupIterations() { // NOTE: must be static method without parameters.
        sumFirst10PositiveIntegers();
    }

    /**
     * Sample method to be benchmarked which computes and prints the sum 1+2+...+9+10.
     * No teardown iterations in benchmark.
     */
    @Benchmark(tearDownIterations = 0)
    static void sumFirst10PositiveIntegersWithoutTeardownIterations() { // NOTE: must be static method without parameters.
        sumFirst10PositiveIntegers();
    }

    /**
     * Sample method to be benchmarked which computes and prints the sum 1+2+...+9+10.
     * No teardown iterations in benchmark.
     */
    @Benchmark(warmUpIterations = 1, iterations = 2, tearDownIterations = 3)
    static void sumFirst10PositiveIntegersWithSpecifiedNumberOfIterations() { // NOTE: must be static method without parameters.
        sumFirst10PositiveIntegers();
    }

    /**
     * Like {@link #sumFirst10PositiveIntegersWithSpecifiedNumberOfIterations()},
     * but with a comment to report in the benchmark result.
     */
    @Benchmark(warmUpIterations = 1, iterations = 2, tearDownIterations = 3, commentToReport = "This is a comment")
    static void sumFirst10PositiveIntegersWithSpecifiedNumberOfIterationsWithAComment() { // NOTE: must be static method without parameters.
        sumFirst10PositiveIntegers();
    }
}
