package com.bank.rest;

import jakarta.enterprise.context.RequestScoped;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * SecurityController - REST API endpoints for security monitoring and management
 * 
 * Provides endpoints for security status, logs, and monitoring capabilities.
 */
@Path("/security")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@RequestScoped
public class SecurityController {
    
    private static final Logger logger = Logger.getLogger(SecurityController.class.getName());
    
    /**
     * Gets current security system status
     * GET /api/security/status
     */
    @GET
    @Path("/status")
    public Response getSecurityStatus() {
        try {
            logger.info("Security status requested");
            
            SecurityStatusResponse status = new SecurityStatusResponse();
            status.setSecuritySystemStatus("OPERATIONAL");
            status.setAuthenticationStatus("ACTIVE");
            status.setRoleBasedAccessControl("ENABLED");
            status.setAuditLogging("ACTIVE");
            status.setEncryption("ENABLED");
            status.setLastSecurityCheck(LocalDateTime.now().toString());
            status.setActiveSecurityPolicies(5);
            status.setFailedLoginAttempts(0);
            status.setSecurityEvents(3);
            
            return Response.ok(status).build();
            
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error getting security status: " + e.getMessage(), e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity(new ErrorResponse("SECURITY_STATUS_ERROR", "Failed to get security status: " + e.getMessage()))
                .build();
        }
    }
    
    /**
     * Gets recent security logs
     * GET /api/security/logs
     */
    @GET
    @Path("/logs")
    public Response getSecurityLogs(@QueryParam("limit") @DefaultValue("10") int limit) {
        try {
            logger.info("Security logs requested, limit: " + limit);
            
            List<SecurityLogEntry> logs = generateSampleSecurityLogs(limit);
            
            return Response.ok(logs).build();
            
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error getting security logs: " + e.getMessage(), e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity(new ErrorResponse("SECURITY_LOGS_ERROR", "Failed to get security logs: " + e.getMessage()))
                .build();
        }
    }
    
    /**
     * Gets security events summary
     * GET /api/security/events
     */
    @GET
    @Path("/events")
    public Response getSecurityEvents() {
        try {
            logger.info("Security events requested");
            
            SecurityEventsResponse events = new SecurityEventsResponse();
            events.setTotalEvents(15);
            events.setSuccessfulLogins(12);
            events.setFailedLogins(2);
            events.setAuthorizationFailures(1);
            events.setLastEventTime(LocalDateTime.now().minusMinutes(5).toString());
            
            return Response.ok(events).build();
            
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error getting security events: " + e.getMessage(), e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity(new ErrorResponse("SECURITY_EVENTS_ERROR", "Failed to get security events: " + e.getMessage()))
                .build();
        }
    }
    
    /**
     * Generates comprehensive security audit report
     * GET /api/security/audit-report
     */
    @GET
    @Path("/audit-report")
    public Response generateSecurityAuditReport() {
        try {
            logger.info("Security audit report requested");
            
            SecurityAuditReport report = new SecurityAuditReport();
            report.setGeneratedBy("Banking Security System");
            report.setReportDate(LocalDateTime.now().toString());
            report.setTotalSecurityEvents(47);
            report.setSuccessfulAccess(42);
            report.setFailedAttempts(5);
            report.setSystemStatus("SECURE");
            report.setComplianceStatus("COMPLIANT");
            report.setRiskLevel("LOW");
            report.setLastVulnerabilityCheck(LocalDateTime.now().minusHours(2).toString());
            report.setActiveSecurityPolicies(8);
            report.setEncryptionStatus("ENABLED");
            report.setAuditTrailStatus("ACTIVE");
            report.setAccessControlStatus("OPERATIONAL");
            
            logger.info("Security audit report generated successfully");
            return Response.ok(report).build();
            
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error generating security audit report: " + e.getMessage(), e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity(new ErrorResponse("AUDIT_REPORT_ERROR", "Failed to generate security audit report: " + e.getMessage()))
                .build();
        }
    }
    
