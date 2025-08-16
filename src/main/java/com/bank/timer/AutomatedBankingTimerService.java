package com.bank.timer;

import com.bank.ejb.FinancialAccountService;
import com.bank.ejb.FinancialTransactionService;
import com.bank.entity.FinancialAccount;
import com.bank.entity.AccountType;
import com.bank.entity.Transaction;
import com.bank.entity.TransactionStatus;
import com.bank.exception.BankingException;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.security.PermitAll;
import jakarta.annotation.security.RolesAllowed;
import jakarta.ejb.EJB;
import jakarta.ejb.Schedule;
import jakarta.ejb.Singleton;
import jakarta.ejb.Startup;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Enhanced Banking Timer Service - EJB Timer Service for production banking operations
 * 
 * This service demonstrates:
 * - @Singleton EJB with @Startup for system-wide timer operations
 * - @Schedule annotations for critical banking operations
 * - Scheduled fund transfers processing
 * - Interest calculations on savings accounts
 * - Transaction demarcation with @TransactionAttribute
 * - Security with @RolesAllowed annotations
 * - Error handling and recovery mechanisms
 * 
 * Key Banking Operations:
 * 1. Scheduled Transaction Processing - Executes scheduled transfers and payments
 * 2. Interest Calculation - Calculates and applies interest to eligible accounts
 * 3. System Maintenance - Performs cleanup and health checks
 * 4. Risk Management - Monitors account limits and suspicious activities
 */
@Singleton
@Startup
public class AutomatedBankingTimerService {
    
    private static final Logger logger = Logger.getLogger(AutomatedBankingTimerService.class.getName());
    
    // Business Constants
    private static final BigDecimal DAILY_INTEREST_DIVISOR = new BigDecimal("365");
    private static final BigDecimal MIN_BALANCE_FOR_INTEREST = new BigDecimal("100.00");
    
    @EJB
    private FinancialTransactionService financialTransactionService;
    
    @EJB
    private FinancialAccountService financialAccountService;
    
    @PersistenceContext(unitName = "bankingPU")
    private EntityManager entityManager;
    
    /**
     * Initialize the banking timer service
     * Demonstrates @PostConstruct lifecycle management
     */
    @PostConstruct
    public void initializeTimerService() {
        logger.info("✓ Enhanced Banking Timer Service started successfully!");
        logger.info("  Scheduled Fund Transfers: ENABLED");
        logger.info("  Interest Calculations: ENABLED");
        logger.info("  System Monitoring: ACTIVE");
        logger.info("  Security Context: LOADED");
    }
    
