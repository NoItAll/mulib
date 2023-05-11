package de.wwu.mulib.annotations;

import java.lang.annotation.*;

@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface TestDriver {

    String name() default "";

    Class<?> testedClass();

    String testedMethod();

    Class<?>[] parameters();

}
