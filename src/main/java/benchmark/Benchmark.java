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
    int DEFAULT_ITERATIONS = 1000;
    /** Number of warmup iterations (excluded from benchmark statistics). */
    int warmUpIterations() default DEFAULT_ITERATIONS;
    /** Number of iterations (for benchmark statistics). */
    int iterations() default DEFAULT_ITERATIONS;
    /** Number of teardown iterations (excluded from benchmark statistics). */
    int tearDownIterations() default DEFAULT_ITERATIONS;
}