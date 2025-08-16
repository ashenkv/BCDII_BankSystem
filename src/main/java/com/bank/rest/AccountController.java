package com.bank.rest;

import com.bank.ejb.FinancialAccountService;
import com.bank.entity.FinancialAccount;
import com.bank.entity.AccountType;
import com.bank.exception.BankingException;
import jakarta.ejb.EJB;
import jakarta.enterprise.context.RequestScoped;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.*;
import java.math.BigDecimal;
import java.net.URI;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * REST Controller for Account Management
 * Provides endpoints for account operations
 */
@Path("/accounts")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@RequestScoped
public class AccountController {
    
    private static final Logger logger = Logger.getLogger(AccountController.class.getName());
    
    @EJB
    private FinancialAccountService financialAccountService;
    
    @Context
    private UriInfo uriInfo;
    
    /**
     * Creates a new account
     * POST /api/accounts
     */
    @POST
    public Response createAccount(@Valid @NotNull AccountRequest accountRequest) {
        try {
            logger.info("Creating account for customer: " + accountRequest.getCustomerId());
            
            // Validate required fields
            if (accountRequest.getCustomerId() == null || accountRequest.getCustomerId().trim().isEmpty()) {
                return Response.status(Response.Status.BAD_REQUEST)
                    .entity(new ErrorResponse("INVALID_INPUT", "Customer ID is required"))
                    .build();
            }
            
            if (accountRequest.getAccountType() == null || accountRequest.getAccountType().trim().isEmpty()) {
                return Response.status(Response.Status.BAD_REQUEST)
                    .entity(new ErrorResponse("INVALID_INPUT", "Account type is required"))
                    .build();
            }
            
            // Parse account type
            AccountType accountType;
            try {
                accountType = AccountType.valueOf(accountRequest.getAccountType().toUpperCase());
            } catch (IllegalArgumentException e) {
                return Response.status(Response.Status.BAD_REQUEST)
                    .entity(new ErrorResponse("INVALID_INPUT", "Invalid account type: " + accountRequest.getAccountType()))
                    .build();
            }
            
            // Parse initial deposit
            BigDecimal initialDeposit = null;
            if (accountRequest.getInitialDeposit() != null && accountRequest.getInitialDeposit() > 0) {
                initialDeposit = new BigDecimal(accountRequest.getInitialDeposit().toString());
            }
            
            // Create account
            FinancialAccount createdAccount = financialAccountService.establishNewAccount(
                accountRequest.getCustomerId().trim(),
                accountType,
                initialDeposit
            );
            
            URI location = uriInfo.getAbsolutePathBuilder()
                .path(createdAccount.getAccountNumber())
                .build();
            
            logger.info("Account created successfully: " + createdAccount.getAccountNumber());
            
            return Response.status(Response.Status.CREATED)
                .location(location)
                .entity(createdAccount)
                .build();
                
        } catch (BankingException e) {
            logger.log(Level.WARNING, "Business error creating account: " + e.getMessage(), e);
            return Response.status(Response.Status.BAD_REQUEST)
                .entity(new ErrorResponse("BUSINESS_ERROR", e.getUserMessage()))
                .build();
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Unexpected error creating account: " + e.getMessage(), e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity(new ErrorResponse("INTERNAL_ERROR", "An unexpected error occurred while creating account"))
                .build();
        }
    }
    
    /**
     * Gets all active accounts
     * GET /api/accounts
     */
    @GET
    public Response getAllAccounts() {
        try {
            List<FinancialAccount> accounts = financialAccountService.retrieveActiveAccounts();
            return Response.ok(accounts).build();
        } catch (BankingException e) {
            logger.log(Level.WARNING, "Error retrieving accounts: " + e.getMessage(), e);
            return Response.status(Response.Status.BAD_REQUEST)
                .entity(new ErrorResponse("RETRIEVAL_FAILED", e.getUserMessage()))
                .build();
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Unexpected error retrieving accounts: " + e.getMessage(), e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity(new ErrorResponse("INTERNAL_ERROR", "An unexpected error occurred"))
                .build();
        }
    }
    