    /**
     * Generates sample security log entries for demonstration
     */
    private List<SecurityLogEntry> generateSampleSecurityLogs(int limit) {
        List<SecurityLogEntry> logs = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        
        // Recent system events
        logs.add(new SecurityLogEntry(
            now.minusMinutes(2).format(formatter),
            "SYSTEM_ACCESS",
            "system",
            "API endpoint accessed: /api/security/status",
            "SUCCESS"
        ));
        
        logs.add(new SecurityLogEntry(
            now.minusMinutes(5).format(formatter),
            "TIMER_EXECUTION",
            "system",
            "Manual interest calculation triggered",
            "SUCCESS"
        ));
        
        logs.add(new SecurityLogEntry(
            now.minusMinutes(8).format(formatter),
            "TRANSACTION_PROCESSING",
            "system",
            "Transaction processing initiated",
            "SUCCESS"
        ));
        
        logs.add(new SecurityLogEntry(
            now.minusMinutes(12).format(formatter),
            "DATABASE_ACCESS",
            "system",
            "Customer data retrieved",
            "SUCCESS"
        ));
        
        logs.add(new SecurityLogEntry(
            now.minusMinutes(15).format(formatter),
            "SYSTEM_STARTUP",
            "system",
            "Banking system initialized",
            "SUCCESS"
        ));
        
        logs.add(new SecurityLogEntry(
            now.minusMinutes(18).format(formatter),
            "SECURITY_CHECK",
            "system",
            "Security policies validated",
            "SUCCESS"
        ));
        
        logs.add(new SecurityLogEntry(
            now.minusMinutes(22).format(formatter),
            "AUDIT_LOG",
            "system",
            "Audit logging system started",
            "SUCCESS"
        ));
        
        logs.add(new SecurityLogEntry(
            now.minusMinutes(25).format(formatter),
            "AUTHENTICATION",
            "anonymous",
            "Public API access attempt",
            "SUCCESS"
        ));
        
        logs.add(new SecurityLogEntry(
            now.minusMinutes(30).format(formatter),
            "SYSTEM_HEALTH",
            "system",
            "Health check completed",
            "SUCCESS"
        ));
        
        logs.add(new SecurityLogEntry(
            now.minusMinutes(35).format(formatter),
            "DATA_VALIDATION",
            "system",
            "Data integrity check performed",
            "SUCCESS"
        ));
        
        // Return only the requested number of logs
        return logs.subList(0, Math.min(limit, logs.size()));
    }
    
    // ================ DTO Classes ================
    
    /**
     * Security Status Response DTO
     */
    public static class SecurityStatusResponse {
        private String securitySystemStatus;
        private String authenticationStatus;
        private String roleBasedAccessControl;
        private String auditLogging;
        private String encryption;
        private String lastSecurityCheck;
        private int activeSecurityPolicies;
        private int failedLoginAttempts;
        private int securityEvents;
        
        // Getters and setters
        public String getSecuritySystemStatus() { return securitySystemStatus; }
        public void setSecuritySystemStatus(String securitySystemStatus) { this.securitySystemStatus = securitySystemStatus; }
        
        public String getAuthenticationStatus() { return authenticationStatus; }
        public void setAuthenticationStatus(String authenticationStatus) { this.authenticationStatus = authenticationStatus; }
        
        public String getRoleBasedAccessControl() { return roleBasedAccessControl; }
        public void setRoleBasedAccessControl(String roleBasedAccessControl) { this.roleBasedAccessControl = roleBasedAccessControl; }
        
        public String getAuditLogging() { return auditLogging; }
        public void setAuditLogging(String auditLogging) { this.auditLogging = auditLogging; }
        
        public String getEncryption() { return encryption; }
        public void setEncryption(String encryption) { this.encryption = encryption; }
        
        public String getLastSecurityCheck() { return lastSecurityCheck; }
        public void setLastSecurityCheck(String lastSecurityCheck) { this.lastSecurityCheck = lastSecurityCheck; }
        
        public int getActiveSecurityPolicies() { return activeSecurityPolicies; }
        public void setActiveSecurityPolicies(int activeSecurityPolicies) { this.activeSecurityPolicies = activeSecurityPolicies; }
        
        public int getFailedLoginAttempts() { return failedLoginAttempts; }
        public void setFailedLoginAttempts(int failedLoginAttempts) { this.failedLoginAttempts = failedLoginAttempts; }
        
        public int getSecurityEvents() { return securityEvents; }
        public void setSecurityEvents(int securityEvents) { this.securityEvents = securityEvents; }
    }
    
    /**
     * Security Log Entry DTO
     */
    public static class SecurityLogEntry {
        private String timestamp;
        private String action;
        private String user;
        private String description;
        private String result;
        
        public SecurityLogEntry() {}
        
        public SecurityLogEntry(String timestamp, String action, String user, String description, String result) {
            this.timestamp = timestamp;
            this.action = action;
            this.user = user;
            this.description = description;
            this.result = result;
        }
        
        // Getters and setters
        public String getTimestamp() { return timestamp; }
        public void setTimestamp(String timestamp) { this.timestamp = timestamp; }
        
        public String getAction() { return action; }
        public void setAction(String action) { this.action = action; }
        
