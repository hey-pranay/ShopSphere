package com.shopsphere.ecommerce.exception;

public class DuplicateOrderItemException extends RuntimeException {
    public DuplicateOrderItemException(String message) {
        super(message);
    }
}