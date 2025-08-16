package com.bank.ejb;

import com.bank.entity.FinancialAccount;
import com.bank.entity.Transaction;
import com.bank.entity.TransactionStatus;
import com.bank.entity.TransactionType;
import com.bank.exception.AccountNotFoundException;
import com.bank.exception.BankingException;
import com.bank.exception.InsufficientFundsException;
import com.bank.exception.TransactionNotFoundException;
import com.bank.interceptor.AuditInterceptor;
import com.bank.interceptor.LoggingInterceptor;


import java.math.RoundingMode;
import jakarta.annotation.security.PermitAll;
import jakarta.annotation.security.RolesAllowed;
import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;
import jakarta.interceptor.Interceptors;
import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * FinancialTransactionService EJB - Handles all banking transactions
 * Implements secure transaction processing with proper rollback mechanisms
 */
@Stateless
@Interceptors({LoggingInterceptor.class, AuditInterceptor.class})
public class FinancialTransactionService {
    
    private static final Logger logger = Logger.getLogger(FinancialTransactionService.class.getName());
    
    @PersistenceContext(unitName = "bankingPU")
    private EntityManager entityManager;
    
    @EJB
    private FinancialAccountService financialAccountService;
    
    /**
     * Processes a money transfer between accounts
     * @param sourceAccountNumber Source account number
     * @param targetAccountNumber Target account number
     * @param amount Amount to transfer
     * @param description Transaction description
     * @return Created transaction
     * @throws BankingException if transfer fails
     */
    @PermitAll  // Allow public access for demo purposes
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public Transaction executeFundTransfer(@NotNull String sourceAccountNumber, 
                               @NotNull String targetAccountNumber, 
                               @NotNull BigDecimal amount, 
                               String description) throws BankingException {
        
        Transaction transaction = null;
        try {
            logger.info(String.format("Processing fund transfer: %s -> %s, amount: %s", 
                sourceAccountNumber, targetAccountNumber, amount));
            
            // Validation
            if (amount.compareTo(BigDecimal.ZERO) <= 0) {
                throw new BankingException("Transfer amount must be positive");
            }
            
            if (sourceAccountNumber.equals(targetAccountNumber)) {
                throw new BankingException("Source and target accounts cannot be the same");
            }
            
            // Find accounts
            FinancialAccount sourceAccount = financialAccountService.retrieveAccountByNumber(sourceAccountNumber);
            if (sourceAccount == null) {
                throw new AccountNotFoundException("Source account not found: " + sourceAccountNumber);
            }
            
            FinancialAccount targetAccount = financialAccountService.retrieveAccountByNumber(targetAccountNumber);
            if (targetAccount == null) {
                throw new AccountNotFoundException("Target account not found: " + targetAccountNumber);
            }
            
            // Validate account states
            if (!sourceAccount.isAccountActive()) {
                throw new BankingException("Source account is not active: " + sourceAccountNumber);
            }
            
            if (!targetAccount.isAccountActive()) {
                throw new BankingException("Target account is not active: " + targetAccountNumber);
            }
            
            // Check funds
            if (!sourceAccount.validateWithdrawalAmount(amount)) {
                throw new InsufficientFundsException("Insufficient funds in source account. Available: " + 
                    sourceAccount.calculateTotalAvailableBalance() + ", Requested: " + amount);
            }
            
            // Create transaction record
            transaction = new Transaction(TransactionType.TRANSFER, amount, sourceAccount, targetAccount, description);
            transaction.setStatus(TransactionStatus.PROCESSING);
            
            // Record balances before transaction
            transaction.setSourceBalanceBefore(sourceAccount.getBalance());
            transaction.setTargetBalanceBefore(targetAccount.getBalance());
            
            entityManager.persist(transaction);
            entityManager.flush();
            
            // Perform the transfer
            sourceAccount.deductFunds(amount);
            targetAccount.addFunds(amount);
            
            // Record balances after transaction
            transaction.setSourceBalanceAfter(sourceAccount.getBalance());
            transaction.setTargetBalanceAfter(targetAccount.getBalance());
            
            // Update accounts
            entityManager.merge(sourceAccount);
            entityManager.merge(targetAccount);
            
            // Mark transaction as completed
            transaction.markAsCompleted();
            entityManager.merge(transaction);
            entityManager.flush();
            
            logger.info("Fund transfer completed successfully. Transaction ID: " + transaction.getTransactionId());
            return transaction;
            
        } catch (AccountNotFoundException | InsufficientFundsException e) {
            if (transaction != null) {
                transaction.markAsFailed();
                entityManager.merge(transaction);
            }
            throw e;
        } catch (Exception e) {
            if (transaction != null) {
                transaction.markAsFailed();
                entityManager.merge(transaction);
            }
            logger.log(Level.SEVERE, "Error processing fund transfer: " + e.getMessage(), e);
            throw new BankingException("Failed to process fund transfer: " + e.getMessage(), e);
        }
    }
    
