package benchmark;

import java.lang.annotation.*;

/**
 * Annotation to be used on methods to benchmark
 * Method annotated with this annotation must be static and must not take any parameters.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Benchmark {
    /**
     * Default iterations number.
     */
    int DEFAULT_ITERATIONS = 1000;

    /**
     * @return the number of warmup iterations (excluded from benchmark statistics).
     */
    int warmUpIterations() default DEFAULT_ITERATIONS;

    /**
     * @return the number of iterations (for benchmark statistics).
     */
    int iterations() default DEFAULT_ITERATIONS;

    /**
     * @return the number of teardown iterations (excluded from benchmark statistics).
     */
    int tearDownIterations() default DEFAULT_ITERATIONS;

    /**
     * @return comment which should appear in the benchmark report.
     */
    String commentToReport() default "";

    /**
     * @return the static method name (without parameters) to be executed before each iteration.
     * The canonical name, starting with the class name and without neither parenthesis nor
     * return type nor parameters, is provided.
     */
    String beforeEach() default "";

    /**
     * @return the static method name (without parameters) to be executed after each iteration.
     * The canonical name, starting with the class name and without neither parenthesis nor
     * return type nor parameters, is provided.
     */
    String afterEach() default "";
}