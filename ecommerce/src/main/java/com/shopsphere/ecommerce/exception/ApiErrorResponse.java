package com.shopsphere.ecommerce.exception;


import java.time.LocalDateTime;
import java.util.Map;

public class ApiErrorResponse {

    private int status;
    private String message;
    private LocalDateTime timeStamp;
    private Map<String, String > errors;

    public ApiErrorResponse(){}

    public ApiErrorResponse(int status, String message, LocalDateTime timeStamp, Map<String, String> errors) {
        this.status = status;
        this.message = message;
        this.timeStamp = timeStamp;
        this.errors = errors;
    }


    public int getStatus() {
        return status;
    }

    public String getMessage() {
        return message;
    }

    public LocalDateTime getTimeStamp() {
        return timeStamp;
    }

    public Map<String, String> getErrors() {
        return errors;
    }
}