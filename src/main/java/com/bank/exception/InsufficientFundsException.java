package com.bank.exception;

import jakarta.ejb.ApplicationException;

/**
 * Exception thrown when there are insufficient funds for a transaction
 */
@ApplicationException(rollback = true)
public class InsufficientFundsException extends BankingException {
    
    private static final long serialVersionUID = 1L;
    
    public InsufficientFundsException() {
        super();
    }
    
    public InsufficientFundsException(String message) {
        super(message);
    }
    
    public InsufficientFundsException(String message, Throwable cause) {
        super(message, cause);
    }
} 