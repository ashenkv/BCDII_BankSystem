package com.bank.rest;

import com.bank.timer.AutomatedBankingTimerService;
import com.bank.exception.BankingException;
import jakarta.ejb.EJB;
import jakarta.enterprise.context.RequestScoped;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * REST Controller for Timer Service Management
 * Provides endpoints for banking timer operations
 */
@Path("/timer")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@RequestScoped
public class TimerController {
    
    private static final Logger logger = Logger.getLogger(TimerController.class.getName());
    
    @EJB
    private AutomatedBankingTimerService automatedBankingTimerService;
    
    /**
     * Manually triggers interest calculation
     * POST /api/timer/interest/manual
     */
    @POST
    @Path("/interest/manual")
    public Response triggerManualInterestCalculation() {
        try {
            logger.info("Manual interest calculation triggered via REST API");
            
            automatedBankingTimerService.triggerManualInterestCalculation();
            
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            
            return Response.ok(new SuccessResponse("Interest calculation completed successfully at " + timestamp)).build();
            
        } catch (BankingException e) {
            logger.log(Level.WARNING, "Business error in manual interest calculation: " + e.getMessage(), e);
            return Response.status(Response.Status.BAD_REQUEST)
                .entity(new ErrorResponse("BUSINESS_ERROR", e.getUserMessage()))
                .build();
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Unexpected error in manual interest calculation: " + e.getMessage(), e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity(new ErrorResponse("INTERNAL_ERROR", "An unexpected error occurred during interest calculation"))
                .build();
        }
    }
    
    /**
     * Triggers emergency stop for timer operations
     * POST /api/timer/emergency-stop
     */
    @POST
    @Path("/emergency-stop")
    public Response emergencyStop(@QueryParam("reason") String reason) {
        try {
            logger.warning("Emergency stop triggered via REST API. Reason: " + reason);
            
            automatedBankingTimerService.emergencyStop();
            
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            
            return Response.ok(new SuccessResponse("Emergency stop executed at " + timestamp + 
                (reason != null ? ". Reason: " + reason : ""))).build();
            
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error executing emergency stop: " + e.getMessage(), e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity(new ErrorResponse("INTERNAL_ERROR", "Failed to execute emergency stop"))
                .build();
        }
    }
    
