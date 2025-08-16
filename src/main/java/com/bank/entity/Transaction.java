package com.bank.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Transaction entity representing banking transactions
 * Tracks all financial operations between accounts
 */
@Entity
@Table(name = "transactions")
@NamedQueries({
    @NamedQuery(name = "Transaction.findAll", query = "SELECT t FROM Transaction t ORDER BY t.transactionDate DESC"),
    @NamedQuery(name = "Transaction.findByAccount", query = "SELECT t FROM Transaction t WHERE t.sourceAccount = :account OR t.targetAccount = :account ORDER BY t.transactionDate DESC"),
    @NamedQuery(name = "Transaction.findByAccountNumber", query = "SELECT t FROM Transaction t WHERE t.sourceAccount.accountNumber = :accountNumber OR t.targetAccount.accountNumber = :accountNumber ORDER BY t.transactionDate DESC"),
    @NamedQuery(name = "Transaction.findByTransactionId", query = "SELECT t FROM Transaction t WHERE t.transactionId = :transactionId"),
    @NamedQuery(name = "Transaction.findByDateRange", query = "SELECT t FROM Transaction t WHERE t.transactionDate BETWEEN :startDate AND :endDate ORDER BY t.transactionDate DESC"),
    @NamedQuery(name = "Transaction.findPendingTransactions", query = "SELECT t FROM Transaction t WHERE t.status = com.bank.entity.TransactionStatus.PENDING"),
    @NamedQuery(name = "Transaction.findScheduled", query = "SELECT t FROM Transaction t WHERE t.status = com.bank.entity.TransactionStatus.SCHEDULED ORDER BY t.scheduledDate ASC")
})
public class Transaction implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "id")
    private Long id;
    
    @Column(name = "transaction_id", unique = true, nullable = false, length = 36)
    @NotBlank(message = "Transaction ID is required")
    @Size(max = 36, message = "Transaction ID cannot exceed 36 characters")
    private String transactionId;
    
    @Column(name = "transaction_type", nullable = false)
    @Enumerated(EnumType.STRING)
    @NotNull(message = "Transaction type is required")
    private TransactionType transactionType;
    
    @Column(name = "amount", nullable = false, precision = 15, scale = 2)
    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.01", message = "Amount must be greater than 0")
    private BigDecimal amount;
    
    @Column(name = "currency", nullable = false, length = 3)
    @NotBlank(message = "Currency is required")
    @Size(min = 3, max = 3, message = "Currency must be 3 characters")
    @Pattern(regexp = "[A-Z]{3}", message = "Currency must be in ISO 4217 format")
    private String currency = "USD";
    
    @Column(name = "description", length = 500)
    @Size(max = 500, message = "Description cannot exceed 500 characters")
    private String description;
    
    @Column(name = "reference_number", length = 50)
    @Size(max = 50, message = "Reference number cannot exceed 50 characters")
    private String referenceNumber;
    
    @Column(name = "transaction_status", nullable = false)
    @Enumerated(EnumType.STRING)
    @NotNull(message = "Transaction status is required")
    private TransactionStatus status = TransactionStatus.PENDING;
    
    @Column(name = "transaction_date", nullable = false)
    @NotNull(message = "Transaction date is required")
    private LocalDateTime transactionDate;
    
    @Column(name = "scheduled_date")
    private LocalDateTime scheduledDate;
    
    @Column(name = "processed_date")
    private LocalDateTime processedDate;
    
    @Column(name = "source_balance_before", precision = 15, scale = 2)
    private BigDecimal sourceBalanceBefore;
    
    @Column(name = "source_balance_after", precision = 15, scale = 2)
    private BigDecimal sourceBalanceAfter;
    
    @Column(name = "target_balance_before", precision = 15, scale = 2)
    private BigDecimal targetBalanceBefore;
    
    @Column(name = "target_balance_after", precision = 15, scale = 2)
    private BigDecimal targetBalanceAfter;
    
    @Column(name = "created_at", nullable = false)
    @NotNull
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @Column(name = "version")
    @Version
    private int version;
    
    // Many-to-One relationship with source Account
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "source_account_id")
    private FinancialAccount sourceAccount;
    
    // Many-to-One relationship with target Account
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "target_account_id")
    private FinancialAccount targetAccount;
    
    // Default constructor
    public Transaction() {
        this.createdAt = LocalDateTime.now();
        this.transactionDate = LocalDateTime.now();
    }
    
    // Constructor for basic transaction
    public Transaction(TransactionType transactionType, BigDecimal amount, FinancialAccount sourceAccount) {
        this();
        this.transactionType = transactionType;
        this.amount = amount;
        this.sourceAccount = sourceAccount;
        this.transactionId = generateTransactionId();
    }
    
    // Constructor for transfer transaction
    public Transaction(TransactionType transactionType, BigDecimal amount, FinancialAccount sourceAccount, FinancialAccount targetAccount, String description) {
        this(transactionType, amount, sourceAccount);
        this.targetAccount = targetAccount;
        this.description = description;
    }
    
    // PrePersist callback
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (transactionId == null) {
            transactionId = generateTransactionId();
        }
        if (transactionDate == null) {
            transactionDate = LocalDateTime.now();
        }
    }
    
    // PreUpdate callback
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
    
    // Business methods
    private String generateTransactionId() {
        return "TXN-" + System.currentTimeMillis() + "-" + (int)(Math.random() * 9999);
    }
    
    public boolean isCompleted() {
        return status == TransactionStatus.COMPLETED;
    }
    
    public boolean isPending() {
        return status == TransactionStatus.PENDING;
    }
    
    public boolean isFailed() {
        return status == TransactionStatus.FAILED;
    }
    
    public boolean isScheduled() {
        return scheduledDate != null && scheduledDate.isAfter(LocalDateTime.now());
    }
    
    public void markAsCompleted() {
        this.status = TransactionStatus.COMPLETED;
        this.processedDate = LocalDateTime.now();
    }
    
    public void markAsFailed() {
        this.status = TransactionStatus.FAILED;
        this.processedDate = LocalDateTime.now();
    }
    
    public void markAsProcessing() {
        this.status = TransactionStatus.PROCESSING;
    }
    
    // Getters and Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public String getTransactionId() {
        return transactionId;
    }
    
    public void setTransactionId(String transactionId) {
        this.transactionId = transactionId;
    }
    
    public TransactionType getTransactionType() {
        return transactionType;
    }
    
    public void setTransactionType(TransactionType transactionType) {
        this.transactionType = transactionType;
    }
    
    public BigDecimal getAmount() {
        return amount;
    }
    
    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }
    
    public String getCurrency() {
        return currency;
    }
    
    public void setCurrency(String currency) {
        this.currency = currency;
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    public String getReferenceNumber() {
        return referenceNumber;
    }
    
    public void setReferenceNumber(String referenceNumber) {
        this.referenceNumber = referenceNumber;
    }
    
    public TransactionStatus getStatus() {
        return status;
    }
    
    public void setStatus(TransactionStatus status) {
        this.status = status;
    }
    
    public LocalDateTime getTransactionDate() {
        return transactionDate;
    }
    
    public void setTransactionDate(LocalDateTime transactionDate) {
        this.transactionDate = transactionDate;
    }
    
    public LocalDateTime getScheduledDate() {
        return scheduledDate;
    }
    
    public void setScheduledDate(LocalDateTime scheduledDate) {
        this.scheduledDate = scheduledDate;
    }
    
    public LocalDateTime getProcessedDate() {
        return processedDate;
    }
    
    public void setProcessedDate(LocalDateTime processedDate) {
        this.processedDate = processedDate;
    }
    
    public BigDecimal getSourceBalanceBefore() {
        return sourceBalanceBefore;
    }
    
    public void setSourceBalanceBefore(BigDecimal sourceBalanceBefore) {
        this.sourceBalanceBefore = sourceBalanceBefore;
    }
    
    public BigDecimal getSourceBalanceAfter() {
        return sourceBalanceAfter;
    }
    
    public void setSourceBalanceAfter(BigDecimal sourceBalanceAfter) {
        this.sourceBalanceAfter = sourceBalanceAfter;
    }
    
    public BigDecimal getTargetBalanceBefore() {
        return targetBalanceBefore;
    }
    
    public void setTargetBalanceBefore(BigDecimal targetBalanceBefore) {
        this.targetBalanceBefore = targetBalanceBefore;
    }
    
    public BigDecimal getTargetBalanceAfter() {
        return targetBalanceAfter;
    }
    
    public void setTargetBalanceAfter(BigDecimal targetBalanceAfter) {
        this.targetBalanceAfter = targetBalanceAfter;
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
    
    public int getVersion() {
        return version;
    }
    
    public void setVersion(int version) {
        this.version = version;
    }
    
    public FinancialAccount getSourceAccount() {
        return sourceAccount;
    }
    
    public void setSourceAccount(FinancialAccount sourceAccount) {
        this.sourceAccount = sourceAccount;
    }
    
    public FinancialAccount getTargetAccount() {
        return targetAccount;
    }
    
    public void setTargetAccount(FinancialAccount targetAccount) {
        this.targetAccount = targetAccount;
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Transaction that = (Transaction) obj;
        return transactionId != null ? transactionId.equals(that.transactionId) : that.transactionId == null;
    }
    
    @Override
    public int hashCode() {
        return transactionId != null ? transactionId.hashCode() : 0;
    }
    
    @Override
    public String toString() {
        return "Transaction{" +
                "id=" + id +
                ", transactionId='" + transactionId + '\'' +
                ", transactionType=" + transactionType +
                ", amount=" + amount +
                ", status=" + status +
                ", transactionDate=" + transactionDate +
                '}';
    }
} 