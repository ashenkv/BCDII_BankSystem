package com.bank.entity;

/**
 * Enumeration representing different types of banking transactions
 */
public enum TransactionType {
    DEPOSIT("Deposit"),
    WITHDRAWAL("Withdrawal"),
    TRANSFER("Transfer"),
    PAYMENT("Payment"),
    INTEREST_CREDIT("Interest Credit"),
    FEE_DEBIT("Fee Debit"),
    REFUND("Refund"),
    REVERSAL("Reversal"),
    LOAN_PAYMENT("Loan Payment"),
    LOAN_DISBURSEMENT("Loan Disbursement"),
    SCHEDULED_TRANSFER("Scheduled Transfer"),
    AUTOMATED_INTEREST("Automated Interest"),
    OVERDRAFT_FEE("Overdraft Fee"),
    ATM_WITHDRAWAL("ATM Withdrawal"),
    ONLINE_TRANSFER("Online Transfer"),
    WIRE_TRANSFER("Wire Transfer"),
    CHECK_DEPOSIT("Check Deposit"),
    DIRECT_DEPOSIT("Direct Deposit");
    
    private final String displayName;
    
    TransactionType(String displayName) {
        this.displayName = displayName;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    public boolean isCredit() {
        return this == DEPOSIT || this == INTEREST_CREDIT || this == REFUND || 
               this == LOAN_DISBURSEMENT || this == CHECK_DEPOSIT || this == DIRECT_DEPOSIT;
    }
    
    public boolean isDebit() {
        return this == WITHDRAWAL || this == PAYMENT || this == FEE_DEBIT || 
               this == LOAN_PAYMENT || this == OVERDRAFT_FEE || this == ATM_WITHDRAWAL;
    }
    
    public boolean isTransfer() {
        return this == TRANSFER || this == SCHEDULED_TRANSFER || 
               this == ONLINE_TRANSFER || this == WIRE_TRANSFER;
    }
    
    public boolean isAutomated() {
        return this == AUTOMATED_INTEREST || this == SCHEDULED_TRANSFER;
    }
    
    @Override
    public String toString() {
        return displayName;
    }
} 