    /**
     * Processes a deposit transaction
     * @param accountNumber Account number
     * @param amount Amount to deposit
     * @param description Transaction description
     * @return Created transaction
     * @throws BankingException if deposit fails
     */
    @PermitAll  // Allow public access for demo purposes
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public Transaction processDepositTransaction(@NotNull String accountNumber, 
                              @NotNull BigDecimal amount, 
                              String description) throws BankingException {
        
        Transaction transaction = null;
        try {
            logger.info("Processing deposit to account: " + accountNumber + ", amount: " + amount);
            
            if (amount.compareTo(BigDecimal.ZERO) <= 0) {
                throw new BankingException("Deposit amount must be positive");
            }
            
            FinancialAccount account = financialAccountService.retrieveAccountByNumber(accountNumber);
            if (account == null) {
                throw new AccountNotFoundException("Account not found: " + accountNumber);
            }
            
            if (!account.isAccountActive()) {
                throw new BankingException("Cannot deposit to inactive account: " + accountNumber);
            }
            
            // Create transaction record
            transaction = new Transaction(TransactionType.DEPOSIT, amount, account);
            transaction.setDescription(description);
            transaction.setStatus(TransactionStatus.PROCESSING);
            transaction.setSourceBalanceBefore(account.getBalance());
            
            entityManager.persist(transaction);
            entityManager.flush();
            
            // Perform deposit
            account.addFunds(amount);
            entityManager.merge(account);
            
            // Update transaction
            transaction.setSourceBalanceAfter(account.getBalance());
            transaction.markAsCompleted();
            entityManager.merge(transaction);
            entityManager.flush();
            
            logger.info("Deposit completed successfully. Transaction ID: " + transaction.getTransactionId());
            return transaction;
            
        } catch (AccountNotFoundException e) {
            if (transaction != null) {
                transaction.markAsFailed();
                entityManager.merge(transaction);
            }
            throw e;
        } catch (Exception e) {
            if (transaction != null) {
                transaction.markAsFailed();
                entityManager.merge(transaction);
            }
            logger.log(Level.SEVERE, "Error processing deposit: " + e.getMessage(), e);
            throw new BankingException("Failed to process deposit: " + e.getMessage(), e);
        }
    }
    
