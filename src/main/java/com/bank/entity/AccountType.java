package com.bank.entity;

/**
 * Enumeration representing different types of bank accounts
 */
public enum AccountType {
    SAVINGS("Savings Account"),
    CHECKING("Checking Account"),
    BUSINESS("Business Account"),
    JOINT("Joint Account"),
    MONEY_MARKET("Money Market Account"),
    CERTIFICATE_OF_DEPOSIT("Certificate of Deposit"),
    CREDIT("Credit Account"),
    LOAN("Loan Account");
    
    private final String displayName;
    
    AccountType(String displayName) {
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