package com.bank.rest;

import com.bank.ejb.FinancialTransactionService;
import com.bank.entity.Transaction;
import com.bank.entity.TransactionType;
import com.bank.exception.BankingException;
import jakarta.ejb.EJB;
import jakarta.enterprise.context.RequestScoped;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.*;
import java.math.BigDecimal;
import java.net.URI;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * REST Controller for Transaction Management
 * Provides endpoints for banking transactions
 */
@Path("/transactions")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@RequestScoped
public class TransactionController {
    
    private static final Logger logger = Logger.getLogger(TransactionController.class.getName());
    
    @EJB
    private FinancialTransactionService financialTransactionService;
    
    @Context
    private UriInfo uriInfo;
    
    /**
     * Processes a money transfer between accounts
     * POST /api/transactions/transfer
     */
    @POST
    @Path("/transfer")
    public Response transfer(@Valid @NotNull TransferRequest transferRequest) {
        try {
            logger.info("Processing transfer request");
            
            // Validate required fields
            if (transferRequest.getSourceAccountNumber() == null || transferRequest.getSourceAccountNumber().trim().isEmpty()) {
                return Response.status(Response.Status.BAD_REQUEST)
                    .entity(new ErrorResponse("INVALID_INPUT", "Source account number is required"))
                    .build();
            }
            
            if (transferRequest.getTargetAccountNumber() == null || transferRequest.getTargetAccountNumber().trim().isEmpty()) {
                return Response.status(Response.Status.BAD_REQUEST)
                    .entity(new ErrorResponse("INVALID_INPUT", "Target account number is required"))
                    .build();
            }
            
            if (transferRequest.getAmount() == null || transferRequest.getAmount() <= 0) {
                return Response.status(Response.Status.BAD_REQUEST)
                    .entity(new ErrorResponse("INVALID_INPUT", "Valid amount is required"))
                    .build();
            }
            
            BigDecimal amount = new BigDecimal(transferRequest.getAmount().toString());
            
            // Process transfer
            Transaction transaction = financialTransactionService.executeFundTransfer(
                transferRequest.getSourceAccountNumber().trim(),
                transferRequest.getTargetAccountNumber().trim(),
                amount,
                transferRequest.getDescription()
            );
            
            URI location = uriInfo.getAbsolutePathBuilder()
                .path("../" + transaction.getTransactionId())
                .build();
            
            logger.info("Transfer completed successfully: " + transaction.getTransactionId());
            
            return Response.status(Response.Status.CREATED)
                .location(location)
                .entity(convertToDTO(transaction))
                .build();
                
        } catch (BankingException e) {
            logger.log(Level.WARNING, "Business error processing transfer: " + e.getMessage(), e);
            return Response.status(Response.Status.BAD_REQUEST)
                .entity(new ErrorResponse("BUSINESS_ERROR", e.getUserMessage()))
                .build();
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Unexpected error processing transfer: " + e.getMessage(), e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity(new ErrorResponse("INTERNAL_ERROR", "An unexpected error occurred while processing transfer"))
                .build();
        }
    }
    
