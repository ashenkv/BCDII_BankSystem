package com.bank.security;

import com.bank.entity.Customer;
import com.bank.entity.CustomerStatus;
import com.bank.exception.BankingException;

import jakarta.annotation.security.DeclareRoles;
import jakarta.annotation.security.DenyAll;
import jakarta.annotation.security.PermitAll;
import jakarta.annotation.security.RolesAllowed;
import jakarta.annotation.security.RunAs;
import jakarta.ejb.EJB;
import jakarta.ejb.SessionContext;
import jakarta.ejb.Stateless;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import jakarta.ejb.EJBAccessException;
import java.security.Principal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Banking Security Service - Comprehensive EJB Security Architecture
 * 
 * This service demonstrates:
 * - @DeclareRoles for role definition
 * - @RolesAllowed for method-level security
 * - @PermitAll for public access methods
 * - @DenyAll for restricted methods
 * - @RunAs for privilege escalation
 * - JAAS integration with SessionContext
 * - Role-based access control (RBAC)
 * - Security audit logging
 * - Authentication and authorization patterns
 * 
 * Security Architecture Components:
 * 1. Role Definition and Management
 * 2. Method-level Security Controls
 * 3. Security Context Management
 * 4. Audit and Compliance Logging
 * 5. Privilege Escalation Controls
 */
@Stateless
@DeclareRoles({"ADMIN", "MANAGER", "EMPLOYEE", "CUSTOMER", "AUDITOR", "SYSTEM", "GUEST"})
@RunAs("SYSTEM")
public class BankingSecurityService {
    
    private static final Logger logger = Logger.getLogger(BankingSecurityService.class.getName());
    
    @PersistenceContext(unitName = "bankingPU")
    private EntityManager entityManager;
    
    @jakarta.annotation.Resource
    private SessionContext sessionContext;
    
    // --- Public Access Methods (@PermitAll) ---
    
    /**
     * System health check - Available to all users including anonymous
     * Demonstrates @PermitAll annotation for public endpoints
     */
    @PermitAll
    @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
    public boolean isSystemAvailable() {
        logger.info("🔓 Public access: System availability check");
        return true;
    }
    
    /**
     * Get public banking information
     * No authentication required for general information
     */
    @PermitAll
    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public String getPublicBankingInfo() {
        logger.info("🔓 Public access: Banking information requested");
        return "Welcome to Secure Banking System - Operating hours: 24/7";
    }
    
    /**
     * Get supported security features for client applications
     */
    @PermitAll
    @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
    public List<String> getSupportedSecurityFeatures() {
        logger.info("🔓 Public access: Security features inquiry");
        return Arrays.asList(
            "Role-Based Access Control (RBAC)",
            "JAAS Authentication",
            "Method-level Security",
            "Transaction Security",
            "Audit Logging"
        );
    }
    
    // --- Customer Access Methods (@RolesAllowed("CUSTOMER")) ---
    
    /**
     * Get current user's security profile
     * Available only to authenticated customers
     */
    @RolesAllowed("CUSTOMER")
    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public SecurityProfile getCurrentUserProfile() {
        try {
            Principal caller = sessionContext.getCallerPrincipal();
            logger.info("🔒 Customer access: Profile requested by " + caller.getName());
            
            SecurityProfile profile = new SecurityProfile();
            profile.setUsername(caller.getName());
            profile.setRoles(getCurrentUserRoles());
            profile.setLastLogin(LocalDateTime.now());
            profile.setSecurityLevel("CUSTOMER");
            
            logSecurityEvent("PROFILE_ACCESS", caller.getName(), "SUCCESS");
            return profile;
            
        } catch (Exception e) {
            logger.log(Level.WARNING, "Error getting user profile: " + e.getMessage(), e);
            logSecurityEvent("PROFILE_ACCESS", "UNKNOWN", "FAILED");
            throw new EJBAccessException("Failed to retrieve user profile");
        }
    }
    
    /**
     * Change customer password
     * Demonstrates customer self-service security operations
     */
    @RolesAllowed("CUSTOMER")
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public boolean changePassword(String oldPassword, String newPassword) {
        try {
            Principal caller = sessionContext.getCallerPrincipal();
            logger.info("🔒 Customer access: Password change requested by " + caller.getName());
            
            // In real implementation, validate old password and update
            // For demo purposes, we simulate the operation
            
            if (newPassword == null || newPassword.length() < 8) {
                throw new EJBAccessException("Password does not meet security requirements");
            }
            
            logSecurityEvent("PASSWORD_CHANGE", caller.getName(), "SUCCESS");
            return true;
            
        } catch (Exception e) {
            logger.log(Level.WARNING, "Password change failed: " + e.getMessage(), e);
            logSecurityEvent("PASSWORD_CHANGE", sessionContext.getCallerPrincipal().getName(), "FAILED");
            return false;
        }
    }
    