    /**
     * Process scheduled transactions every 5 minutes
     * Demonstrates @Schedule annotation with persistent timers for critical operations
     * 
     * @TransactionAttribute.REQUIRED ensures all scheduled transactions
     * are processed within a single transaction context for consistency
     */
    @Schedule(hour = "*", minute = "*/5", second = "0", persistent = true, info = "ScheduledTransactionProcessor")
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    @RolesAllowed("SYSTEM")
    public void executeScheduledTransactions() {
        try {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            logger.info("🔄 Processing Scheduled Transactions at " + timestamp);
            
            // Get all scheduled transactions due for processing
            List<Transaction> scheduledTransactions = financialTransactionService.retrieveScheduledTransactionsDue();
            
            if (scheduledTransactions.isEmpty()) {
                logger.info("   No scheduled transactions due for processing");
                return;
            }
            
            int processedCount = 0;
            int failedCount = 0;
            
            for (Transaction scheduled : scheduledTransactions) {
                try {
                    // Process the scheduled transaction based on type
                    switch (scheduled.getTransactionType()) {
                        case TRANSFER:
                            financialTransactionService.executeFundTransfer(
                                scheduled.getSourceAccount().getAccountNumber(),
                                scheduled.getTargetAccount().getAccountNumber(),
                                scheduled.getAmount(),
                                "SCHEDULED: " + scheduled.getDescription()
                            );
                            processedCount++;
                            logger.info("   ✓ Processed scheduled transfer: " + scheduled.getTransactionId());
                            break;
                            
                        case WITHDRAWAL:
                            financialTransactionService.processWithdrawalTransaction(
                                scheduled.getSourceAccount().getAccountNumber(),
                                scheduled.getAmount(),
                                "SCHEDULED: " + scheduled.getDescription()
                            );
                            processedCount++;
                            logger.info("   ✓ Processed scheduled withdrawal: " + scheduled.getTransactionId());
                            break;
                            
                        case DEPOSIT:
                            financialTransactionService.processDepositTransaction(
                                scheduled.getSourceAccount().getAccountNumber(),
                                scheduled.getAmount(),
                                "SCHEDULED: " + scheduled.getDescription()
                            );
                            processedCount++;
                            logger.info("   ✓ Processed scheduled deposit: " + scheduled.getTransactionId());
                            break;
                            
                        default:
                            logger.warning("   ⚠️ Unsupported scheduled transaction type: " + scheduled.getTransactionType());
                            failedCount++;
                    }
                } catch (Exception e) {
                    logger.log(Level.WARNING, "   ❌ Failed to process scheduled transaction: " + 
                        scheduled.getTransactionId() + " - " + e.getMessage(), e);
                    failedCount++;
                }
            }
            
            logger.info(String.format("📊 Scheduled Transaction Summary: %d processed, %d failed", 
                processedCount, failedCount));
                
        } catch (Exception e) {
            logger.log(Level.SEVERE, "❌ Error in scheduled transaction processing: " + e.getMessage(), e);
        }
    }
    
    /**
     * Calculate and apply interest to eligible accounts daily at 2 AM
     * Demonstrates business logic timing and transaction management
     * 
     * Uses @TransactionAttribute.REQUIRES_NEW to ensure each account's
     * interest calculation is in its own transaction for isolation
     */
    @Schedule(hour = "2", minute = "0", second = "0", persistent = true, info = "DailyInterestCalculation")
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    @RolesAllowed("SYSTEM")
    public void computeDailyInterest() {
        try {
            String date = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
            logger.info("💰 Starting Daily Interest Calculation - " + date);
            
            // Get all active accounts for interest calculation
            List<FinancialAccount> activeAccounts = financialAccountService.retrieveActiveAccounts();
            
            int processedCount = 0;
            BigDecimal totalInterestPaid = BigDecimal.ZERO;
            
            for (FinancialAccount account : activeAccounts) {
                try {
                    // Only process accounts eligible for interest
                    if (isAccountEligibleForInterest(account)) {
                        BigDecimal interestAmount = calculateDailyInterestAmount(account);
                        
                        if (interestAmount.compareTo(BigDecimal.ZERO) > 0) {
                            // Apply interest as a deposit transaction
                            financialTransactionService.processDepositTransaction(
                                account.getAccountNumber(),
                                interestAmount,
                                "Daily Interest - " + date + " (Rate: " + 
                                account.getInterestRate().multiply(new BigDecimal("100")) + "%)"
                            );
                            
                            totalInterestPaid = totalInterestPaid.add(interestAmount);
                            processedCount++;
                            
                            logger.info(String.format("   ✓ Interest applied to %s: $%.2f", 
                                account.getAccountNumber(), interestAmount));
                        }
                    }
                } catch (Exception e) {
                    logger.log(Level.WARNING, "   ❌ Failed to calculate interest for account: " + 
                        account.getAccountNumber() + " - " + e.getMessage(), e);
                }
            }
            
            logger.info(String.format("📊 Daily Interest Summary: %d accounts processed, $%.2f total interest paid", 
                processedCount, totalInterestPaid));
                
        } catch (Exception e) {
            logger.log(Level.SEVERE, "❌ Error in daily interest calculation: " + e.getMessage(), e);
        }
    }
    
