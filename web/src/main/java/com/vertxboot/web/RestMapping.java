package com.vertxboot.web;

import io.vertx.core.http.HttpMethod;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * This annotation represents RestMapping data to be used by the base rest handler and a dynamic loader of rest handlers.
 * <p>
 * The member fields are:
 * HttpMethod httpMethod()
 * HttpMethod[] httpMethods()
 * String path()
 * <p>
 * The annotation specifies three fields, an httpMethod, an array of httpMethods, and a path. The existence of two fields
 * regarding the http method is to enable the client code to specify only one http method through the httpMethod field,
 * or a combination of methods through the httpMethods field. If the client code specifies both of them (a single method
 * and an array) all of them should be honored, because basically, Vert.X allows a single handler to be registered to
 * multiple methods anyway.
 * <p>
 * In order to allow the client code to specify only one field of the httpMethod and the httpMethods fields, we need to
 * make them optional, which requires them to have default values in the annotation definition below.
 * <p>
 * The default value of httpMethod is HttpMethod.OTHER.
 * The default value of httpMethods is an empty array.
 * The path has no default value as it is mandatory.
 * <p>
 * IMPORTANT NOTE #1: HttpMethod.OTHER is used as a placeholder for a null value, used to keep the httpMethod field of
 * the annotation optional. Accordingly, if the httpMethod field has a value of HttpMethod.OTHER, it is ignored by the
 * dynamic loader.
 * <p>
 * In case of a business logic that strictly requires HttpMethod.OTHER, the client code can add it to the httpMethods
 * array, only in this case, it will be honored, and the concrete rest handler will be registered to the router on this
 * method, along with any other methods in the array. Noting that I don't really know what this method actually means,
 * given that it does not exist in the standard HTTP methods at the time of writing this code.
 * <p>
 * IMPORTANT NOTE #2: If the httpMethod field equals to HttpMethod.OTHER and the httpMethods field is an empty, this should
 * signal the dynamic loader that the client code left these two fields to the default values, therefore the concrete rest
 * handler will be registered to the router on ALL HTTP methods.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface RestMapping {
    HttpMethod httpMethod() default HttpMethod.OTHER;
    HttpMethod[] httpMethods() default {};
    String path();
}
