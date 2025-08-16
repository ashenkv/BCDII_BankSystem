package com.bank.entity;

/**
 * Enumeration representing the status of a bank account
 */
public enum AccountStatus {
    ACTIVE("Active"),
    INACTIVE("Inactive"),
    SUSPENDED("Suspended"),
    CLOSED("Closed"),
    FROZEN("Frozen"),
    PENDING_APPROVAL("Pending Approval"),
    DORMANT("Dormant");
    
    private final String displayName;
    
    AccountStatus(String displayName) {
        this.displayName = displayName;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    @Override
    public String toString() {
        return displayName;
    }
} 