    /**
     * Gets account by account number
     * GET /api/accounts/{accountNumber}
     */
    @GET
    @Path("/{accountNumber}")
    public Response getAccount(@PathParam("accountNumber") String accountNumber) {
        try {
            FinancialAccount account = financialAccountService.retrieveAccountByNumber(accountNumber);
            if (account == null) {
                return Response.status(Response.Status.NOT_FOUND)
                    .entity(new ErrorResponse("NOT_FOUND", "Account not found: " + accountNumber))
                    .build();
            }
            return Response.ok(account).build();
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Unexpected error retrieving account: " + e.getMessage(), e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity(new ErrorResponse("INTERNAL_ERROR", "An unexpected error occurred"))
                .build();
        }
    }
    
    /**
     * Gets accounts by customer ID
     * GET /api/accounts/customer/{customerId}
     */
    @GET
    @Path("/customer/{customerId}")
    public Response getAccountsByCustomer(@PathParam("customerId") String customerId) {
        try {
            List<FinancialAccount> accounts = financialAccountService.retrieveCustomerAccounts(customerId);
            return Response.ok(accounts).build();
        } catch (BankingException e) {
            logger.log(Level.WARNING, "Error retrieving customer accounts: " + e.getMessage(), e);
            return Response.status(Response.Status.BAD_REQUEST)
                .entity(new ErrorResponse("RETRIEVAL_FAILED", e.getUserMessage()))
                .build();
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Unexpected error retrieving customer accounts: " + e.getMessage(), e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity(new ErrorResponse("INTERNAL_ERROR", "An unexpected error occurred"))
                .build();
        }
    }
    
    /**
     * Freezes an account
     * POST /api/accounts/{accountNumber}/freeze
     */
    @POST
    @Path("/{accountNumber}/freeze")
    public Response freezeAccount(@PathParam("accountNumber") String accountNumber) {
        try {
            financialAccountService.suspendAccount(accountNumber);
            return Response.ok(new SuccessResponse("Account frozen successfully")).build();
        } catch (BankingException e) {
            logger.log(Level.WARNING, "Error freezing account: " + e.getMessage(), e);
            return Response.status(Response.Status.BAD_REQUEST)
                .entity(new ErrorResponse("FREEZE_FAILED", e.getUserMessage()))
                .build();
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Unexpected error freezing account: " + e.getMessage(), e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity(new ErrorResponse("INTERNAL_ERROR", "An unexpected error occurred"))
                .build();
        }
    }
    
    /**
     * Closes an account
     * POST /api/accounts/{accountNumber}/close
     */
    @POST
    @Path("/{accountNumber}/close")
    public Response closeAccount(@PathParam("accountNumber") String accountNumber) {
        try {
            financialAccountService.terminateAccount(accountNumber);
            return Response.ok(new SuccessResponse("Account closed successfully")).build();
        } catch (BankingException e) {
            logger.log(Level.WARNING, "Error closing account: " + e.getMessage(), e);
            return Response.status(Response.Status.BAD_REQUEST)
                .entity(new ErrorResponse("CLOSE_FAILED", e.getUserMessage()))
                .build();
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Unexpected error closing account: " + e.getMessage(), e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity(new ErrorResponse("INTERNAL_ERROR", "An unexpected error occurred"))
                .build();
        }
    }
    
    // ================ DTO Classes ================
    
    /**
     * Request DTO for account creation
     */
    public static class AccountRequest {
        private String customerId;
        private String accountType;
        private Double initialDeposit;
        
        // Default constructor
        public AccountRequest() {}
        
        // Getters and setters
        public String getCustomerId() { return customerId; }
        public void setCustomerId(String customerId) { this.customerId = customerId; }
        
        public String getAccountType() { return accountType; }
        public void setAccountType(String accountType) { this.accountType = accountType; }
        
        public Double getInitialDeposit() { return initialDeposit; }
        public void setInitialDeposit(Double initialDeposit) { this.initialDeposit = initialDeposit; }
    }
    
    /**
     * Generic success response
     */
    public static class SuccessResponse {
        private String message;
        
        public SuccessResponse(String message) {
            this.message = message;
        }
        
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
    }
    
    /**
     * Generic error response
     */
    public static class ErrorResponse {
        private String error;
        private String message;
        
        public ErrorResponse(String error, String message) {
            this.error = error;
            this.message = message;
        }
        
        public String getError() { return error; }
        public void setError(String error) { this.error = error; }
        
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
    }
} 