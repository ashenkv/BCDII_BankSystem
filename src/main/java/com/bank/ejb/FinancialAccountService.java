package com.bank.ejb;

import com.bank.entity.FinancialAccount;
import com.bank.entity.AccountStatus;
import com.bank.entity.AccountType;
import com.bank.entity.Customer;
import com.bank.exception.AccountNotFoundException;
import com.bank.exception.BankingException;
import com.bank.exception.InsufficientFundsException;
import com.bank.interceptor.AuditInterceptor;
import com.bank.interceptor.LoggingInterceptor;

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
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * FinancialAccountService EJB - Handles all financial account-related operations
 * Implements transaction management, security, and business logic for accounts
 */
@Stateless
@Interceptors({LoggingInterceptor.class, AuditInterceptor.class})
public class FinancialAccountService {
    
    private static final Logger logger = Logger.getLogger(FinancialAccountService.class.getName());
    
    @PersistenceContext(unitName = "bankingPU")
    private EntityManager entityManager;
    
    @EJB
    private ClientManagementService clientManagementService;
    
    /**
     * Creates a new financial account
     * @param customerId Customer ID who owns the account
     * @param accountType Type of account to create
     * @param initialDeposit Initial deposit amount
     * @return Created account
     * @throws BankingException if account creation fails
     */
    @PermitAll  // Allow public access for demo purposes
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public FinancialAccount establishNewAccount(@NotNull String customerId, @NotNull AccountType accountType, BigDecimal initialDeposit) throws BankingException {
        try {
            logger.info("Creating new financial account for customer: " + customerId);
            
            // Find customer
            logger.info("Looking for customer with ID: " + customerId);
            Customer customer = clientManagementService.retrieveClientByCustomerId(customerId);
            if (customer == null) {
                logger.warning("Customer not found with ID: " + customerId);
                throw new BankingException("Customer not found: " + customerId);
            }
            
            logger.info("Found customer: " + customer.getCustomerId() + " - " + customer.getFirstName() + " " + customer.getLastName());
            
            if (!clientManagementService.isClientActive(customer)) {
                logger.warning("Customer is not active: " + customerId);
                throw new BankingException("Cannot create account for inactive customer: " + customerId);
            }
            
            // Generate unique account number
            String accountNumber = generateUniqueAccountNumber();
            logger.info("Generated account number: " + accountNumber);
            
            // Create account
            FinancialAccount account = new FinancialAccount(accountNumber, accountType, customer);
            logger.info("Created account object with customer: " + account.getCustomer().getCustomerId());
            
            // Set initial deposit if provided
            if (initialDeposit != null && initialDeposit.compareTo(BigDecimal.ZERO) > 0) {
                account.setBalance(initialDeposit);
                account.setAvailableBalance(initialDeposit);
            }
            
            // Set default values based on account type
            configureAccountDefaults(account);
            
            // Ensure all required fields are set before persistence
            if (account.getStatus() == null) {
                account.setStatus(AccountStatus.ACTIVE);
            }
            if (account.getCreatedAt() == null) {
                account.setCreatedAt(LocalDateTime.now());
            }
            
            // Validate the account before persisting
            if (account.getCustomer() == null) {
                logger.severe("Customer relationship not properly established");
                throw new BankingException("Customer relationship not properly established");
            }
            
            logger.info("About to persist account with number: " + account.getAccountNumber());
            entityManager.persist(account);
            entityManager.flush();
            
            logger.info("Financial account created successfully: " + account.getAccountNumber());
            return account;
            
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error creating financial account: " + e.getMessage(), e);
            throw new BankingException("Failed to create financial account: " + e.getMessage(), e);
        }
    }
    
    /**
     * Finds an account by account number
     * @param accountNumber Account number to search for
     * @return Account entity or null if not found
     */
    @PermitAll  // Allow public access for demo purposes
    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public FinancialAccount retrieveAccountByNumber(@NotNull String accountNumber) {
        try {
            TypedQuery<FinancialAccount> query = entityManager.createNamedQuery("FinancialAccount.findByAccountNumber", FinancialAccount.class);
            query.setParameter("accountNumber", accountNumber);
            return query.getSingleResult();
        } catch (NoResultException e) {
            logger.info("Account not found: " + accountNumber);
            return null;
        } catch (Exception e) {
            logger.log(Level.WARNING, "Error finding account: " + e.getMessage(), e);
            return null;
        }
    }
    
