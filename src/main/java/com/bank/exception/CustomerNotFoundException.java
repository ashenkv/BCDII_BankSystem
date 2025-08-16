package com.bank.exception;

import jakarta.ejb.ApplicationException;

/**
 * Exception thrown when a customer is not found in the system
 */
@ApplicationException(rollback = false)
public class CustomerNotFoundException extends BankingException {
    
    private static final long serialVersionUID = 1L;
    
    public CustomerNotFoundException() {
        super();
    }
    
    public CustomerNotFoundException(String message) {
        super(message);
    }
    
    public CustomerNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
} 