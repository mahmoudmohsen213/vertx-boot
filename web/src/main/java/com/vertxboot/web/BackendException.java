package com.vertxboot.web;

import java.util.ArrayList;
import java.util.List;

public class BackendException extends RuntimeException {
    private ErrorCode errorCode;
    private List<String> details;

    public BackendException(ErrorCode errorCode, String detail) {
        super(errorCode.toString());
        this.errorCode = errorCode;
        this.details = new ArrayList<>();
        this.details.add(detail);
    }

    public BackendException(ErrorCode errorCode, String detail, Throwable throwable) {
        super(errorCode.toString(), throwable);
        this.errorCode = errorCode;
        this.details = new ArrayList<>();
        this.details.add(detail);
    }

    public BackendException(ErrorCode errorCode, List<String> details) {
        super(errorCode.toString());
        this.errorCode = errorCode;
        this.details = details;
    }

    public BackendException(ErrorCode errorCode, List<String> details, Throwable throwable) {
        super(errorCode.toString(), throwable);
        this.errorCode = errorCode;
        this.details = details;
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }

    public void setErrorCode(ErrorCode errorCode) {
        this.errorCode = errorCode;
    }

    public List<String> getDetails() {
        return details;
    }

    public void setDetails(List<String> details) {
        this.details = details;
    }
}