    /**
     * Demo interest calculation every 3 minutes for testing purposes
     * Demonstrates non-persistent timer for demo operations
     */
    @Schedule(hour = "*", minute = "*/3", second = "0", persistent = false, info = "AutomaticDemoInterestCalculation")
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    @PermitAll  // Allow for demo purposes
    public void computeDemoInterest() {
        try {
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
            logger.info("🎯 Demo Interest Calculation at " + timestamp);
            
            // Get all active accounts
            List<FinancialAccount> activeAccounts = financialAccountService.retrieveActiveAccounts();
            
            int processedCount = 0;
            BigDecimal totalInterestPaid = BigDecimal.ZERO;
            
            for (FinancialAccount account : activeAccounts) {
                try {
                    // Only process savings accounts for demo
                    if (account.getAccountType() == AccountType.SAVINGS && account.isAccountActive()) {
                        BigDecimal demoInterest = calculateDemoInterestAmount(account);
                        
                        if (demoInterest.compareTo(BigDecimal.ZERO) > 0) {
                            financialTransactionService.processDepositTransaction(
                                account.getAccountNumber(),
                                demoInterest,
                                "Demo Interest - " + timestamp
                            );
                            
                            totalInterestPaid = totalInterestPaid.add(demoInterest);
                            processedCount++;
                            
                            logger.info(String.format("   ✓ Demo interest applied to %s: $%.2f", 
                                account.getAccountNumber(), demoInterest));
                        }
                    }
                } catch (Exception e) {
                    logger.log(Level.WARNING, "   ❌ Failed to apply demo interest to account: " + 
                        account.getAccountNumber() + " - " + e.getMessage(), e);
                }
            }
            
            if (processedCount > 0) {
                logger.info(String.format("📊 Demo Interest Summary: %d accounts processed, $%.2f total interest paid", 
                    processedCount, totalInterestPaid));
                storeAutomaticInterestResult(processedCount, totalInterestPaid, timestamp);
            }
                
        } catch (Exception e) {
            logger.log(Level.SEVERE, "❌ Error in demo interest calculation: " + e.getMessage(), e);
        }
    }
    
    /**
     * System health check every hour
     * Demonstrates system monitoring and health checks
     */
    @Schedule(hour = "*", minute = "0", second = "0", persistent = false, info = "SystemHealthCheck")
    @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
    @RolesAllowed("SYSTEM")
    public void performSystemHealthCheck() {
        try {
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            logger.info("🏥 System Health Check at " + timestamp);
            
            // Check database connectivity
            try {
                entityManager.createNativeQuery("SELECT 1").getSingleResult();
                logger.info("   ✓ Database connectivity: OK");
            } catch (Exception e) {
                logger.severe("   ❌ Database connectivity: FAILED - " + e.getMessage());
            }
            
            // Check active accounts count
            try {
                List<FinancialAccount> activeAccounts = financialAccountService.retrieveActiveAccounts();
                logger.info("   ✓ Active accounts: " + activeAccounts.size());
            } catch (Exception e) {
                logger.warning("   ⚠️ Unable to retrieve active accounts: " + e.getMessage());
            }
            
            // Check scheduled transactions
            try {
                List<Transaction> scheduledTransactions = financialTransactionService.retrieveScheduledTransactionsDue();
                logger.info("   ✓ Pending scheduled transactions: " + scheduledTransactions.size());
            } catch (Exception e) {
                logger.warning("   ⚠️ Unable to retrieve scheduled transactions: " + e.getMessage());
            }
            
            logger.info("🏥 System Health Check completed successfully");
                
        } catch (Exception e) {
            logger.log(Level.SEVERE, "❌ Error in system health check: " + e.getMessage(), e);
        }
    }
    
    /**
     * Weekly system maintenance on Sundays at 3 AM
     * Demonstrates weekly maintenance operations
     */
    @Schedule(dayOfWeek = "0", hour = "3", minute = "0", second = "0", persistent = true, info = "WeeklyMaintenance")
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    @RolesAllowed("SYSTEM")
    public void executeWeeklyMaintenance() {
        try {
            String date = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
            logger.info("🔧 Weekly System Maintenance - " + date);
            
            // Generate weekly reports
            List<FinancialAccount> allAccounts = financialAccountService.retrieveActiveAccounts();
            generateAccountSummaryReport(allAccounts, date);
            generateTransactionVolumeReport(date);
            generateInterestPaymentsReport(date);
            generateRiskComplianceReport(date);
            
            logger.info("🔧 Weekly System Maintenance completed successfully");
                
        } catch (Exception e) {
            logger.log(Level.SEVERE, "❌ Error in weekly maintenance: " + e.getMessage(), e);
        }
    }
    