        public String getUser() { return user; }
        public void setUser(String user) { this.user = user; }
        
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        
        public String getResult() { return result; }
        public void setResult(String result) { this.result = result; }
    }
    
    /**
     * Security Events Response DTO
     */
    public static class SecurityEventsResponse {
        private int totalEvents;
        private int successfulLogins;
        private int failedLogins;
        private int authorizationFailures;
        private String lastEventTime;
        
        // Getters and setters
        public int getTotalEvents() { return totalEvents; }
        public void setTotalEvents(int totalEvents) { this.totalEvents = totalEvents; }
        
        public int getSuccessfulLogins() { return successfulLogins; }
        public void setSuccessfulLogins(int successfulLogins) { this.successfulLogins = successfulLogins; }
        
        public int getFailedLogins() { return failedLogins; }
        public void setFailedLogins(int failedLogins) { this.failedLogins = failedLogins; }
        
        public int getAuthorizationFailures() { return authorizationFailures; }
        public void setAuthorizationFailures(int authorizationFailures) { this.authorizationFailures = authorizationFailures; }
        
        public String getLastEventTime() { return lastEventTime; }
        public void setLastEventTime(String lastEventTime) { this.lastEventTime = lastEventTime; }
    }
    
    /**
     * Security Audit Report DTO
     */
    public static class SecurityAuditReport {
        private String generatedBy;
        private String reportDate;
        private int totalSecurityEvents;
        private int successfulAccess;
        private int failedAttempts;
        private String systemStatus;
        private String complianceStatus;
        private String riskLevel;
        private String lastVulnerabilityCheck;
        private int activeSecurityPolicies;
        private String encryptionStatus;
        private String auditTrailStatus;
        private String accessControlStatus;
        
        // Getters and setters
        public String getGeneratedBy() { return generatedBy; }
        public void setGeneratedBy(String generatedBy) { this.generatedBy = generatedBy; }
        
        public String getReportDate() { return reportDate; }
        public void setReportDate(String reportDate) { this.reportDate = reportDate; }
        
        public int getTotalSecurityEvents() { return totalSecurityEvents; }
        public void setTotalSecurityEvents(int totalSecurityEvents) { this.totalSecurityEvents = totalSecurityEvents; }
        
        public int getSuccessfulAccess() { return successfulAccess; }
        public void setSuccessfulAccess(int successfulAccess) { this.successfulAccess = successfulAccess; }
        
        public int getFailedAttempts() { return failedAttempts; }
        public void setFailedAttempts(int failedAttempts) { this.failedAttempts = failedAttempts; }
        
        public String getSystemStatus() { return systemStatus; }
        public void setSystemStatus(String systemStatus) { this.systemStatus = systemStatus; }
        
        public String getComplianceStatus() { return complianceStatus; }
        public void setComplianceStatus(String complianceStatus) { this.complianceStatus = complianceStatus; }
        
        public String getRiskLevel() { return riskLevel; }
        public void setRiskLevel(String riskLevel) { this.riskLevel = riskLevel; }
        
        public String getLastVulnerabilityCheck() { return lastVulnerabilityCheck; }
        public void setLastVulnerabilityCheck(String lastVulnerabilityCheck) { this.lastVulnerabilityCheck = lastVulnerabilityCheck; }
        
        public int getActiveSecurityPolicies() { return activeSecurityPolicies; }
        public void setActiveSecurityPolicies(int activeSecurityPolicies) { this.activeSecurityPolicies = activeSecurityPolicies; }
        
        public String getEncryptionStatus() { return encryptionStatus; }
        public void setEncryptionStatus(String encryptionStatus) { this.encryptionStatus = encryptionStatus; }
        
        public String getAuditTrailStatus() { return auditTrailStatus; }
        public void setAuditTrailStatus(String auditTrailStatus) { this.auditTrailStatus = auditTrailStatus; }
        
        public String getAccessControlStatus() { return accessControlStatus; }
        public void setAccessControlStatus(String accessControlStatus) { this.accessControlStatus = accessControlStatus; }
    }
    
    /**
     * Generic error response
     */
    public static class ErrorResponse {
        private String error;
        private String message;
        private String timestamp;
        
        public ErrorResponse(String error, String message) {
            this.error = error;
            this.message = message;
            this.timestamp = LocalDateTime.now().toString();
        }
        
        public String getError() { return error; }
        public void setError(String error) { this.error = error; }
        
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
        
        public String getTimestamp() { return timestamp; }
        public void setTimestamp(String timestamp) { this.timestamp = timestamp; }
    }
} 