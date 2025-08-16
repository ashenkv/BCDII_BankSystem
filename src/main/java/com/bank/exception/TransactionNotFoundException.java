package com.bank.exception;

import jakarta.ejb.ApplicationException;

/**
 * Exception thrown when a transaction is not found in the system
 */
@ApplicationException(rollback = false)
public class TransactionNotFoundException extends BankingException {
    
    private static final long serialVersionUID = 1L;
    
    public TransactionNotFoundException() {
        super();
    }
    
    public TransactionNotFoundException(String message) {
        super(message);
    }
    
    public TransactionNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
} 