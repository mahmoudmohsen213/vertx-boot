package com.vertxboot.web;

import io.netty.handler.codec.http.HttpResponseStatus;

public enum ErrorCode {
    INITIALIZATION_FAILED(HttpResponseStatus.INTERNAL_SERVER_ERROR.code()),
    OPERATION_FAILED(HttpResponseStatus.INTERNAL_SERVER_ERROR.code()),
    INVALID_INPUT(HttpResponseStatus.BAD_REQUEST.code()),
    ENTITY_NOT_FOUND(HttpResponseStatus.BAD_REQUEST.code()),
    ENTITY_LOCKED(HttpResponseStatus.LOCKED.code()),
    PRECONDITION_FAILED(HttpResponseStatus.PRECONDITION_FAILED.code()),
    DATA_ACCESS_FAILED(HttpResponseStatus.INTERNAL_SERVER_ERROR.code()),
    DATA_PERSISTENCE_CONFLICT(HttpResponseStatus.CONFLICT.code()),
    DATA_PERSISTENCE_FAILED(HttpResponseStatus.INTERNAL_SERVER_ERROR.code());

    private final int statusCode;

    ErrorCode(int statusCode) {
        this.statusCode = statusCode;
    }

    @Override
    public String toString() {
        return String.format("%d %s", this.statusCode, this.name());
    }

    public int getStatusCode() {
        return this.statusCode;
    }
}