    /**
     * Processes a deposit transaction
     * POST /api/transactions/deposit
     */
    @POST
    @Path("/deposit")
    public Response deposit(@Valid @NotNull DepositRequest depositRequest) {
        try {
            logger.info("Processing deposit request");
            
            // Validate required fields
            if (depositRequest.getAccountNumber() == null || depositRequest.getAccountNumber().trim().isEmpty()) {
                return Response.status(Response.Status.BAD_REQUEST)
                    .entity(new ErrorResponse("INVALID_INPUT", "Account number is required"))
                    .build();
            }
            
            if (depositRequest.getAmount() == null || depositRequest.getAmount() <= 0) {
                return Response.status(Response.Status.BAD_REQUEST)
                    .entity(new ErrorResponse("INVALID_INPUT", "Valid amount is required"))
                    .build();
            }
            
            BigDecimal amount = new BigDecimal(depositRequest.getAmount().toString());
            
            // Process deposit
            Transaction transaction = financialTransactionService.processDepositTransaction(
                depositRequest.getAccountNumber().trim(),
                amount,
                depositRequest.getDescription()
            );
            
            logger.info("Deposit completed successfully: " + transaction.getTransactionId());
            
            return Response.status(Response.Status.CREATED)
                .entity(convertToDTO(transaction))
                .build();
                
        } catch (BankingException e) {
            logger.log(Level.WARNING, "Business error processing deposit: " + e.getMessage(), e);
            return Response.status(Response.Status.BAD_REQUEST)
                .entity(new ErrorResponse("BUSINESS_ERROR", e.getUserMessage()))
                .build();
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Unexpected error processing deposit: " + e.getMessage(), e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity(new ErrorResponse("INTERNAL_ERROR", "An unexpected error occurred while processing deposit"))
                .build();
        }
    }
    
    /**
     * Processes a withdrawal transaction
     * POST /api/transactions/withdraw
     */
    @POST
    @Path("/withdraw")
    public Response withdraw(@Valid @NotNull WithdrawRequest withdrawRequest) {
        try {
            logger.info("Processing withdrawal request");
            
            // Validate required fields
            if (withdrawRequest.getAccountNumber() == null || withdrawRequest.getAccountNumber().trim().isEmpty()) {
                return Response.status(Response.Status.BAD_REQUEST)
                    .entity(new ErrorResponse("INVALID_INPUT", "Account number is required"))
                    .build();
            }
            
            if (withdrawRequest.getAmount() == null || withdrawRequest.getAmount() <= 0) {
                return Response.status(Response.Status.BAD_REQUEST)
                    .entity(new ErrorResponse("INVALID_INPUT", "Valid amount is required"))
                    .build();
            }
            
            BigDecimal amount = new BigDecimal(withdrawRequest.getAmount().toString());
            
            // Process withdrawal
            Transaction transaction = financialTransactionService.processWithdrawalTransaction(
                withdrawRequest.getAccountNumber().trim(),
                amount,
                withdrawRequest.getDescription()
            );
            
            logger.info("Withdrawal completed successfully: " + transaction.getTransactionId());
            
            return Response.status(Response.Status.CREATED)
                .entity(convertToDTO(transaction))
                .build();
                
        } catch (BankingException e) {
            logger.log(Level.WARNING, "Business error processing withdrawal: " + e.getMessage(), e);
            return Response.status(Response.Status.BAD_REQUEST)
                .entity(new ErrorResponse("BUSINESS_ERROR", e.getUserMessage()))
                .build();
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Unexpected error processing withdrawal: " + e.getMessage(), e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity(new ErrorResponse("INTERNAL_ERROR", "An unexpected error occurred while processing withdrawal"))
                .build();
        }
    }
    