    /**
     * Gets all accounts for a customer
     * @param customerId Customer ID
     * @return List of customer's accounts
     */
    @PermitAll  // Allow public access for demo purposes
    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public List<FinancialAccount> retrieveCustomerAccounts(@NotNull String customerId) throws BankingException {
        try {
            Customer customer = clientManagementService.retrieveClientByCustomerId(customerId);
            if (customer == null) {
                throw new BankingException("Customer not found: " + customerId);
            }
            
            TypedQuery<FinancialAccount> query = entityManager.createNamedQuery("FinancialAccount.findByCustomer", FinancialAccount.class);
            query.setParameter("customer", customer);
            return query.getResultList();
            
        } catch (Exception e) {
            logger.log(Level.WARNING, "Error getting accounts by customer: " + e.getMessage(), e);
            throw new BankingException("Failed to retrieve customer accounts: " + e.getMessage(), e);
        }
    }
    
    /**
     * Gets all active accounts
     * @return List of active accounts
     */
    @PermitAll  // Allow public access for demo purposes
    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public List<FinancialAccount> retrieveActiveAccounts() throws BankingException {
        try {
            TypedQuery<FinancialAccount> query = entityManager.createNamedQuery("FinancialAccount.findActiveAccounts", FinancialAccount.class);
            return query.getResultList();
        } catch (Exception e) {
            logger.log(Level.WARNING, "Error getting active accounts: " + e.getMessage(), e);
            throw new BankingException("Failed to retrieve active accounts: " + e.getMessage(), e);
        }
    }
    
    /**
     * Deposits money into an account
     * @param accountNumber Account number
     * @param amount Amount to deposit
     * @return Updated account
     * @throws BankingException if deposit fails
     */
    @RolesAllowed({"ADMIN", "MANAGER", "EMPLOYEE", "CUSTOMER"})
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public FinancialAccount processDeposit(@NotNull String accountNumber, @NotNull BigDecimal amount) throws BankingException {
        try {
            logger.info("Processing deposit to account: " + accountNumber + ", amount: " + amount);
            
            if (amount.compareTo(BigDecimal.ZERO) <= 0) {
                throw new BankingException("Deposit amount must be positive");
            }
            
            FinancialAccount account = retrieveAccountByNumber(accountNumber);
            if (account == null) {
                throw new AccountNotFoundException("Account not found: " + accountNumber);
            }
            
            if (!account.isAccountActive()) {
                throw new BankingException("Cannot deposit to inactive account: " + accountNumber);
            }
            
            account.addFunds(amount);
            FinancialAccount updatedAccount = entityManager.merge(account);
            entityManager.flush();
            
            logger.info("Deposit completed successfully. New balance: " + updatedAccount.getBalance());
            return updatedAccount;
            
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error processing deposit: " + e.getMessage(), e);
            throw new BankingException("Failed to process deposit: " + e.getMessage(), e);
        }
    }
    
    /**
     * Withdraws money from an account
     * @param accountNumber Account number
     * @param amount Amount to withdraw
     * @return Updated account
     * @throws BankingException if withdrawal fails
     */
    @RolesAllowed({"ADMIN", "MANAGER", "EMPLOYEE", "CUSTOMER"})
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public FinancialAccount processWithdrawal(@NotNull String accountNumber, @NotNull BigDecimal amount) throws BankingException {
        try {
            logger.info("Processing withdrawal from account: " + accountNumber + ", amount: " + amount);
            
            if (amount.compareTo(BigDecimal.ZERO) <= 0) {
                throw new BankingException("Withdrawal amount must be positive");
            }
            
            FinancialAccount account = retrieveAccountByNumber(accountNumber);
            if (account == null) {
                throw new AccountNotFoundException("Account not found: " + accountNumber);
            }
            
            if (!account.isAccountActive()) {
                throw new BankingException("Cannot withdraw from inactive account: " + accountNumber);
            }
            
            if (!account.validateWithdrawalAmount(amount)) {
                throw new InsufficientFundsException("Insufficient funds for withdrawal: " + amount);
            }
            
            account.deductFunds(amount);
            FinancialAccount updatedAccount = entityManager.merge(account);
            entityManager.flush();
            
            logger.info("Withdrawal completed successfully. New balance: " + updatedAccount.getBalance());
            return updatedAccount;
            
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error processing withdrawal: " + e.getMessage(), e);
            throw new BankingException("Failed to process withdrawal: " + e.getMessage(), e);
        }
    }
    