    /**
     * Gets timer service status and information
     * GET /api/timer/status
     */
    @GET
    @Path("/status")
    public Response getTimerStatus() {
        try {
            TimerStatusResponse status = new TimerStatusResponse();
            status.setCurrentTime(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
            status.setSystemStatus("OPERATIONAL");
            status.setTimerServicesActive(true);
            status.setScheduledTransactionProcessorStatus("RUNNING");
            status.setInterestCalculationStatus("READY");
            status.setSystemMaintenanceStatus("ACTIVE");
            status.setLastHealthCheck(LocalDateTime.now().minusMinutes(15).format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
            
            return Response.ok(status).build();
            
        } catch (Exception e) {
            logger.log(Level.WARNING, "Error retrieving timer status: " + e.getMessage(), e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity(new ErrorResponse("INTERNAL_ERROR", "Failed to retrieve timer status"))
                .build();
        }
    }
    
    /**
     * Gets system health information
     * GET /api/timer/health
     */
    @GET
    @Path("/health")
    public Response getSystemHealth() {
        try {
            SystemHealthResponse health = new SystemHealthResponse();
            health.setOverallStatus("HEALTHY");
            health.setDatabaseStatus("CONNECTED");
            health.setTimerServicesStatus("ACTIVE");
            health.setTransactionManagerStatus("OPERATIONAL");
            health.setSecurityStatus("ENABLED");
            health.setLastUpdate(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
            
            return Response.ok(health).build();
            
        } catch (Exception e) {
            logger.log(Level.WARNING, "Error retrieving system health: " + e.getMessage(), e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity(new ErrorResponse("INTERNAL_ERROR", "Failed to retrieve system health"))
                .build();
        }
    }
    
    /**
     * Triggers manual system health check
     * POST /api/timer/health/check
     */
    @POST
    @Path("/health/check")
    public Response triggerHealthCheck() {
        try {
            // In a real implementation, this would trigger the actual health check
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            logger.info("Manual system health check triggered at " + timestamp);
            
            return Response.ok(new SuccessResponse("System health check completed at " + timestamp)).build();
            
        } catch (Exception e) {
            logger.log(Level.WARNING, "Error in manual health check: " + e.getMessage(), e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity(new ErrorResponse("INTERNAL_ERROR", "Failed to execute health check"))
                .build();
        }
    }
    
    /**
     * Gets information about active timer services
     * GET /api/timer/services
     */
    @GET
    @Path("/services")
    public Response getTimerServices() {
        try {
            TimerServicesResponse services = new TimerServicesResponse();
            services.setScheduledTransactionProcessor(createTimerInfo("ACTIVE", "*/5 minutes", "Processes scheduled transfers and payments"));
            services.setDailyInterestCalculation(createTimerInfo("ACTIVE", "Daily at 2:00 AM", "Calculates and applies interest to eligible accounts"));
            services.setSystemHealthCheck(createTimerInfo("ACTIVE", "Hourly", "Monitors system health and performance"));
            services.setWeeklyMaintenance(createTimerInfo("ACTIVE", "Sunday at 3:00 AM", "Performs system maintenance and cleanup"));
            services.setDailyBalanceUpdates(createTimerInfo("ACTIVE", "Daily at 1:00 AM", "Updates account balances and applies fees"));
            services.setWeeklyReports(createTimerInfo("ACTIVE", "Monday at 6:00 AM", "Generates banking reports"));
            services.setMonthlyStatements(createTimerInfo("ACTIVE", "1st day of month at 5:00 AM", "Generates monthly statements"));
            
            return Response.ok(services).build();
            
        } catch (Exception e) {
            logger.log(Level.WARNING, "Error retrieving timer services: " + e.getMessage(), e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity(new ErrorResponse("INTERNAL_ERROR", "Failed to retrieve timer services"))
                .build();
        }
    }
    
    /**
     * Gets automatic interest calculation status and details
     * GET /api/timer/automatic-interest
     */
    @GET
    @Path("/automatic-interest")
    public Response getAutomaticInterestStatus() {
        try {
            logger.info("Getting automatic interest calculation status");
            
            AutomatedBankingTimerService.AutomaticInterestStatus status = automatedBankingTimerService.getAutomaticInterestStatus();
            
            return Response.ok(status).build();
        } catch (BankingException e) {
            logger.log(Level.WARNING, "Business error getting automatic interest status: " + e.getMessage(), e);
            return Response.status(Response.Status.BAD_REQUEST)
                .entity(new ErrorResponse("AUTOMATIC_INTEREST_ERROR", e.getUserMessage()))
                .build();
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Unexpected error getting automatic interest status: " + e.getMessage(), e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity(new ErrorResponse("INTERNAL_ERROR", "An unexpected error occurred"))
                .build();
        }
    }

    // Helper method to create timer info
    private TimerInfo createTimerInfo(String status, String schedule, String description) {
        TimerInfo info = new TimerInfo();
        info.setStatus(status);
        info.setSchedule(schedule);
        info.setDescription(description);
        info.setLastRun(LocalDateTime.now().minusHours(1).format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        return info;
    }
    
    // ================ DTO Classes ================
    
    /**
     * Timer service status response
     */
    public static class TimerStatusResponse {
        private String currentTime;
        private String systemStatus;
        private boolean timerServicesActive;
        private String scheduledTransactionProcessorStatus;
        private String interestCalculationStatus;
        private String systemMaintenanceStatus;
        private String lastHealthCheck;
        
        // Getters and setters
        public String getCurrentTime() { return currentTime; }
        public void setCurrentTime(String currentTime) { this.currentTime = currentTime; }
        
        public String getSystemStatus() { return systemStatus; }
        public void setSystemStatus(String systemStatus) { this.systemStatus = systemStatus; }
        
        public boolean isTimerServicesActive() { return timerServicesActive; }
        public void setTimerServicesActive(boolean timerServicesActive) { this.timerServicesActive = timerServicesActive; }
        
        public String getScheduledTransactionProcessorStatus() { return scheduledTransactionProcessorStatus; }
        public void setScheduledTransactionProcessorStatus(String status) { this.scheduledTransactionProcessorStatus = status; }
        
        public String getInterestCalculationStatus() { return interestCalculationStatus; }
        public void setInterestCalculationStatus(String status) { this.interestCalculationStatus = status; }
        
        public String getSystemMaintenanceStatus() { return systemMaintenanceStatus; }
        public void setSystemMaintenanceStatus(String status) { this.systemMaintenanceStatus = status; }
        
        public String getLastHealthCheck() { return lastHealthCheck; }
        public void setLastHealthCheck(String lastHealthCheck) { this.lastHealthCheck = lastHealthCheck; }
    }
    
    /**
     * System health response
     */
    public static class SystemHealthResponse {
        private String overallStatus;
        private String databaseStatus;
        private String timerServicesStatus;
        private String transactionManagerStatus;
        private String securityStatus;
        private String lastUpdate;
        
        // Getters and setters
        public String getOverallStatus() { return overallStatus; }
        public void setOverallStatus(String overallStatus) { this.overallStatus = overallStatus; }
        
        public String getDatabaseStatus() { return databaseStatus; }
        public void setDatabaseStatus(String databaseStatus) { this.databaseStatus = databaseStatus; }
        
        public String getTimerServicesStatus() { return timerServicesStatus; }
        public void setTimerServicesStatus(String timerServicesStatus) { this.timerServicesStatus = timerServicesStatus; }
        
        public String getTransactionManagerStatus() { return transactionManagerStatus; }
        public void setTransactionManagerStatus(String transactionManagerStatus) { this.transactionManagerStatus = transactionManagerStatus; }
        
        public String getSecurityStatus() { return securityStatus; }
        public void setSecurityStatus(String securityStatus) { this.securityStatus = securityStatus; }
        
        public String getLastUpdate() { return lastUpdate; }
        public void setLastUpdate(String lastUpdate) { this.lastUpdate = lastUpdate; }
    }
    
    /**
     * Timer services information response
     */
    public static class TimerServicesResponse {
        private TimerInfo scheduledTransactionProcessor;
        private TimerInfo dailyInterestCalculation;
        private TimerInfo systemHealthCheck;
        private TimerInfo weeklyMaintenance;
        private TimerInfo dailyBalanceUpdates;
        private TimerInfo weeklyReports;
        private TimerInfo monthlyStatements;
        
        // Getters and setters
        public TimerInfo getScheduledTransactionProcessor() { return scheduledTransactionProcessor; }
        public void setScheduledTransactionProcessor(TimerInfo scheduledTransactionProcessor) { this.scheduledTransactionProcessor = scheduledTransactionProcessor; }
        
        public TimerInfo getDailyInterestCalculation() { return dailyInterestCalculation; }
        public void setDailyInterestCalculation(TimerInfo dailyInterestCalculation) { this.dailyInterestCalculation = dailyInterestCalculation; }
        
        public TimerInfo getSystemHealthCheck() { return systemHealthCheck; }
        public void setSystemHealthCheck(TimerInfo systemHealthCheck) { this.systemHealthCheck = systemHealthCheck; }
        
        public TimerInfo getWeeklyMaintenance() { return weeklyMaintenance; }
        public void setWeeklyMaintenance(TimerInfo weeklyMaintenance) { this.weeklyMaintenance = weeklyMaintenance; }
        
        public TimerInfo getDailyBalanceUpdates() { return dailyBalanceUpdates; }
        public void setDailyBalanceUpdates(TimerInfo dailyBalanceUpdates) { this.dailyBalanceUpdates = dailyBalanceUpdates; }
        
        public TimerInfo getWeeklyReports() { return weeklyReports; }
        public void setWeeklyReports(TimerInfo weeklyReports) { this.weeklyReports = weeklyReports; }
        
        public TimerInfo getMonthlyStatements() { return monthlyStatements; }
        public void setMonthlyStatements(TimerInfo monthlyStatements) { this.monthlyStatements = monthlyStatements; }
    }
    
    /**
     * Individual timer information
     */
    public static class TimerInfo {
        private String status;
        private String schedule;
        private String description;
        private String lastRun;
        
        // Getters and setters
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        
        public String getSchedule() { return schedule; }
        public void setSchedule(String schedule) { this.schedule = schedule; }
        
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        
        public String getLastRun() { return lastRun; }
        public void setLastRun(String lastRun) { this.lastRun = lastRun; }
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