    /**
     * Processes a withdrawal transaction
     * @param accountNumber Account number
     * @param amount Amount to withdraw
     * @param description Transaction description
     * @return Created transaction
     * @throws BankingException if withdrawal fails
     */
    @PermitAll  // Allow public access for demo purposes
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public Transaction processWithdrawalTransaction(@NotNull String accountNumber, 
                               @NotNull BigDecimal amount, 
                               String description) throws BankingException {
        
        Transaction transaction = null;
        try {
            logger.info("Processing withdrawal from account: " + accountNumber + ", amount: " + amount);
            
            if (amount.compareTo(BigDecimal.ZERO) <= 0) {
                throw new BankingException("Withdrawal amount must be positive");
            }
            
            FinancialAccount account = financialAccountService.retrieveAccountByNumber(accountNumber);
            if (account == null) {
                throw new AccountNotFoundException("Account not found: " + accountNumber);
            }
            
            if (!account.isAccountActive()) {
                throw new BankingException("Cannot withdraw from inactive account: " + accountNumber);
            }
            
            if (!account.validateWithdrawalAmount(amount)) {
                throw new InsufficientFundsException("Insufficient funds for withdrawal. Available: " + 
                    account.calculateTotalAvailableBalance() + ", Requested: " + amount);
            }
            
            // Create transaction record
            transaction = new Transaction(TransactionType.WITHDRAWAL, amount, account);
            transaction.setDescription(description);
            transaction.setStatus(TransactionStatus.PROCESSING);
            transaction.setSourceBalanceBefore(account.getBalance());
            
            entityManager.persist(transaction);
            entityManager.flush();
            
            // Perform withdrawal
            account.deductFunds(amount);
            entityManager.merge(account);
            
            // Update transaction
            transaction.setSourceBalanceAfter(account.getBalance());
            transaction.markAsCompleted();
            entityManager.merge(transaction);
            entityManager.flush();
            
            logger.info("Withdrawal completed successfully. Transaction ID: " + transaction.getTransactionId());
            return transaction;
            
        } catch (AccountNotFoundException | InsufficientFundsException e) {
            if (transaction != null) {
                transaction.markAsFailed();
                entityManager.merge(transaction);
            }
            throw e;
        } catch (Exception e) {
            if (transaction != null) {
                transaction.markAsFailed();
                entityManager.merge(transaction);
            }
            logger.log(Level.SEVERE, "Error processing withdrawal: " + e.getMessage(), e);
            throw new BankingException("Failed to process withdrawal: " + e.getMessage(), e);
        }
    }
    
    /**
     * Schedules a transaction for future execution
     * @param sourceAccountNumber Source account number
     * @param targetAccountNumber Target account number (null for deposits/withdrawals)
     * @param amount Transaction amount
     * @param transactionType Type of transaction
     * @param scheduledDate Scheduled execution date
     * @param description Transaction description
     * @return Created scheduled transaction
     * @throws BankingException if scheduling fails
     */
    @PermitAll  // Allow public access for demo purposes
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public Transaction scheduleFutureTransaction(@NotNull String sourceAccountNumber,
                                         String targetAccountNumber,
                                         @NotNull BigDecimal amount,
                                         @NotNull TransactionType transactionType,
                                         @NotNull LocalDateTime scheduledDate,
                                         String description) throws BankingException {
        try {
            logger.info("Scheduling transaction for account: " + sourceAccountNumber + ", scheduled for: " + scheduledDate);
            
            if (scheduledDate.isBefore(LocalDateTime.now())) {
                throw new BankingException("Scheduled date must be in the future");
            }
            
            FinancialAccount sourceAccount = financialAccountService.retrieveAccountByNumber(sourceAccountNumber);
            if (sourceAccount == null) {
                throw new AccountNotFoundException("Source account not found: " + sourceAccountNumber);
            }
            
            if (!sourceAccount.isAccountActive()) {
                throw new BankingException("Cannot schedule transaction for inactive account: " + sourceAccountNumber);
            }
            
            Transaction transaction = new Transaction(transactionType, amount, sourceAccount, null, description);
            transaction.setScheduledDate(scheduledDate);
            transaction.setStatus(TransactionStatus.SCHEDULED);
            
            entityManager.persist(transaction);
            entityManager.flush();
            
            logger.info("Transaction scheduled successfully. Transaction ID: " + transaction.getTransactionId());
            return transaction;
            
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error scheduling transaction: " + e.getMessage(), e);
            throw new BankingException("Failed to schedule transaction: " + e.getMessage(), e);
        }
    }
    
    /**
     * Finds a transaction by ID
     * @param transactionId Transaction ID to search for
     * @return Transaction entity or null if not found
     */
    @PermitAll  // Allow public access for demo purposes
    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public Transaction retrieveTransactionById(@NotNull String transactionId) {
        try {
            TypedQuery<Transaction> query = entityManager.createNamedQuery("Transaction.findByTransactionId", Transaction.class);
            query.setParameter("transactionId", transactionId);
            return query.getSingleResult();
        } catch (NoResultException e) {
            logger.info("Transaction not found: " + transactionId);
            return null;
        } catch (Exception e) {
            logger.log(Level.WARNING, "Error finding transaction: " + e.getMessage(), e);
            return null;
        }
    }
    