    /**
     * Schedules a future transaction
     * POST /api/transactions/schedule
     */
    @POST
    @Path("/schedule")
    public Response scheduleTransaction(@Valid @NotNull ScheduleRequest scheduleRequest) {
        try {
            logger.info("Processing schedule transaction request");
            
            // Validate required fields
            if (scheduleRequest.getSourceAccountNumber() == null || scheduleRequest.getSourceAccountNumber().trim().isEmpty()) {
                return Response.status(Response.Status.BAD_REQUEST)
                    .entity(new ErrorResponse("INVALID_INPUT", "Source account number is required"))
                    .build();
            }
            
            if (scheduleRequest.getAmount() == null || scheduleRequest.getAmount() <= 0) {
                return Response.status(Response.Status.BAD_REQUEST)
                    .entity(new ErrorResponse("INVALID_INPUT", "Valid amount is required"))
                    .build();
            }
            
            if (scheduleRequest.getScheduledDate() == null || scheduleRequest.getScheduledDate().trim().isEmpty()) {
                return Response.status(Response.Status.BAD_REQUEST)
                    .entity(new ErrorResponse("INVALID_INPUT", "Scheduled date is required"))
                    .build();
            }
            
            // Parse transaction type
            TransactionType transactionType;
            try {
                transactionType = TransactionType.valueOf(scheduleRequest.getTransactionType().toUpperCase());
            } catch (Exception e) {
                return Response.status(Response.Status.BAD_REQUEST)
                    .entity(new ErrorResponse("INVALID_INPUT", "Invalid transaction type: " + scheduleRequest.getTransactionType()))
                    .build();
            }
            
            // Parse scheduled date
            LocalDateTime scheduledDate;
            try {
                scheduledDate = LocalDateTime.parse(scheduleRequest.getScheduledDate(), DateTimeFormatter.ISO_LOCAL_DATE_TIME);
            } catch (Exception e) {
                return Response.status(Response.Status.BAD_REQUEST)
                    .entity(new ErrorResponse("INVALID_INPUT", "Invalid date format. Use ISO format: YYYY-MM-DDTHH:MM:SS"))
                    .build();
            }
            
            BigDecimal amount = new BigDecimal(scheduleRequest.getAmount().toString());
            
            // Schedule transaction
            Transaction transaction = financialTransactionService.scheduleFutureTransaction(
                scheduleRequest.getSourceAccountNumber().trim(),
                scheduleRequest.getTargetAccountNumber(),
                amount,
                transactionType,
                scheduledDate,
                scheduleRequest.getDescription()
            );
            
            logger.info("Transaction scheduled successfully: " + transaction.getTransactionId());
            
            return Response.status(Response.Status.CREATED)
                .entity(convertToDTO(transaction))
                .build();
                
        } catch (BankingException e) {
            logger.log(Level.WARNING, "Business error scheduling transaction: " + e.getMessage(), e);
            return Response.status(Response.Status.BAD_REQUEST)
                .entity(new ErrorResponse("BUSINESS_ERROR", e.getUserMessage()))
                .build();
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Unexpected error scheduling transaction: " + e.getMessage(), e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity(new ErrorResponse("INTERNAL_ERROR", "An unexpected error occurred while scheduling transaction"))
                .build();
        }
    }
    
    /**
     * Gets transaction by ID
     * GET /api/transactions/{transactionId}
     */
    @GET
    @Path("/{transactionId}")
    public Response getTransaction(@PathParam("transactionId") String transactionId) {
        try {
            Transaction transaction = financialTransactionService.retrieveTransactionById(transactionId);
            if (transaction == null) {
                return Response.status(Response.Status.NOT_FOUND)
                    .entity(new ErrorResponse("NOT_FOUND", "Transaction not found: " + transactionId))
                    .build();
            }
            return Response.ok(convertToDTO(transaction)).build();
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Unexpected error retrieving transaction: " + e.getMessage(), e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity(new ErrorResponse("INTERNAL_ERROR", "An unexpected error occurred"))
                .build();
        }
    }
    
    /**
     * Gets all transaction history (for dashboard view)
     * GET /api/transactions/history
     */
    @GET
    @Path("/history")
    public Response getAllTransactionHistory(@QueryParam("limit") @DefaultValue("100") int limit) {
        try {
            List<Transaction> transactions = financialTransactionService.retrieveAllTransactionHistory(limit);
            List<TransactionDTO> transactionDTOs = transactions.stream()
                .map(this::convertToDTO)
                .collect(java.util.stream.Collectors.toList());
            return Response.ok(transactionDTOs).build();
        } catch (BankingException e) {
            logger.log(Level.WARNING, "Error retrieving all transaction history: " + e.getMessage(), e);
            return Response.status(Response.Status.BAD_REQUEST)
                .entity(new ErrorResponse("RETRIEVAL_FAILED", e.getUserMessage()))
                .build();
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Unexpected error retrieving all transaction history: " + e.getMessage(), e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity(new ErrorResponse("INTERNAL_ERROR", "An unexpected error occurred"))
                .build();
        }
    }

