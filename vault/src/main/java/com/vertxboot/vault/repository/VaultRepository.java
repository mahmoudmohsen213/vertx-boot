package com.vertxboot.vault.repository;

import com.vertxboot.vault.entity.VaultEntityScope;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface VaultRepository {
    Class entityClass();
    VaultEntityScope scope() default VaultEntityScope.USER;
    String dataPath();
    String metaDataPath() default "";
}