    /**
     * Gets transaction history for an account
     * @param accountNumber Account number
     * @param limit Maximum number of transactions to return
     * @return List of transactions
     * @throws BankingException if retrieval fails
     */
    @PermitAll  // Allow public access for demo purposes
    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public List<Transaction> retrieveAccountTransactionHistory(@NotNull String accountNumber, int limit) throws BankingException {
        try {
            TypedQuery<Transaction> query = entityManager.createNamedQuery("Transaction.findByAccountNumber", Transaction.class);
            query.setParameter("accountNumber", accountNumber);
            query.setMaxResults(limit);
            return query.getResultList();
        } catch (Exception e) {
            logger.log(Level.WARNING, "Error getting transaction history: " + e.getMessage(), e);
            throw new BankingException("Failed to retrieve transaction history: " + e.getMessage(), e);
        }
    }
    
    /**
     * Gets pending transactions
     * @return List of pending transactions
     * @throws BankingException if retrieval fails
     */
    @PermitAll  // Allow public access for demo purposes
    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public List<Transaction> retrievePendingTransactions() throws BankingException {
        try {
            TypedQuery<Transaction> query = entityManager.createNamedQuery("Transaction.findPendingTransactions", Transaction.class);
            return query.getResultList();
        } catch (Exception e) {
            logger.log(Level.WARNING, "Error getting pending transactions: " + e.getMessage(), e);
            throw new BankingException("Failed to retrieve pending transactions: " + e.getMessage(), e);
        }
    }
    
    /**
     * Gets all transaction history
     * @param limit Maximum number of transactions to return
     * @return List of transactions
     * @throws BankingException if retrieval fails
     */
    @PermitAll  // Allow public access for demo purposes
    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public List<Transaction> retrieveAllTransactionHistory(int limit) throws BankingException {
        try {
            TypedQuery<Transaction> query = entityManager.createNamedQuery("Transaction.findAll", Transaction.class);
            query.setMaxResults(limit);
            return query.getResultList();
        } catch (Exception e) {
            logger.log(Level.WARNING, "Error getting all transaction history: " + e.getMessage(), e);
            throw new BankingException("Failed to retrieve all transaction history: " + e.getMessage(), e);
        }
    }
    