    /**
     * Updates account information
     * @param account Account to update
     * @return Updated account
     * @throws BankingException if update fails
     */
    @RolesAllowed({"ADMIN", "MANAGER", "EMPLOYEE"})
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public FinancialAccount modifyAccountDetails(@Valid @NotNull FinancialAccount account) throws BankingException {
        try {
            logger.info("Updating account: " + account.getAccountNumber());
            
            FinancialAccount existingAccount = retrieveAccountByNumber(account.getAccountNumber());
            if (existingAccount == null) {
                throw new AccountNotFoundException("Account not found: " + account.getAccountNumber());
            }
            
            FinancialAccount updatedAccount = entityManager.merge(account);
            entityManager.flush();
            
            logger.info("Account updated successfully: " + updatedAccount.getAccountNumber());
            return updatedAccount;
            
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error updating account: " + e.getMessage(), e);
            throw new BankingException("Failed to update account: " + e.getMessage(), e);
        }
    }
    
    /**
     * Closes an account
     * @param accountNumber Account number to close
     * @throws BankingException if account closure fails
     */
    @RolesAllowed({"ADMIN", "MANAGER"})
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public void terminateAccount(@NotNull String accountNumber) throws BankingException {
        try {
            logger.info("Closing account: " + accountNumber);
            
            FinancialAccount account = retrieveAccountByNumber(accountNumber);
            if (account == null) {
                throw new AccountNotFoundException("Account not found: " + accountNumber);
            }
            
            if (account.getBalance().compareTo(BigDecimal.ZERO) != 0) {
                throw new BankingException("Cannot close account with non-zero balance: " + account.getBalance());
            }
            
            account.setStatus(AccountStatus.CLOSED);
            entityManager.merge(account);
            entityManager.flush();
            
            logger.info("Account closed successfully: " + accountNumber);
            
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error closing account: " + e.getMessage(), e);
            throw new BankingException("Failed to close account: " + e.getMessage(), e);
        }
    }
    
    /**
     * Freezes an account
     * @param accountNumber Account number to freeze
     * @throws BankingException if account freeze fails
     */
    @RolesAllowed({"ADMIN", "MANAGER"})
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public void suspendAccount(@NotNull String accountNumber) throws BankingException {
        try {
            logger.info("Freezing account: " + accountNumber);
            
            FinancialAccount account = retrieveAccountByNumber(accountNumber);
            if (account == null) {
                throw new AccountNotFoundException("Account not found: " + accountNumber);
            }
            
            account.setStatus(AccountStatus.FROZEN);
            entityManager.merge(account);
            entityManager.flush();
            
            logger.info("Account frozen successfully: " + accountNumber);
            
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error freezing account: " + e.getMessage(), e);
            throw new BankingException("Failed to freeze account: " + e.getMessage(), e);
        }
    }
    
    /**
     * Gets account balance
     * @param accountNumber Account number
     * @return Account balance
     * @throws BankingException if balance retrieval fails
     */
    @RolesAllowed({"ADMIN", "MANAGER", "EMPLOYEE", "CUSTOMER"})
    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public BigDecimal retrieveAccountBalance(@NotNull String accountNumber) throws BankingException {
        try {
            FinancialAccount account = retrieveAccountByNumber(accountNumber);
            if (account == null) {
                throw new AccountNotFoundException("Account not found: " + accountNumber);
            }
            return account.getBalance();
        } catch (Exception e) {
            logger.log(Level.WARNING, "Error retrieving account balance: " + e.getMessage(), e);
            throw new BankingException("Failed to retrieve account balance: " + e.getMessage(), e);
        }
    }
    
    // Private helper methods
    private String generateUniqueAccountNumber() {
        return "ACC" + System.currentTimeMillis() % 1000000 + UUID.randomUUID().toString().substring(0, 4).toUpperCase();
    }
    
    private void configureAccountDefaults(FinancialAccount account) {
        switch (account.getAccountType()) {
            case SAVINGS:
                account.setInterestRate(new BigDecimal("0.025")); // 2.5% annual interest
                account.setOverdraftLimit(BigDecimal.ZERO);
                break;
            case CHECKING:
                account.setInterestRate(new BigDecimal("0.005")); // 0.5% annual interest
                account.setOverdraftLimit(new BigDecimal("500.00"));
                break;
            case BUSINESS:
                account.setInterestRate(new BigDecimal("0.015")); // 1.5% annual interest
                account.setOverdraftLimit(new BigDecimal("1000.00"));
                break;
            default:
                account.setInterestRate(BigDecimal.ZERO);
                account.setOverdraftLimit(BigDecimal.ZERO);
        }
    }
} 