    /**
     * Gets transaction history for a specific account
     * GET /api/transactions/history/{accountNumber}
     */
    @GET
    @Path("/history/{accountNumber}")
    public Response getTransactionHistory(@PathParam("accountNumber") String accountNumber,
                                        @QueryParam("limit") @DefaultValue("50") int limit) {
        try {
            List<Transaction> transactions = financialTransactionService.retrieveAccountTransactionHistory(accountNumber, limit);
            List<TransactionDTO> transactionDTOs = transactions.stream()
                .map(this::convertToDTO)
                .collect(java.util.stream.Collectors.toList());
            return Response.ok(transactionDTOs).build();
        } catch (BankingException e) {
            logger.log(Level.WARNING, "Error retrieving transaction history: " + e.getMessage(), e);
            return Response.status(Response.Status.BAD_REQUEST)
                .entity(new ErrorResponse("RETRIEVAL_FAILED", e.getUserMessage()))
                .build();
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Unexpected error retrieving transaction history: " + e.getMessage(), e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity(new ErrorResponse("INTERNAL_ERROR", "An unexpected error occurred"))
                .build();
        }
    }
    
    /**
     * Gets weekly transaction report
     * GET /api/transactions/reports/weekly
     */
    @GET
    @Path("/reports/weekly")
    public Response getWeeklyReport() {
        try {
            FinancialTransactionService.WeeklyReportDTO report = financialTransactionService.generateWeeklyReport();
            return Response.ok(report).build();
        } catch (BankingException e) {
            logger.log(Level.WARNING, "Error generating weekly report: " + e.getMessage(), e);
            return Response.status(Response.Status.BAD_REQUEST)
                .entity(new ErrorResponse("REPORT_FAILED", e.getUserMessage()))
                .build();
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Unexpected error generating weekly report: " + e.getMessage(), e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity(new ErrorResponse("INTERNAL_ERROR", "An unexpected error occurred"))
                .build();
        }
    }
    
    /**
     * Gets monthly transaction report
     * GET /api/transactions/reports/monthly
     */
    @GET
    @Path("/reports/monthly")
    public Response getMonthlyReport() {
        try {
            FinancialTransactionService.MonthlyReportDTO report = financialTransactionService.generateMonthlyReport();
            return Response.ok(report).build();
        } catch (BankingException e) {
            logger.log(Level.WARNING, "Error generating monthly report: " + e.getMessage(), e);
            return Response.status(Response.Status.BAD_REQUEST)
                .entity(new ErrorResponse("REPORT_FAILED", e.getUserMessage()))
                .build();
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Unexpected error generating monthly report: " + e.getMessage(), e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity(new ErrorResponse("INTERNAL_ERROR", "An unexpected error occurred"))
                .build();
        }
    }
    
    /**
     * Gets pending transactions
     * GET /api/transactions/pending
     */
    @GET
    @Path("/pending")
    public Response getPendingTransactions() {
        try {
            List<Transaction> transactions = financialTransactionService.retrievePendingTransactions();
            List<TransactionDTO> transactionDTOs = transactions.stream()
                .map(this::convertToDTO)
                .collect(java.util.stream.Collectors.toList());
            return Response.ok(transactionDTOs).build();
        } catch (BankingException e) {
            logger.log(Level.WARNING, "Error retrieving pending transactions: " + e.getMessage(), e);
            return Response.status(Response.Status.BAD_REQUEST)
                .entity(new ErrorResponse("RETRIEVAL_FAILED", e.getUserMessage()))
                .build();
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Unexpected error retrieving pending transactions: " + e.getMessage(), e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity(new ErrorResponse("INTERNAL_ERROR", "An unexpected error occurred"))
                .build();
        }
    }
    