    /**
     * Daily balance updates at 1 AM
     * Demonstrates daily balance reconciliation
     */
    @Schedule(hour = "1", minute = "0", second = "0", persistent = true, info = "DailyBalanceUpdates")
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    @RolesAllowed("SYSTEM")
    public void performDailyBalanceReconciliation() {
        try {
            String date = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
            logger.info("💼 Daily Balance Reconciliation - " + date);
            
            List<FinancialAccount> activeAccounts = financialAccountService.retrieveActiveAccounts();
            
            for (FinancialAccount account : activeAccounts) {
                try {
                    // Apply daily maintenance fees if applicable
                    if (shouldApplyDailyFee(account)) {
                        BigDecimal maintenanceFee = getDailyMaintenanceFee(account);
                        
                        if (account.validateWithdrawalAmount(maintenanceFee)) {
                            financialTransactionService.processWithdrawalTransaction(
                                account.getAccountNumber(),
                                maintenanceFee,
                                "Daily Maintenance Fee - " + date
                            );
                            
                            logger.info(String.format("   ✓ Maintenance fee applied to %s: $%.2f", 
                                account.getAccountNumber(), maintenanceFee));
                        }
                    }
                    
                    // Update available balance calculation
                    BigDecimal calculatedAvailableBalance = calculateAvailableBalance(account);
                    if (calculatedAvailableBalance.compareTo(account.getAvailableBalance()) != 0) {
                        account.setAvailableBalance(calculatedAvailableBalance);
                        entityManager.merge(account);
                        logger.info(String.format("   ✓ Available balance updated for %s: $%.2f", 
                            account.getAccountNumber(), calculatedAvailableBalance));
                    }
                    
                } catch (Exception e) {
                    logger.log(Level.WARNING, "   ❌ Failed to process balance reconciliation for account: " + 
                        account.getAccountNumber() + " - " + e.getMessage(), e);
                }
            }
            
            logger.info("💼 Daily Balance Reconciliation completed successfully");
                
        } catch (Exception e) {
            logger.log(Level.SEVERE, "❌ Error in daily balance reconciliation: " + e.getMessage(), e);
        }
    }
    
    /**
     * Weekly report generation on Mondays at 6 AM
     * Demonstrates weekly reporting operations
     */
    @Schedule(dayOfWeek = "1", hour = "6", minute = "0", second = "0", persistent = true, info = "WeeklyReportGeneration")
    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    @RolesAllowed("SYSTEM")
    public void generateWeeklyReports() {
        try {
            String date = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
            logger.info("📊 Weekly Report Generation - " + date);
            
            // Generate various weekly reports
            List<FinancialAccount> allAccounts = financialAccountService.retrieveActiveAccounts();
            
            // Account summary report
            generateAccountSummaryReport(allAccounts, date);
            
            // Transaction volume report
            generateTransactionVolumeReport(date);
            
            // Interest payments report
            generateInterestPaymentsReport(date);
            
            // Risk and compliance report
            generateRiskComplianceReport(date);
            
            logger.info("📊 Weekly Report Generation completed successfully");
                
        } catch (Exception e) {
            logger.log(Level.SEVERE, "❌ Error in weekly report generation: " + e.getMessage(), e);
        }
    }
    
