package com.policyflow.policyservice.exception;

public class PolicyNotFoundException extends RuntimeException {

    public PolicyNotFoundException(Long id) {
        super("Policy not found: " + id);
    }

    public PolicyNotFoundException(String policyNumber) {
        super("Policy not found: " + policyNumber);
    }
}