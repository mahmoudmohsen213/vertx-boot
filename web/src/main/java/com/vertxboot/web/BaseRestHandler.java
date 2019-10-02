package com.vertxboot.web;

import io.vertx.core.Handler;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.api.validation.HTTPRequestValidationHandler;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.Set;

/**
 * IMPORTANT NOTE #1: HttpMethod.OTHER is used as a placeholder for a null value, used to keep the httpMethod field of the
 * annotation optional. Accordingly, if the httpMethod field has a value of HttpMethod.OTHER, it is ignored by the base
 * rest handler and should be ignored by any dynamic loader.
 * <p>
 * IMPORTANT NOTE #2: If the httpMethod field equals to HttpMethod.OTHER and the httpMethods field is an empty, this should
 * signal the dynamic loader that the client code left these two fields to the default values, therefore the concrete rest
 * handler will be registered to the router on ALL HTTP methods.
 * <p>
 * Please see com.orange.kms.base.rest.RestMapping for more details.
 */
public abstract class BaseRestHandler implements Handler<RoutingContext> {
    protected Logger logger;
    protected final EnumSet<HttpMethod> httpMethods;
    protected final String path;

    protected BaseRestHandler() {
        logger = LoggerFactory.getLogger(this.getClass());
        RestMapping restMapping = this.getClass().getAnnotation(RestMapping.class);

        if (restMapping == null) {
            logger.fatal("Error initializing rest handler due to missing RestMapping annotation");
            throw new RuntimeException("Error initializing rest handler due to missing RestMapping annotation");
        }

        if ((restMapping.httpMethod() == HttpMethod.OTHER) && (restMapping.httpMethods().length == 0))
            httpMethods = EnumSet.allOf(HttpMethod.class);
        else {
            if (restMapping.httpMethods().length == 0)
                httpMethods = EnumSet.noneOf(HttpMethod.class);
            else httpMethods = EnumSet.copyOf(Arrays.asList(restMapping.httpMethods()));

            if (restMapping.httpMethod() != HttpMethod.OTHER)
                httpMethods.add(restMapping.httpMethod());
        }

        path = restMapping.path();
        logger.info("Rest handler initialized: class: " + this.getClass().getName() +
                " path: " + path + " HTTP methods: " + httpMethods.toString());
    }

    public HTTPRequestValidationHandler getHttpRequestValidationHandler() {
        return null;
    }

    public final Set<HttpMethod> getHttpMethods() {
        return this.httpMethods;
    }

    public final String getPath() {
        return this.path;
    }
}
