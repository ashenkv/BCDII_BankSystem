package com.bank.entity;

/**
 * Enumeration representing the status of a banking transaction
 */
public enum TransactionStatus {
    PENDING("Pending"),
    PROCESSING("Processing"),
    COMPLETED("Completed"),
    FAILED("Failed"),
    CANCELLED("Cancelled"),
    REVERSED("Reversed"),
    SCHEDULED("Scheduled"),
    DECLINED("Declined"),
    TIMEOUT("Timeout"),
    FROZEN("Frozen");
    
    private final String displayName;
    
    TransactionStatus(String displayName) {
        this.displayName = displayName;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    public boolean isFinal() {
        return this == COMPLETED || this == FAILED || this == CANCELLED || this == REVERSED || this == DECLINED;
    }
    
    public boolean isActive() {
        return this == PENDING || this == PROCESSING || this == SCHEDULED;
    }
    
    public boolean isSuccessful() {
        return this == COMPLETED;
    }
    
    @Override
    public String toString() {
        return displayName;
    }
} 