    /**
     * Gets weekly transaction report
     * @return Weekly report data
     * @throws BankingException if report generation fails
     */
    @PermitAll  // Allow public access for demo purposes
    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public WeeklyReportDTO generateWeeklyReport() throws BankingException {
        try {
            LocalDateTime weekStart = LocalDateTime.now().minusDays(7);
            
            TypedQuery<Transaction> query = entityManager.createQuery(
                "SELECT t FROM Transaction t WHERE t.createdAt >= :weekStart AND t.status = :status", Transaction.class);
            query.setParameter("weekStart", weekStart);
            query.setParameter("status", TransactionStatus.COMPLETED);
            
            List<Transaction> weeklyTransactions = query.getResultList();
            
            BigDecimal totalVolume = weeklyTransactions.stream()
                .map(Transaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
            
            long transactionCount = weeklyTransactions.size();
            
            WeeklyReportDTO report = new WeeklyReportDTO();
            report.setReportPeriod("Past 7 Days");
            report.setStartDate(weekStart.toLocalDate().toString());
            report.setEndDate(LocalDateTime.now().toLocalDate().toString());
            report.setTotalTransactions((int) transactionCount);
            report.setTotalAmount(totalVolume);
            report.setDepositCount(0);
            report.setWithdrawalCount(0);
            report.setTransferCount(0);
            report.setDepositAmount(BigDecimal.ZERO);
            report.setWithdrawalAmount(BigDecimal.ZERO);
            report.setTransferAmount(BigDecimal.ZERO);
            
            return report;
            
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error generating weekly report: " + e.getMessage(), e);
            throw new BankingException("Failed to generate weekly report: " + e.getMessage(), e);
        }
    }
    
    /**
     * Gets monthly transaction report
     * @return Monthly report data
     * @throws BankingException if report generation fails
     */
    @PermitAll  // Allow public access for demo purposes
    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public MonthlyReportDTO generateMonthlyReport() throws BankingException {
        try {
            LocalDateTime monthStart = LocalDateTime.now().minusDays(30);
            
            TypedQuery<Transaction> query = entityManager.createQuery(
                "SELECT t FROM Transaction t WHERE t.createdAt >= :monthStart AND t.status = :status", Transaction.class);
            query.setParameter("monthStart", monthStart);
            query.setParameter("status", TransactionStatus.COMPLETED);
            
            List<Transaction> monthlyTransactions = query.getResultList();
            
            BigDecimal totalVolume = monthlyTransactions.stream()
                .map(Transaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
            
            long transactionCount = monthlyTransactions.size();
            
            // Calculate daily averages
            BigDecimal dailyAverage = totalVolume.divide(BigDecimal.valueOf(30), 2, RoundingMode.HALF_UP);
            
            // Calculate transaction type breakdown
            long transferCount = monthlyTransactions.stream()
                .filter(t -> t.getTransactionType() == TransactionType.TRANSFER)
                .count();
            
            long depositCount = monthlyTransactions.stream()
                .filter(t -> t.getTransactionType() == TransactionType.DEPOSIT)
                .count();
            
            long withdrawalCount = monthlyTransactions.stream()
                .filter(t -> t.getTransactionType() == TransactionType.WITHDRAWAL)
                .count();
            
            MonthlyReportDTO report = new MonthlyReportDTO();
            report.setReportPeriod("Past 30 Days");
            report.setStartDate(monthStart.toLocalDate().toString());
            report.setEndDate(LocalDateTime.now().toLocalDate().toString());
            report.setTotalTransactions((int) transactionCount);
            report.setTotalAmount(totalVolume);
            report.setDepositCount((int) depositCount);
            report.setWithdrawalCount((int) withdrawalCount);
            report.setTransferCount((int) transferCount);
            report.setInterestCount(0);
            report.setDepositAmount(BigDecimal.ZERO);
            report.setWithdrawalAmount(BigDecimal.ZERO);
            report.setTransferAmount(BigDecimal.ZERO);
            report.setInterestAmount(BigDecimal.ZERO);
            report.setAverageDailyTransactions(transactionCount / 30.0);
            report.setAverageDailyAmount(dailyAverage);
            
            return report;
            
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error generating monthly report: " + e.getMessage(), e);
            throw new BankingException("Failed to generate monthly report: " + e.getMessage(), e);
        }
    }
    
    /**
     * Gets scheduled transactions due for processing
     * @return List of scheduled transactions due
     * @throws BankingException if retrieval fails
     */
    @PermitAll  // Allow public access for demo purposes
    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public List<Transaction> retrieveScheduledTransactionsDue() throws BankingException {
        try {
            TypedQuery<Transaction> query = entityManager.createQuery(
                "SELECT t FROM Transaction t WHERE t.status = :status AND t.scheduledDate <= :now", Transaction.class);
            query.setParameter("status", TransactionStatus.SCHEDULED);
            query.setParameter("now", LocalDateTime.now());
            return query.getResultList();
        } catch (Exception e) {
            logger.log(Level.WARNING, "Error getting scheduled transactions due: " + e.getMessage(), e);
            throw new BankingException("Failed to retrieve scheduled transactions due: " + e.getMessage(), e);
        }
    }
    
    /**
     * Gets all scheduled transactions
     * @return List of all scheduled transactions
     * @throws BankingException if retrieval fails
     */
    @PermitAll  // Allow public access for demo purposes
    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public List<Transaction> retrieveAllScheduledTransactions() throws BankingException {
        try {
            TypedQuery<Transaction> query = entityManager.createNamedQuery("Transaction.findScheduled", Transaction.class);
            return query.getResultList();
        } catch (Exception e) {
            logger.log(Level.WARNING, "Error getting all scheduled transactions: " + e.getMessage(), e);
            throw new BankingException("Failed to retrieve all scheduled transactions: " + e.getMessage(), e);
        }
    }
    
    /**
     * Reverses a completed transaction
     * @param transactionId Transaction ID to reverse
     * @param reason Reason for reversal
     * @return Reversal transaction
     * @throws BankingException if reversal fails
     */
    @RolesAllowed({"ADMIN", "MANAGER"})
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public Transaction reverseCompletedTransaction(@NotNull String transactionId, String reason) throws BankingException {
        try {
            logger.info("Reversing transaction: " + transactionId + ", reason: " + reason);
            
            Transaction originalTransaction = retrieveTransactionById(transactionId);
            if (originalTransaction == null) {
                throw new TransactionNotFoundException("Transaction not found: " + transactionId);
            }
            
            if (originalTransaction.getStatus() != TransactionStatus.COMPLETED) {
                throw new BankingException("Cannot reverse non-completed transaction: " + transactionId);
            }
            
            // Create reversal transaction
            Transaction reversalTransaction = new Transaction(
                originalTransaction.getTransactionType(),
                originalTransaction.getAmount(),
                originalTransaction.getTargetAccount(),
                originalTransaction.getSourceAccount(),
                "REVERSAL: " + (reason != null ? reason : "No reason provided")
            );
            
            reversalTransaction.setStatus(TransactionStatus.PROCESSING);
            entityManager.persist(reversalTransaction);
            
            // Execute the reversal
            if (originalTransaction.getTransactionType() == TransactionType.TRANSFER) {
                executeFundTransfer(
                    originalTransaction.getTargetAccount().getAccountNumber(),
                    originalTransaction.getSourceAccount().getAccountNumber(),
                    originalTransaction.getAmount(),
                    "REVERSAL: " + (reason != null ? reason : "No reason provided")
                );
            } else if (originalTransaction.getTransactionType() == TransactionType.DEPOSIT) {
                processWithdrawalTransaction(
                    originalTransaction.getSourceAccount().getAccountNumber(),
                    originalTransaction.getAmount(),
                    "REVERSAL: " + (reason != null ? reason : "No reason provided")
                );
            } else if (originalTransaction.getTransactionType() == TransactionType.WITHDRAWAL) {
                processDepositTransaction(
                    originalTransaction.getSourceAccount().getAccountNumber(),
                    originalTransaction.getAmount(),
                    "REVERSAL: " + (reason != null ? reason : "No reason provided")
                );
            }
            
            // Mark original transaction as reversed
            originalTransaction.setStatus(TransactionStatus.REVERSED);
            entityManager.merge(originalTransaction);
            
            logger.info("Transaction reversed successfully: " + transactionId);
            return reversalTransaction;
            
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error reversing transaction: " + e.getMessage(), e);
            throw new BankingException("Failed to reverse transaction: " + e.getMessage(), e);
        }
    }
    
    // DTO Classes for Reports
    public static class WeeklyReportDTO {
        private String reportPeriod;
        private String startDate;
        private String endDate;
        private int totalTransactions;
        private BigDecimal totalAmount;
        private int depositCount;
        private int withdrawalCount;
        private int transferCount;
        private BigDecimal depositAmount;
        private BigDecimal withdrawalAmount;
        private BigDecimal transferAmount;
        
        // Getters and Setters
        public String getReportPeriod() { return reportPeriod; }
        public void setReportPeriod(String reportPeriod) { this.reportPeriod = reportPeriod; }
        
        public String getStartDate() { return startDate; }
        public void setStartDate(String startDate) { this.startDate = startDate; }
        
        public String getEndDate() { return endDate; }
        public void setEndDate(String endDate) { this.endDate = endDate; }
        
        public int getTotalTransactions() { return totalTransactions; }
        public void setTotalTransactions(int totalTransactions) { this.totalTransactions = totalTransactions; }
        
        public BigDecimal getTotalAmount() { return totalAmount; }
        public void setTotalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; }
        
        public int getDepositCount() { return depositCount; }
        public void setDepositCount(int depositCount) { this.depositCount = depositCount; }
        
        public int getWithdrawalCount() { return withdrawalCount; }
        public void setWithdrawalCount(int withdrawalCount) { this.withdrawalCount = withdrawalCount; }
        
        public int getTransferCount() { return transferCount; }
        public void setTransferCount(int transferCount) { this.transferCount = transferCount; }
        
        public BigDecimal getDepositAmount() { return depositAmount; }
        public void setDepositAmount(BigDecimal depositAmount) { this.depositAmount = depositAmount; }
        
        public BigDecimal getWithdrawalAmount() { return withdrawalAmount; }
        public void setWithdrawalAmount(BigDecimal withdrawalAmount) { this.withdrawalAmount = withdrawalAmount; }
        
        public BigDecimal getTransferAmount() { return transferAmount; }
        public void setTransferAmount(BigDecimal transferAmount) { this.transferAmount = transferAmount; }
    }
    
    public static class MonthlyReportDTO {
        private String reportPeriod;
        private String startDate;
        private String endDate;
        private int totalTransactions;
        private BigDecimal totalAmount;
        private int depositCount;
        private int withdrawalCount;
        private int transferCount;
        private int interestCount;
        private BigDecimal depositAmount;
        private BigDecimal withdrawalAmount;
        private BigDecimal transferAmount;
        private BigDecimal interestAmount;
        private double averageDailyTransactions;
        private BigDecimal averageDailyAmount;
        
        // Getters and Setters
        public String getReportPeriod() { return reportPeriod; }
        public void setReportPeriod(String reportPeriod) { this.reportPeriod = reportPeriod; }
        
        public String getStartDate() { return startDate; }
        public void setStartDate(String startDate) { this.startDate = startDate; }
        
        public String getEndDate() { return endDate; }
        public void setEndDate(String endDate) { this.endDate = endDate; }
        
        public int getTotalTransactions() { return totalTransactions; }
        public void setTotalTransactions(int totalTransactions) { this.totalTransactions = totalTransactions; }
        
        public BigDecimal getTotalAmount() { return totalAmount; }
        public void setTotalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; }
        
        public int getDepositCount() { return depositCount; }
        public void setDepositCount(int depositCount) { this.depositCount = depositCount; }
        
        public int getWithdrawalCount() { return withdrawalCount; }
        public void setWithdrawalCount(int withdrawalCount) { this.withdrawalCount = withdrawalCount; }
        
        public int getTransferCount() { return transferCount; }
        public void setTransferCount(int transferCount) { this.transferCount = transferCount; }
        
        public int getInterestCount() { return interestCount; }
        public void setInterestCount(int interestCount) { this.interestCount = interestCount; }
        
        public BigDecimal getDepositAmount() { return depositAmount; }
        public void setDepositAmount(BigDecimal depositAmount) { this.depositAmount = depositAmount; }
        
        public BigDecimal getWithdrawalAmount() { return withdrawalAmount; }
        public void setWithdrawalAmount(BigDecimal withdrawalAmount) { this.withdrawalAmount = withdrawalAmount; }
        
        public BigDecimal getTransferAmount() { return transferAmount; }
        public void setTransferAmount(BigDecimal transferAmount) { this.transferAmount = transferAmount; }
        
        public BigDecimal getInterestAmount() { return interestAmount; }
        public void setInterestAmount(BigDecimal interestAmount) { this.interestAmount = interestAmount; }
        
        public double getAverageDailyTransactions() { return averageDailyTransactions; }
        public void setAverageDailyTransactions(double averageDailyTransactions) { this.averageDailyTransactions = averageDailyTransactions; }
        
        public BigDecimal getAverageDailyAmount() { return averageDailyAmount; }
        public void setAverageDailyAmount(BigDecimal averageDailyAmount) { this.averageDailyAmount = averageDailyAmount; }
    }
} 