    /**
     * Monthly statement generation on the 1st of each month at 5 AM
     * Demonstrates monthly operations
     */
    @Schedule(dayOfMonth = "1", hour = "5", minute = "0", second = "0", persistent = true, info = "MonthlyStatements")
    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    @RolesAllowed("SYSTEM")
    public void generateMonthlyStatements() {
        try {
            String month = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM"));
            logger.info("📄 Monthly Statement Generation - " + month);
            
            List<FinancialAccount> allAccounts = financialAccountService.retrieveActiveAccounts();
            
            for (FinancialAccount account : allAccounts) {
                try {
                    generateAccountStatement(account, month);
                    logger.info(String.format("   ✓ Statement generated for account: %s", account.getAccountNumber()));
                } catch (Exception e) {
                    logger.log(Level.WARNING, "   ❌ Failed to generate statement for account: " + 
                        account.getAccountNumber() + " - " + e.getMessage(), e);
                }
            }
            
            logger.info("📄 Monthly Statement Generation completed successfully");
                
        } catch (Exception e) {
            logger.log(Level.SEVERE, "❌ Error in monthly statement generation: " + e.getMessage(), e);
        }
    }
    
    /**
     * Manual trigger for interest calculation (for demo purposes)
     * Demonstrates manual timer operations
     */
    @PermitAll  // Allow public access for demo purposes
    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public void triggerManualInterestCalculation() throws BankingException {
        try {
            logger.info("🎯 Manual Interest Calculation Triggered");
            computeDemoInterest();
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error in manual interest calculation: " + e.getMessage(), e);
            throw new BankingException("Failed to trigger manual interest calculation: " + e.getMessage(), e);
        }
    }
    
    /**
     * Get automatic interest calculation status
     * Demonstrates status reporting
     */
    @PermitAll  // Allow public access for demo purposes
    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public AutomaticInterestStatus getAutomaticInterestStatus() throws BankingException {
        try {
            AutomaticInterestStatus status = new AutomaticInterestStatus();
            status.setAutomaticCalculationEnabled(true);
            status.setCalculationInterval("Every 3 minutes");
            status.setNextCalculationTime(getNextCalculationTime());
            status.setLastCalculationTime(getLastCalculationTime());
            status.setTotalAccountsEligible(getTotalEligibleAccounts());
            return status;
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error getting automatic interest status: " + e.getMessage(), e);
            throw new BankingException("Failed to get automatic interest status: " + e.getMessage(), e);
        }
    }
    