    // --- Employee Access Methods (@RolesAllowed("EMPLOYEE")) ---
    
    /**
     * Access customer data for service purposes
     * Available to bank employees only
     */
    @RolesAllowed({"EMPLOYEE", "MANAGER", "ADMIN"})
    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public List<Customer> getCustomersForService(String searchCriteria) {
        try {
            Principal caller = sessionContext.getCallerPrincipal();
            logger.info("🔒 Employee access: Customer search by " + caller.getName());
            
            // Verify caller has proper employee role
            if (!sessionContext.isCallerInRole("EMPLOYEE")) {
                throw new EJBAccessException("Insufficient privileges for customer data access");
            }
            
            TypedQuery<Customer> query = entityManager.createQuery(
                "SELECT c FROM Customer c WHERE c.firstName LIKE :criteria OR c.lastName LIKE :criteria",
                Customer.class);
            query.setParameter("criteria", "%" + searchCriteria + "%");
            query.setMaxResults(50); // Limit for security
            
            List<Customer> customers = query.getResultList();
            
            logSecurityEvent("CUSTOMER_SEARCH", caller.getName(), 
                "SUCCESS - " + customers.size() + " records accessed");
            
            return customers;
            
        } catch (Exception e) {
            logger.log(Level.WARNING, "Customer search failed: " + e.getMessage(), e);
            logSecurityEvent("CUSTOMER_SEARCH", sessionContext.getCallerPrincipal().getName(), "FAILED");
            throw new EJBAccessException("Failed to search customers: " + e.getMessage());
        }
    }
    
    // --- Manager Access Methods (@RolesAllowed("MANAGER")) ---
    
    /**
     * Generate security audit report
     * Available to managers and above only
     */
    @RolesAllowed({"MANAGER", "ADMIN"})
    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public SecurityAuditReport generateAuditReport(LocalDateTime startDate, LocalDateTime endDate) {
        try {
            Principal caller = sessionContext.getCallerPrincipal();
            logger.info("🔒 Manager access: Audit report requested by " + caller.getName());
            
            SecurityAuditReport report = new SecurityAuditReport();
            report.setGeneratedBy(caller.getName());
            report.setGeneratedAt(LocalDateTime.now());
            report.setPeriodStart(startDate);
            report.setPeriodEnd(endDate);
            
            // In real implementation, query audit log
            report.setTotalSecurityEvents(150);
            report.setFailedAttempts(5);
            report.setSuccessfulAccess(145);
            
            logSecurityEvent("AUDIT_REPORT", caller.getName(), "SUCCESS");
            return report;
            
        } catch (Exception e) {
            logger.log(Level.WARNING, "Audit report generation failed: " + e.getMessage(), e);
            logSecurityEvent("AUDIT_REPORT", sessionContext.getCallerPrincipal().getName(), "FAILED");
            throw new EJBAccessException("Failed to generate audit report: " + e.getMessage());
        }
    }
    
    /**
     * Approve high-value transactions
     * Requires manager approval for amounts over threshold
     */
    @RolesAllowed({"MANAGER", "ADMIN"})
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public boolean approveHighValueTransaction(String transactionId, String justification) {
        try {
            Principal caller = sessionContext.getCallerPrincipal();
            logger.info("🔒 Manager access: High-value transaction approval by " + caller.getName());
            
            // Verify manager authority
            if (!sessionContext.isCallerInRole("MANAGER")) {
                throw new EJBAccessException("Only managers can approve high-value transactions");
            }
            
            // Log approval with full audit trail
            logSecurityEvent("HIGH_VALUE_APPROVAL", caller.getName(), 
                "Transaction: " + transactionId + " - " + justification);
            
            return true;
            
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Transaction approval failed: " + e.getMessage(), e);
            logSecurityEvent("HIGH_VALUE_APPROVAL", sessionContext.getCallerPrincipal().getName(), "FAILED");
            return false;
        }
    }
    
    // --- Administrator Access Methods (@RolesAllowed("ADMIN")) ---
    