    /**
     * Gets scheduled transactions
     * GET /api/transactions/scheduled
     */
    @GET
    @Path("/scheduled")
    public Response getScheduledTransactions() {
        try {
            List<Transaction> transactions = financialTransactionService.retrieveAllScheduledTransactions();
            List<TransactionDTO> transactionDTOs = transactions.stream()
                .map(this::convertToDTO)
                .collect(java.util.stream.Collectors.toList());
            return Response.ok(transactionDTOs).build();
        } catch (BankingException e) {
            logger.log(Level.WARNING, "Error retrieving scheduled transactions: " + e.getMessage(), e);
            return Response.status(Response.Status.BAD_REQUEST)
                .entity(new ErrorResponse("RETRIEVAL_FAILED", e.getUserMessage()))
                .build();
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Unexpected error retrieving scheduled transactions: " + e.getMessage(), e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity(new ErrorResponse("INTERNAL_ERROR", "An unexpected error occurred"))
                .build();
        }
    }
    
    /**
     * Gets scheduled transactions that are due for processing now
     * GET /api/transactions/scheduled/due
     */
    @GET
    @Path("/scheduled/due")
    public Response getScheduledTransactionsDue() {
        try {
            List<Transaction> transactions = financialTransactionService.retrieveScheduledTransactionsDue();
            List<TransactionDTO> transactionDTOs = transactions.stream()
                .map(this::convertToDTO)
                .collect(java.util.stream.Collectors.toList());
            return Response.ok(transactionDTOs).build();
        } catch (BankingException e) {
            logger.log(Level.WARNING, "Error retrieving due scheduled transactions: " + e.getMessage(), e);
            return Response.status(Response.Status.BAD_REQUEST)
                .entity(new ErrorResponse("RETRIEVAL_FAILED", e.getUserMessage()))
                .build();
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Unexpected error retrieving due scheduled transactions: " + e.getMessage(), e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity(new ErrorResponse("INTERNAL_ERROR", "An unexpected error occurred"))
                .build();
        }
    }
    
    /**
     * Converts Transaction entity to DTO to avoid circular references
     */
    private TransactionDTO convertToDTO(Transaction transaction) {
        TransactionDTO dto = new TransactionDTO();
        dto.setTransactionId(transaction.getTransactionId());
        dto.setTransactionType(transaction.getTransactionType().toString());
        dto.setAmount(transaction.getAmount());
        dto.setCurrency(transaction.getCurrency());
        dto.setDescription(transaction.getDescription());
        dto.setStatus(transaction.getStatus().toString());
        dto.setTransactionDate(transaction.getTransactionDate());
        dto.setScheduledDate(transaction.getScheduledDate());
        
        // Include account numbers instead of full account objects
        if (transaction.getSourceAccount() != null) {
            dto.setSourceAccountNumber(transaction.getSourceAccount().getAccountNumber());
        }
        if (transaction.getTargetAccount() != null) {
            dto.setTargetAccountNumber(transaction.getTargetAccount().getAccountNumber());
        }
        
        return dto;
    }
    
    // ================ DTO Classes ================
    
    /**
     * Request DTO for money transfer
     */
    public static class TransferRequest {
        private String sourceAccountNumber;
        private String targetAccountNumber;
        private Double amount;
        private String description;
        
        // Default constructor
        public TransferRequest() {}
        
        // Getters and setters
        public String getSourceAccountNumber() { return sourceAccountNumber; }
        public void setSourceAccountNumber(String sourceAccountNumber) { this.sourceAccountNumber = sourceAccountNumber; }
        
        public String getTargetAccountNumber() { return targetAccountNumber; }
        public void setTargetAccountNumber(String targetAccountNumber) { this.targetAccountNumber = targetAccountNumber; }
        
        public Double getAmount() { return amount; }
        public void setAmount(Double amount) { this.amount = amount; }
        
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
    }
    
    /**
     * Request DTO for deposit
     */
    public static class DepositRequest {
        private String accountNumber;
        private Double amount;
        private String description;
        
