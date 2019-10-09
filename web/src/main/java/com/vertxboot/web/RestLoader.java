package com.vertxboot.web;

import com.vertxboot.beans.BeanFactory;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.api.validation.HTTPRequestValidationHandler;
import org.reflections.Reflections;

import java.lang.reflect.InvocationTargetException;
import java.util.Objects;
import java.util.Set;

public class RestLoader {

    protected static final String STATIC_FACTORY_METHOD_NAME = "instance";

    protected static Logger logger = LoggerFactory.getLogger(RestLoader.class);

    protected RestLoader() {
    }

    public static void load(Router router, InterceptorConfig interceptorConfig) {
        logger.info("RestLoader: loading rest handlers start...");

        if (Objects.nonNull(interceptorConfig)) {
            logger.info("RestLoader: loading interceptors");
            interceptorConfig.interceptors().forEach(baseInterceptor ->
                    baseInterceptor.getHttpMethods().forEach(httpMethod -> router.route(httpMethod,
                            baseInterceptor.getPath()).handler(baseInterceptor)));
        }

        logger.info("RestLoader: scanning for rest handlers");
        Reflections reflections = BeanFactory.instance().getSync(Reflections.class);
        Set<Class<?>> set = reflections.getTypesAnnotatedWith(RestHandler.class);

        logger.info("RestLoader: loading rest handlers");
        set.forEach(restHandlerClass -> {
            try {
                logger.info("RestLoader: loading " + restHandlerClass.getName());
                BaseRestHandler baseRestHandler = (BaseRestHandler) restHandlerClass
                        .getMethod(STATIC_FACTORY_METHOD_NAME).invoke(null);

                HTTPRequestValidationHandler httpRequestValidationHandler = baseRestHandler.getHttpRequestValidationHandler();
                if (httpRequestValidationHandler != null) {
                    logger.info("RestLoader: routing validation handler for " + restHandlerClass.getName());
                    baseRestHandler.getHttpMethods().forEach(httpMethod -> router.route(httpMethod,
                            baseRestHandler.getPath()).handler(httpRequestValidationHandler));
                }

                logger.info("RestLoader: routing handler for " + restHandlerClass.getName());
                baseRestHandler.getHttpMethods().forEach(httpMethod -> router.route(httpMethod,
                        baseRestHandler.getPath()).handler(baseRestHandler));
            } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
                logger.error("RestLoader: loading failed: " + restHandlerClass.getName(), e);
            }
        });

        if (Objects.nonNull(interceptorConfig)) {
            logger.info("RestLoader: loading error handlers");
            interceptorConfig.errorHandlers().forEach(baseInterceptor ->
                    baseInterceptor.getHttpMethods().forEach(httpMethod -> router.route(httpMethod,
                            baseInterceptor.getPath()).failureHandler(baseInterceptor)));
        }

        logger.info("RestLoader: loading rest handlers done");
    }
}
