package com.vertxboot.web;

import com.vertxboot.beans.BeanConfig;

import java.util.Collections;
import java.util.List;

public interface InterceptorConfig {
    default List<BaseInterceptor> interceptors() {
        return Collections.emptyList();
    }

    default List<BaseInterceptor> errorHandlers() {
        return Collections.emptyList();
    }

    @BeanConfig(async = false, overridable = true)
    static InterceptorConfig interceptorConfig() {
        return new InterceptorConfig() {
        };
    }
}
