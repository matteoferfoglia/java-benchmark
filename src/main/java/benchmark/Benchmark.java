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
}