    /**
     * System-wide security configuration
     * Highest privilege level required
     */
    @RolesAllowed("ADMIN")
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public boolean updateSecurityConfiguration(SecurityConfiguration config) {
        try {
            Principal caller = sessionContext.getCallerPrincipal();
            logger.info("🔒 Admin access: Security configuration update by " + caller.getName());
            
            // Verify admin privileges
            if (!sessionContext.isCallerInRole("ADMIN")) {
                throw new EJBAccessException("Administrator privileges required");
            }
            
            // In real implementation, update security settings
            logger.info("Security configuration updated: " + config.toString());
            
            logSecurityEvent("SECURITY_CONFIG", caller.getName(), "SUCCESS - Configuration updated");
            return true;
            
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Security configuration update failed: " + e.getMessage(), e);
            logSecurityEvent("SECURITY_CONFIG", sessionContext.getCallerPrincipal().getName(), "FAILED");
            return false;
        }
    }
    
    /**
     * Emergency security lockdown
     * Critical administrative function
     */
    @RolesAllowed("ADMIN")
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public void emergencyLockdown(String reason) {
        try {
            Principal caller = sessionContext.getCallerPrincipal();
            logger.warning("🚨 EMERGENCY LOCKDOWN initiated by " + caller.getName() + ": " + reason);
            
            // In real implementation, disable all non-admin access
            logSecurityEvent("EMERGENCY_LOCKDOWN", caller.getName(), "INITIATED - " + reason);
            
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Emergency lockdown failed: " + e.getMessage(), e);
        }
    }
    
    // --- Restricted Methods (@DenyAll) ---
    
    /**
     * Internal security method - No external access allowed
     * Demonstrates @DenyAll annotation for internal methods
     */
    @DenyAll
    public void internalSecurityOperation() {
        logger.warning("🚫 SECURITY VIOLATION: Attempt to access internal security method");
        // This method cannot be called externally due to @DenyAll
    }
    
    /**
     * Debug method - Disabled in production
     */
    @DenyAll
    @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
    public void debugSecurityContext() {
        // This method is completely blocked for security reasons
        logger.warning("🚫 BLOCKED: Debug method access attempt");
    }
    
    // --- Auditor Access Methods (@RolesAllowed("AUDITOR")) ---
    
    /**
     * Read-only access to security logs for compliance
     * Special auditor role with limited access
     */
    @RolesAllowed({"AUDITOR", "ADMIN"})
    @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
    public List<SecurityLogEntry> getSecurityLogs(LocalDateTime startDate, LocalDateTime endDate) {
        try {
            Principal caller = sessionContext.getCallerPrincipal();
            logger.info("🔍 Auditor access: Security logs requested by " + caller.getName());
            
            // Return mock security log entries for demonstration
            List<SecurityLogEntry> logs = new ArrayList<>();
            
            SecurityLogEntry entry1 = new SecurityLogEntry();
            entry1.setTimestamp(LocalDateTime.now().minusHours(1));
            entry1.setUser("customer1");
            entry1.setAction("LOGIN");
            entry1.setResult("SUCCESS");
            entry1.setDetails("Customer portal access");
            logs.add(entry1);
            
            SecurityLogEntry entry2 = new SecurityLogEntry();
            entry2.setTimestamp(LocalDateTime.now().minusHours(2));
            entry2.setUser("employee1");
            entry2.setAction("CUSTOMER_SEARCH");
            entry2.setResult("SUCCESS");
            entry2.setDetails("Customer service lookup");
            logs.add(entry2);
            
            logSecurityEvent("LOG_ACCESS", caller.getName(), "SUCCESS - " + logs.size() + " entries");
            return logs;
            
        } catch (Exception e) {
            logger.log(Level.WARNING, "Security log access failed: " + e.getMessage(), e);
            logSecurityEvent("LOG_ACCESS", sessionContext.getCallerPrincipal().getName(), "FAILED");
            throw new EJBAccessException("Failed to access security logs: " + e.getMessage());
        }
    }
    
    // --- Helper Methods ---
    
    /**
     * Get current user's roles from security context
     * Demonstrates JAAS integration
     */
    private List<String> getCurrentUserRoles() {
        List<String> roles = new ArrayList<>();
        
        String[] allRoles = {"ADMIN", "MANAGER", "EMPLOYEE", "CUSTOMER", "AUDITOR", "SYSTEM", "GUEST"};
        
        for (String role : allRoles) {
            if (sessionContext.isCallerInRole(role)) {
                roles.add(role);
            }
        }
        
        return roles;
    }
    
