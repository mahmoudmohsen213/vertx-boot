package com.vertxboot.web;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.vertx.ext.web.RoutingContext;

import javax.validation.ConstraintViolation;
import javax.validation.Validator;
import java.io.IOException;
import java.util.Set;
import java.util.stream.Collectors;

public class RequestBodyValidationUtil {

    private RequestBodyValidationUtil() {
    }

    public static <T> T validate(RoutingContext routingContext, Validator validator, TypeReference<T> responseBodyType) {
        T responseBody;
        try {
            responseBody = new ObjectMapper().readValue(
                    routingContext.getBodyAsString(), responseBodyType);
        } catch (IOException e) {
            throw new BackendException(ErrorCode.INVALID_INPUT, "Invalid json body");
        }

        Set<ConstraintViolation<T>> constraintViolations = validator.validate(responseBody);
        if (!constraintViolations.isEmpty()) {
            throw new BackendException(ErrorCode.INVALID_INPUT,
                    constraintViolations.stream().map(ConstraintViolation::getMessage).collect(Collectors.toList()));
        }

        return responseBody;
    }
}
