package com.vertxboot.web;

import java.util.List;

public interface InterceptorConfig {
    List<BaseInterceptor> interceptors();

    List<BaseInterceptor> errorHandlers();
}