    /**
     * Log security events for audit trail
     * Critical for compliance and monitoring
     */
    private void logSecurityEvent(String action, String user, String result) {
        try {
            String logEntry = String.format("[%s] SECURITY_EVENT: %s by %s - %s",
                LocalDateTime.now().toString(), action, user, result);
            
            logger.info("🛡️ " + logEntry);
            
            // In real implementation, persist to audit database
            
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Failed to log security event: " + e.getMessage(), e);
        }
    }
    
    // --- Inner Classes for Security Data Transfer ---
    
    public static class SecurityProfile {
        private String username;
        private List<String> roles;
        private LocalDateTime lastLogin;
        private String securityLevel;
        
        // Getters and setters
        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }
        
        public List<String> getRoles() { return roles; }
        public void setRoles(List<String> roles) { this.roles = roles; }
        
        public LocalDateTime getLastLogin() { return lastLogin; }
        public void setLastLogin(LocalDateTime lastLogin) { this.lastLogin = lastLogin; }
        
        public String getSecurityLevel() { return securityLevel; }
        public void setSecurityLevel(String securityLevel) { this.securityLevel = securityLevel; }
    }
    
    public static class SecurityAuditReport {
        private String generatedBy;
        private LocalDateTime generatedAt;
        private LocalDateTime periodStart;
        private LocalDateTime periodEnd;
        private int totalSecurityEvents;
        private int failedAttempts;
        private int successfulAccess;
        
        // Getters and setters
        public String getGeneratedBy() { return generatedBy; }
        public void setGeneratedBy(String generatedBy) { this.generatedBy = generatedBy; }
        
        public LocalDateTime getGeneratedAt() { return generatedAt; }
        public void setGeneratedAt(LocalDateTime generatedAt) { this.generatedAt = generatedAt; }
        
        public LocalDateTime getPeriodStart() { return periodStart; }
        public void setPeriodStart(LocalDateTime periodStart) { this.periodStart = periodStart; }
        
        public LocalDateTime getPeriodEnd() { return periodEnd; }
        public void setPeriodEnd(LocalDateTime periodEnd) { this.periodEnd = periodEnd; }
        
        public int getTotalSecurityEvents() { return totalSecurityEvents; }
        public void setTotalSecurityEvents(int totalSecurityEvents) { this.totalSecurityEvents = totalSecurityEvents; }
        
        public int getFailedAttempts() { return failedAttempts; }
        public void setFailedAttempts(int failedAttempts) { this.failedAttempts = failedAttempts; }
        
        public int getSuccessfulAccess() { return successfulAccess; }
        public void setSuccessfulAccess(int successfulAccess) { this.successfulAccess = successfulAccess; }
    }
    
    public static class SecurityConfiguration {
        private int sessionTimeout;
        private int maxLoginAttempts;
        private boolean enableAuditLogging;
        private String encryptionLevel;
        
        // Getters and setters
        public int getSessionTimeout() { return sessionTimeout; }
        public void setSessionTimeout(int sessionTimeout) { this.sessionTimeout = sessionTimeout; }
        
        public int getMaxLoginAttempts() { return maxLoginAttempts; }
        public void setMaxLoginAttempts(int maxLoginAttempts) { this.maxLoginAttempts = maxLoginAttempts; }
        
        public boolean isEnableAuditLogging() { return enableAuditLogging; }
        public void setEnableAuditLogging(boolean enableAuditLogging) { this.enableAuditLogging = enableAuditLogging; }
        
        public String getEncryptionLevel() { return encryptionLevel; }
        public void setEncryptionLevel(String encryptionLevel) { this.encryptionLevel = encryptionLevel; }
        
        @Override
        public String toString() {
            return String.format("SecurityConfig{timeout=%d, maxAttempts=%d, audit=%b, encryption=%s}",
                sessionTimeout, maxLoginAttempts, enableAuditLogging, encryptionLevel);
        }
    }
    
    public static class SecurityLogEntry {
        private LocalDateTime timestamp;
        private String user;
        private String action;
        private String result;
        private String details;
        
        // Getters and setters
        public LocalDateTime getTimestamp() { return timestamp; }
        public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
        
        public String getUser() { return user; }
        public void setUser(String user) { this.user = user; }
        
        public String getAction() { return action; }
        public void setAction(String action) { this.action = action; }
        
        public String getResult() { return result; }
        public void setResult(String result) { this.result = result; }
        
        public String getDetails() { return details; }
        public void setDetails(String details) { this.details = details; }
    }
} 