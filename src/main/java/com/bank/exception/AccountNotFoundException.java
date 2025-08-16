package com.bank.exception;

import jakarta.ejb.ApplicationException;

/**
 * Exception thrown when an account is not found in the system
 */
@ApplicationException(rollback = false)
public class AccountNotFoundException extends BankingException {
    
    private static final long serialVersionUID = 1L;
    
    public AccountNotFoundException() {
        super();
    }
    
    public AccountNotFoundException(String message) {
        super(message);
    }
    
    public AccountNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
} 