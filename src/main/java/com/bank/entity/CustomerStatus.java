package com.bank.entity;

/**
 * Enumeration representing the status of a customer account
 */
public enum CustomerStatus {
    ACTIVE("Active"),
    INACTIVE("Inactive"),
    SUSPENDED("Suspended"),
    CLOSED("Closed"),
    PENDING_VERIFICATION("Pending Verification");
    
    private final String displayName;
    
    CustomerStatus(String displayName) {
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