        // Default constructor
        public DepositRequest() {}
        
        // Getters and setters
        public String getAccountNumber() { return accountNumber; }
        public void setAccountNumber(String accountNumber) { this.accountNumber = accountNumber; }
        
        public Double getAmount() { return amount; }
        public void setAmount(Double amount) { this.amount = amount; }
        
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
    }
    
    /**
     * Request DTO for withdrawal
     */
    public static class WithdrawRequest {
        private String accountNumber;
        private Double amount;
        private String description;
        
        // Default constructor
        public WithdrawRequest() {}
        
        // Getters and setters
        public String getAccountNumber() { return accountNumber; }
        public void setAccountNumber(String accountNumber) { this.accountNumber = accountNumber; }
        
        public Double getAmount() { return amount; }
        public void setAmount(Double amount) { this.amount = amount; }
        
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
    }
    
    /**
     * Request DTO for scheduling transactions
     */
    public static class ScheduleRequest {
        private String sourceAccountNumber;
        private String targetAccountNumber;
        private String transactionType;
        private Double amount;
        private String scheduledDate;
        private String description;
        
        // Default constructor
        public ScheduleRequest() {}
        
        // Getters and setters
        public String getSourceAccountNumber() { return sourceAccountNumber; }
        public void setSourceAccountNumber(String sourceAccountNumber) { this.sourceAccountNumber = sourceAccountNumber; }
        
        public String getTargetAccountNumber() { return targetAccountNumber; }
        public void setTargetAccountNumber(String targetAccountNumber) { this.targetAccountNumber = targetAccountNumber; }
        
        public String getTransactionType() { return transactionType; }
        public void setTransactionType(String transactionType) { this.transactionType = transactionType; }
        
        public Double getAmount() { return amount; }
        public void setAmount(Double amount) { this.amount = amount; }
        
        public String getScheduledDate() { return scheduledDate; }
        public void setScheduledDate(String scheduledDate) { this.scheduledDate = scheduledDate; }
        
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
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
    
    /**
     * Transaction DTO to avoid circular references
     */
    public static class TransactionDTO {
        private String transactionId;
        private String transactionType;
        private BigDecimal amount;
        private String currency;
        private String description;
        private String status;
        private LocalDateTime transactionDate;
        private LocalDateTime scheduledDate;
        private String sourceAccountNumber;
        private String targetAccountNumber;
        
        // Default constructor
        public TransactionDTO() {}
        
        // Getters and setters
        public String getTransactionId() { return transactionId; }
        public void setTransactionId(String transactionId) { this.transactionId = transactionId; }
        
        public String getTransactionType() { return transactionType; }
        public void setTransactionType(String transactionType) { this.transactionType = transactionType; }
        
        public BigDecimal getAmount() { return amount; }
        public void setAmount(BigDecimal amount) { this.amount = amount; }
        
        public String getCurrency() { return currency; }
        public void setCurrency(String currency) { this.currency = currency; }
        
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        
        public LocalDateTime getTransactionDate() { return transactionDate; }
        public void setTransactionDate(LocalDateTime transactionDate) { this.transactionDate = transactionDate; }
        
        public LocalDateTime getScheduledDate() { return scheduledDate; }
        public void setScheduledDate(LocalDateTime scheduledDate) { this.scheduledDate = scheduledDate; }
        
        public String getSourceAccountNumber() { return sourceAccountNumber; }
        public void setSourceAccountNumber(String sourceAccountNumber) { this.sourceAccountNumber = sourceAccountNumber; }
        
        public String getTargetAccountNumber() { return targetAccountNumber; }
        public void setTargetAccountNumber(String targetAccountNumber) { this.targetAccountNumber = targetAccountNumber; }
    }
    
    /**
     * Weekly Report DTO
     */
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
        
        // Getters and setters
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
    
    /**
     * Monthly Report DTO
     */
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
        
        // Getters and setters
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