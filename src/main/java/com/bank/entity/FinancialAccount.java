package com.bank.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * FinancialAccount entity representing banking accounts
 * Contains account information and balance management
 */
@Entity
@Table(name = "accounts")
@NamedQueries({
    @NamedQuery(name = "FinancialAccount.findAll", query = "SELECT a FROM FinancialAccount a"),
    @NamedQuery(name = "FinancialAccount.findByAccountNumber", query = "SELECT a FROM FinancialAccount a WHERE a.accountNumber = :accountNumber"),
    @NamedQuery(name = "FinancialAccount.findByCustomer", query = "SELECT a FROM FinancialAccount a WHERE a.customer = :customer"),
    @NamedQuery(name = "FinancialAccount.findActiveAccounts", query = "SELECT a FROM FinancialAccount a WHERE a.status = com.bank.entity.AccountStatus.ACTIVE")
})
public class FinancialAccount implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "id")
    private Long id;
    
    @Column(name = "account_number", unique = true, nullable = false, length = 20)
    @NotBlank(message = "Account number is required")
    @Size(min = 5, max = 20, message = "Account number must be between 5 and 20 characters")
    private String accountNumber;
    
    @Column(name = "account_type", nullable = false)
    @Enumerated(EnumType.STRING)
    @NotNull(message = "Account type is required")
    private AccountType accountType;
    
    @Column(name = "balance", nullable = false, precision = 15, scale = 2)
    @NotNull(message = "Balance cannot be null")
    @DecimalMin(value = "0.0", inclusive = true, message = "Balance cannot be negative")
    private BigDecimal balance = BigDecimal.ZERO;
    
    @Column(name = "available_balance", nullable = false, precision = 15, scale = 2)
    @NotNull(message = "Available balance cannot be null")
    @DecimalMin(value = "0.0", inclusive = true, message = "Available balance cannot be negative")
    private BigDecimal availableBalance = BigDecimal.ZERO;
    
    @Column(name = "overdraft_limit", precision = 15, scale = 2)
    @DecimalMin(value = "0.0", inclusive = true, message = "Overdraft limit cannot be negative")
    private BigDecimal overdraftLimit = BigDecimal.ZERO;
    
    @Column(name = "interest_rate", precision = 5, scale = 4)
    @DecimalMin(value = "0.0", inclusive = true, message = "Interest rate cannot be negative")
    @DecimalMax(value = "100.0", inclusive = true, message = "Interest rate cannot exceed 100%")
    private BigDecimal interestRate = BigDecimal.ZERO;
    
    @Column(name = "account_status", nullable = false)
    @Enumerated(EnumType.STRING)
    @NotNull(message = "Account status is required")
    private AccountStatus status = AccountStatus.ACTIVE;
    
    @Column(name = "created_at", nullable = false)
    @NotNull
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @Column(name = "last_transaction_date")
    private LocalDateTime lastTransactionDate;
    
    @Column(name = "version")
    @Version
    private int version;
    
    // Many-to-One relationship with Customer
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    @NotNull(message = "Customer is required")
    private Customer customer;
    
    // Default constructor
    public FinancialAccount() {
        this.createdAt = LocalDateTime.now();
    }
    
    // Constructor with essential fields
    public FinancialAccount(String accountNumber, AccountType accountType, Customer customer) {
        this();
        this.accountNumber = accountNumber;
        this.accountType = accountType;
        this.customer = customer;
    }
    
    // PrePersist callback
    @PrePersist
    protected void initializeAccount() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (balance == null) {
            balance = BigDecimal.ZERO;
        }
        if (availableBalance == null) {
            availableBalance = balance;
        }
        if (overdraftLimit == null) {
            overdraftLimit = BigDecimal.ZERO;
        }
        if (interestRate == null) {
            interestRate = BigDecimal.ZERO;
        }
        if (status == null) {
            status = AccountStatus.ACTIVE;
        }
    }
    
    // PreUpdate callback
    @PreUpdate
    protected void updateTimestamp() {
        updatedAt = LocalDateTime.now();
    }
    
    // Business methods
    public boolean validateWithdrawalAmount(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            return false;
        }
        BigDecimal totalAvailable = availableBalance.add(overdraftLimit);
        return amount.compareTo(totalAvailable) <= 0;
    }
    
    public void addFunds(BigDecimal amount) {
        if (amount != null && amount.compareTo(BigDecimal.ZERO) > 0) {
            this.balance = this.balance.add(amount);
            this.availableBalance = this.availableBalance.add(amount);
            this.lastTransactionDate = LocalDateTime.now();
        }
    }
    
    public void deductFunds(BigDecimal amount) {
        if (validateWithdrawalAmount(amount)) {
            this.balance = this.balance.subtract(amount);
            this.availableBalance = this.availableBalance.subtract(amount);
            this.lastTransactionDate = LocalDateTime.now();
        }
    }
    
    public BigDecimal calculateTotalAvailableBalance() {
        return availableBalance.add(overdraftLimit);
    }
    
    public boolean isAccountActive() {
        return status == AccountStatus.ACTIVE;
    }
    
    // Getters and Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public String getAccountNumber() {
        return accountNumber;
    }
    
    public void setAccountNumber(String accountNumber) {
        this.accountNumber = accountNumber;
    }
    
    public AccountType getAccountType() {
        return accountType;
    }
    
    public void setAccountType(AccountType accountType) {
        this.accountType = accountType;
    }
    
    public BigDecimal getBalance() {
        return balance;
    }
    
    public void setBalance(BigDecimal balance) {
        this.balance = balance;
    }
    
    public BigDecimal getAvailableBalance() {
        return availableBalance;
    }
    
    public void setAvailableBalance(BigDecimal availableBalance) {
        this.availableBalance = availableBalance;
    }
    
    public BigDecimal getOverdraftLimit() {
        return overdraftLimit;
    }
    
    public void setOverdraftLimit(BigDecimal overdraftLimit) {
        this.overdraftLimit = overdraftLimit;
    }
    
    public BigDecimal getInterestRate() {
        return interestRate;
    }
    
    public void setInterestRate(BigDecimal interestRate) {
        this.interestRate = interestRate;
    }
    
    public AccountStatus getStatus() {
        return status;
    }
    
    public void setStatus(AccountStatus status) {
        this.status = status;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
    
    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
    
    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
    
    public LocalDateTime getLastTransactionDate() {
        return lastTransactionDate;
    }
    
    public void setLastTransactionDate(LocalDateTime lastTransactionDate) {
        this.lastTransactionDate = lastTransactionDate;
    }
    
    public int getVersion() {
        return version;
    }
    
    public void setVersion(int version) {
        this.version = version;
    }
    
    public Customer getCustomer() {
        return customer;
    }
    
    public void setCustomer(Customer customer) {
        this.customer = customer;
    }
    
    // Override methods
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        FinancialAccount that = (FinancialAccount) obj;
        return accountNumber != null ? accountNumber.equals(that.accountNumber) : that.accountNumber == null;
    }
    
    @Override
    public int hashCode() {
        return accountNumber != null ? accountNumber.hashCode() : 0;
    }
    
    @Override
    public String toString() {
        return "FinancialAccount{" +
                "id=" + id +
                ", accountNumber='" + accountNumber + '\'' +
                ", accountType=" + accountType +
                ", balance=" + balance +
                ", status=" + status +
                '}';
    }
} 