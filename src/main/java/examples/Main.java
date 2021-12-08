package examples;

import benchmark.Benchmark;
import benchmark.BenchmarkRunner;

/**
 * Class to show how to use the Benchmark framework proposed by this project.
 */
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
}