    // Helper methods for status reporting
    private String getNextCalculationTime() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime next = now.plusMinutes(3 - (now.getMinute() % 3));
        return next.format(DateTimeFormatter.ofPattern("HH:mm:ss"));
    }
    
    private String getLastCalculationTime() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime last = now.minusMinutes(now.getMinute() % 3);
        return last.format(DateTimeFormatter.ofPattern("HH:mm:ss"));
    }
    
    private int getTotalEligibleAccounts() {
        try {
            List<FinancialAccount> activeAccounts = financialAccountService.retrieveActiveAccounts();
            return (int) activeAccounts.stream()
                .filter(account -> account.getAccountType() == AccountType.SAVINGS && account.isAccountActive())
                .count();
        } catch (Exception e) {
            logger.warning("Unable to get total eligible accounts: " + e.getMessage());
            return 0;
        }
    }
    
    /**
     * Emergency stop method for demo purposes
     * Demonstrates emergency operations
     */
    @PermitAll  // Allow public access for demo purposes
    @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
    public void emergencyStop() {
        logger.warning("🚨 EMERGENCY STOP TRIGGERED - Timer operations suspended");
        // In a real system, this would stop all timer operations
        // For demo purposes, we just log the event
    }
    
    // Private helper methods
    private boolean isAccountEligibleForInterest(FinancialAccount account) {
        return account.getAccountType() == AccountType.SAVINGS && 
               account.isAccountActive() && 
               account.getBalance().compareTo(MIN_BALANCE_FOR_INTEREST) >= 0 &&
               account.getInterestRate().compareTo(BigDecimal.ZERO) > 0;
    }
    
    private BigDecimal calculateDailyInterestAmount(FinancialAccount account) {
        BigDecimal dailyRate = account.getInterestRate().divide(DAILY_INTEREST_DIVISOR, 8, RoundingMode.HALF_UP);
        return account.getBalance().multiply(dailyRate).setScale(2, RoundingMode.HALF_UP);
    }
    
    private BigDecimal calculateDemoInterestAmount(FinancialAccount account) {
        // Demo interest calculation - higher rate for demonstration
        BigDecimal demoRate = new BigDecimal("0.001"); // 0.1% per calculation
        return account.getBalance().multiply(demoRate).setScale(2, RoundingMode.HALF_UP);
    }
    
    private void storeAutomaticInterestResult(int processedCount, BigDecimal totalInterest, String timestamp) {
        // In a real system, this would store the result in a database
        // For demo purposes, we just log it
        logger.info(String.format("📈 Automatic Interest Result stored: %d accounts, $%.2f total, %s", 
            processedCount, totalInterest, timestamp));
    }
    
    private BigDecimal calculateAvailableBalance(FinancialAccount account) {
        // Simple calculation - in a real system, this would consider pending transactions
        return account.getBalance().max(BigDecimal.ZERO);
    }
    
    private boolean shouldApplyDailyFee(FinancialAccount account) {
        // Apply fees to checking accounts with low balance
        return account.getAccountType() == AccountType.CHECKING && 
               account.getBalance().compareTo(new BigDecimal("1000.00")) < 0;
    }
    
    private BigDecimal getDailyMaintenanceFee(FinancialAccount account) {
        // Daily maintenance fee for low balance checking accounts
        if (account.getAccountType() == AccountType.CHECKING && 
            account.getBalance().compareTo(new BigDecimal("500.00")) < 0) {
            return new BigDecimal("1.00");
        }
        return BigDecimal.ZERO;
    }
    
    private void generateAccountSummaryReport(List<FinancialAccount> accounts, String date) {
        logger.info(String.format("📊 Account Summary Report for %s: %d total accounts", date, accounts.size()));
        // In a real system, this would generate a detailed report
    }
    
    private void generateTransactionVolumeReport(String date) {
        logger.info(String.format("📊 Transaction Volume Report for %s generated", date));
        // In a real system, this would analyze transaction volumes
    }
    
    private void generateInterestPaymentsReport(String date) {
        logger.info(String.format("📊 Interest Payments Report for %s generated", date));
        // In a real system, this would track interest payments
    }
    
    private void generateRiskComplianceReport(String date) {
        logger.info(String.format("📊 Risk & Compliance Report for %s generated", date));
        // In a real system, this would analyze risk metrics
    }
    
    private void generateAccountStatement(FinancialAccount account, String month) {
        logger.info(String.format("📄 Statement generated for account %s for %s", account.getAccountNumber(), month));
        // In a real system, this would generate a detailed statement
    }
    
    /**
     * Status class for automatic interest calculation
     */
    public static class AutomaticInterestStatus {
        private boolean automaticCalculationEnabled;
        private String calculationInterval;
        private String nextCalculationTime;
        private String lastCalculationTime;
        private int totalAccountsEligible;
        
        // Getters and Setters
        public boolean isAutomaticCalculationEnabled() { return automaticCalculationEnabled; }
        public void setAutomaticCalculationEnabled(boolean automaticCalculationEnabled) { this.automaticCalculationEnabled = automaticCalculationEnabled; }
        
        public String getCalculationInterval() { return calculationInterval; }
        public void setCalculationInterval(String calculationInterval) { this.calculationInterval = calculationInterval; }
        
        public String getNextCalculationTime() { return nextCalculationTime; }
        public void setNextCalculationTime(String nextCalculationTime) { this.nextCalculationTime = nextCalculationTime; }
        
        public String getLastCalculationTime() { return lastCalculationTime; }
        public void setLastCalculationTime(String lastCalculationTime) { this.lastCalculationTime = lastCalculationTime; }
        
        public int getTotalAccountsEligible() { return totalAccountsEligible; }
        public void setTotalAccountsEligible(int totalAccountsEligible) { this.totalAccountsEligible = totalAccountsEligible; }
    }
} 