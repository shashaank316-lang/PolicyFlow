package com.policyflow.customerservice.exception;

public class CustomerNotFoundException extends RuntimeException {

    public CustomerNotFoundException(Long id) {
        super("Customer not found: " + id);
    }

    public CustomerNotFoundException(String customerNumber) {
        super("Customer not found: " + customerNumber);
    }
}