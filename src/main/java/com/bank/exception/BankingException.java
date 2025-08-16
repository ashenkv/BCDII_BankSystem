package com.bank.exception;

import jakarta.ejb.ApplicationException;

/**
 * Base exception class for all banking-related exceptions
 * Uses @ApplicationException to ensure proper transaction rollback behavior
 */
@ApplicationException(rollback = true)
public class BankingException extends Exception {
    
    private static final long serialVersionUID = 1L;
    
    private String errorCode;
    private String userMessage;
    
    public BankingException() {
        super();
    }
    
    public BankingException(String message) {
        super(message);
        this.userMessage = message;
    }
    
    public BankingException(String message, Throwable cause) {
        super(message, cause);
        this.userMessage = message;
    }
    
    public BankingException(String errorCode, String message, String userMessage) {
        super(message);
        this.errorCode = errorCode;
        this.userMessage = userMessage;
    }
    
    public BankingException(String errorCode, String message, String userMessage, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
        this.userMessage = userMessage;
    }
    
    public String getErrorCode() {
        return errorCode;
    }
    
    public void setErrorCode(String errorCode) {
        this.errorCode = errorCode;
    }
    
    public String getUserMessage() {
        return userMessage != null ? userMessage : getMessage();
    }
    
    public void setUserMessage(String userMessage) {
        this.userMessage = userMessage;
    }
} 