package com.vertxboot.beans;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface BeanConfig {
    boolean async() default true;

    boolean overridable() default false;

    BeanScope scope() default BeanScope.SINGLETON;
}
