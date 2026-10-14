package com.policyflow.policyservice.exception;

public class DuplicatePolicyException extends RuntimeException {

    public DuplicatePolicyException(String policyNumber) {
        super("Policy number already exists: " + policyNumber);
    }
}