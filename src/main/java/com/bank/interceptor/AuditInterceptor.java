package com.bank.interceptor;

import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.Interceptor;
import jakarta.interceptor.InvocationContext;
import jakarta.annotation.Resource;
import jakarta.ejb.SessionContext;
import java.io.Serializable;
import java.math.BigDecimal;
import java.security.Principal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.logging.Logger;

/**
 * AuditInterceptor - Handles security auditing and compliance tracking
 * 
 * This interceptor demonstrates:
 * - User action auditing
 * - Security event logging
 * - Compliance tracking
 * - Financial transaction auditing
 * - Access control monitoring
 */
@Interceptor
public class AuditInterceptor implements Serializable {
    
    private static final long serialVersionUID = 1L;
    private static final Logger auditLogger = Logger.getLogger("AUDIT." + AuditInterceptor.class.getName());
    
    @Resource
    private SessionContext sessionContext;
    
    /**
     * Intercepts method calls to provide auditing functionality
     * 
     * @param context InvocationContext containing method information
     * @return The result of the intercepted method call
     * @throws Exception if the intercepted method throws an exception
     */
    @AroundInvoke
    public Object auditMethodCall(InvocationContext context) throws Exception {
        
        // Get audit information
        String className = context.getTarget().getClass().getSimpleName();
        String methodName = context.getMethod().getName();
        Object[] parameters = context.getParameters();
        
        // Get user information
        String username = getCurrentUser();
        String sessionId = getSessionId();
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        
        // Determine if this is an auditable operation
        boolean isAuditable = isAuditableOperation(className, methodName);
        
        if (isAuditable) {
            logAuditEntry(username, sessionId, timestamp, className, methodName, parameters, "STARTED");
        }
        
        Object result = null;
        Exception thrownException = null;
        
        try {
            // Proceed with the actual method call
            result = context.proceed();
            
            // Log successful completion for auditable operations
            if (isAuditable) {
                logAuditEntry(username, sessionId, timestamp, className, methodName, parameters, "COMPLETED");
                logBusinessEvent(username, className, methodName, parameters, result);
            }
            
            return result;
            
        } catch (Exception e) {
            thrownException = e;
            
            // Log failed operations
            if (isAuditable) {
                logAuditEntry(username, sessionId, timestamp, className, methodName, parameters, 
                    "FAILED: " + e.getClass().getSimpleName() + " - " + e.getMessage());
                logSecurityEvent(username, className, methodName, e);
            }
            
            // Re-throw the exception
            throw e;
        }
    }
    
    /**
     * Determines if an operation should be audited
     * 
     * @param className Name of the class
     * @param methodName Name of the method
     * @return true if the operation should be audited
     */
    private boolean isAuditableOperation(String className, String methodName) {
        // Always audit service layer operations
        if (className.endsWith("Service")) {
            return true;
        }
        
        // Always audit timer operations
        if (className.contains("Timer")) {
            return true;
        }
        
        // Audit specific method patterns
        return methodName.startsWith("create") ||
               methodName.startsWith("update") ||
               methodName.startsWith("delete") ||
               methodName.startsWith("transfer") ||
               methodName.startsWith("deposit") ||
               methodName.startsWith("withdraw") ||
               methodName.startsWith("activate") ||
               methodName.startsWith("deactivate") ||
               methodName.startsWith("close") ||
               methodName.startsWith("freeze") ||
               methodName.startsWith("reverse");
    }
    
    /**
     * Logs a general audit entry
     * 
     * @param username Current user
     * @param sessionId Session identifier
     * @param timestamp Operation timestamp
     * @param className Class name
     * @param methodName Method name
     * @param parameters Method parameters
     * @param status Operation status
     */
    private void logAuditEntry(String username, String sessionId, String timestamp, 
                              String className, String methodName, Object[] parameters, String status) {
        
        StringBuilder auditEntry = new StringBuilder();
        auditEntry.append("AUDIT_ENTRY|")
                  .append("USER=").append(username).append("|")
                  .append("SESSION=").append(sessionId).append("|")
                  .append("TIMESTAMP=").append(timestamp).append("|")
                  .append("CLASS=").append(className).append("|")
                  .append("METHOD=").append(methodName).append("|")
                  .append("STATUS=").append(status).append("|");
        
        // Add sanitized parameter information for specific operations
        if (isFinancialOperation(methodName)) {
            auditEntry.append("PARAMS=").append(sanitizeFinancialParameters(parameters)).append("|");
        }
        
        auditLogger.info(auditEntry.toString());
    }
    
    /**
     * Logs business events for compliance and analysis
     * 
     * @param username Current user
     * @param className Class name
     * @param methodName Method name
     * @param parameters Method parameters
     * @param result Method result
     */
    private void logBusinessEvent(String username, String className, String methodName, 
                                Object[] parameters, Object result) {
        
        StringBuilder businessEvent = new StringBuilder();
        businessEvent.append("BUSINESS_EVENT|")
                     .append("USER=").append(username).append("|")
                     .append("OPERATION=").append(getBusinessOperation(methodName)).append("|");
        
        // Add specific business context
        if (methodName.contains("transfer") || methodName.contains("Transfer")) {
            logTransferEvent(businessEvent, parameters, result);
        } else if (methodName.contains("deposit") || methodName.contains("Deposit")) {
            logDepositEvent(businessEvent, parameters, result);
        } else if (methodName.contains("withdraw") || methodName.contains("Withdraw")) {
            logWithdrawalEvent(businessEvent, parameters, result);
        } else if (methodName.contains("create") && className.contains("Customer")) {
            logCustomerCreationEvent(businessEvent, parameters, result);
        } else if (methodName.contains("create") && className.contains("Account")) {
            logAccountCreationEvent(businessEvent, parameters, result);
        }
        
        auditLogger.info(businessEvent.toString());
    }
    
    /**
     * Logs security events for monitoring and alerting
     * 
     * @param username Current user
     * @param className Class name
     * @param methodName Method name
     * @param exception Exception that occurred
     */
    private void logSecurityEvent(String username, String className, String methodName, Exception exception) {
        
        StringBuilder securityEvent = new StringBuilder();
        securityEvent.append("SECURITY_EVENT|")
                     .append("USER=").append(username).append("|")
                     .append("OPERATION=").append(className).append(".").append(methodName).append("|")
                     .append("EXCEPTION=").append(exception.getClass().getSimpleName()).append("|")
                     .append("MESSAGE=").append(exception.getMessage()).append("|");
        
        // Add security context
        if (exception.getMessage() != null) {
            String message = exception.getMessage().toLowerCase();
            
            if (message.contains("access") || message.contains("permission") || message.contains("unauthorized")) {
                securityEvent.append("TYPE=ACCESS_VIOLATION|");
            } else if (message.contains("authentication") || message.contains("login")) {
                securityEvent.append("TYPE=AUTHENTICATION_FAILURE|");
            } else if (message.contains("insufficient") || message.contains("funds")) {
                securityEvent.append("TYPE=INSUFFICIENT_FUNDS|");
            } else if (message.contains("not found")) {
                securityEvent.append("TYPE=RESOURCE_NOT_FOUND|");
            } else {
                securityEvent.append("TYPE=BUSINESS_EXCEPTION|");
            }
        }
        
        auditLogger.warning(securityEvent.toString());
    }
    
    /**
     * Gets the current user from the security context
     * 
     * @return Current username or "SYSTEM" if not available
     */
    private String getCurrentUser() {
        try {
            if (sessionContext != null) {
                Principal callerPrincipal = sessionContext.getCallerPrincipal();
                if (callerPrincipal != null) {
                    return callerPrincipal.getName();
                }
            }
        } catch (Exception e) {
            // If we can't get the user, log it but don't fail
            auditLogger.warning("Unable to determine current user: " + e.getMessage());
        }
        
        return "SYSTEM";
    }
    
    /**
     * Gets a session identifier (simplified implementation)
     * 
     * @return Session identifier
     */
    private String getSessionId() {
        // In a real implementation, this would extract the actual session ID
        // For demonstration purposes, we'll use thread ID and timestamp
        return "SESSION_" + Thread.currentThread().getId() + "_" + System.currentTimeMillis();
    }
    
    /**
     * Determines if an operation is financial in nature
     * 
     * @param methodName Method name
     * @return true if it's a financial operation
     */
    private boolean isFinancialOperation(String methodName) {
        return methodName.contains("transfer") ||
               methodName.contains("deposit") ||
               methodName.contains("withdraw") ||
               methodName.contains("Transaction") ||
               methodName.contains("Interest");
    }
    
    /**
     * Sanitizes financial parameters for auditing
     * 
     * @param parameters Method parameters
     * @return Sanitized parameter string
     */
    private String sanitizeFinancialParameters(Object[] parameters) {
        StringBuilder sanitized = new StringBuilder();
        
        // Handle null or empty parameters
        if (parameters == null || parameters.length == 0) {
            return "[]";
        }
        
        for (int i = 0; i < parameters.length; i++) {
            if (i > 0) sanitized.append(",");
            
            Object param = parameters[i];
            if (param instanceof BigDecimal) {
                sanitized.append("AMOUNT=").append(param);
            } else if (param instanceof String && param.toString().matches(".*\\d{10,}.*")) {
                // Looks like an account number - mask it
                String maskedAccount = maskAccountNumber(param.toString());
                sanitized.append("ACCOUNT=").append(maskedAccount);
            } else if (param != null) {
                sanitized.append(param.getClass().getSimpleName());
            }
        }
        
        return sanitized.toString();
    }
    
    /**
     * Masks account numbers for security
     * 
     * @param accountNumber Original account number
     * @return Masked account number
     */
    private String maskAccountNumber(String accountNumber) {
        if (accountNumber.length() <= 4) {
            return "****";
        }
        
        return "****" + accountNumber.substring(accountNumber.length() - 4);
    }
    
    /**
     * Gets business operation name from method name
     * 
     * @param methodName Method name
     * @return Business operation name
     */
    private String getBusinessOperation(String methodName) {
        if (methodName.contains("transfer")) return "FUND_TRANSFER";
        if (methodName.contains("deposit")) return "DEPOSIT";
        if (methodName.contains("withdraw")) return "WITHDRAWAL";
        if (methodName.contains("create") && methodName.contains("Customer")) return "CUSTOMER_CREATION";
        if (methodName.contains("create") && methodName.contains("Account")) return "ACCOUNT_CREATION";
        if (methodName.contains("Interest")) return "INTEREST_CALCULATION";
        if (methodName.contains("close")) return "ACCOUNT_CLOSURE";
        if (methodName.contains("freeze")) return "ACCOUNT_FREEZE";
        
        return methodName.toUpperCase();
    }
    
    /**
     * Logs transfer event details
     */
    private void logTransferEvent(StringBuilder event, Object[] parameters, Object result) {
        event.append("TRANSFER_TYPE=FUND_TRANSFER|");
        
        if (parameters != null && parameters.length >= 3) {
            event.append("SOURCE_ACCOUNT=").append(maskAccountNumber(parameters[0].toString())).append("|");
            event.append("TARGET_ACCOUNT=").append(maskAccountNumber(parameters[1].toString())).append("|");
            event.append("AMOUNT=").append(parameters[2]).append("|");
        }
        
        if (result != null) {
            event.append("TRANSACTION_ID=").append(result.toString()).append("|");
        }
    }
    
    /**
     * Logs deposit event details
     */
    private void logDepositEvent(StringBuilder event, Object[] parameters, Object result) {
        event.append("TRANSACTION_TYPE=DEPOSIT|");
        
        if (parameters != null && parameters.length >= 2) {
            event.append("ACCOUNT=").append(maskAccountNumber(parameters[0].toString())).append("|");
            event.append("AMOUNT=").append(parameters[1]).append("|");
        }
    }
    
    /**
     * Logs withdrawal event details
     */
    private void logWithdrawalEvent(StringBuilder event, Object[] parameters, Object result) {
        event.append("TRANSACTION_TYPE=WITHDRAWAL|");
        
        if (parameters != null && parameters.length >= 2) {
            event.append("ACCOUNT=").append(maskAccountNumber(parameters[0].toString())).append("|");
            event.append("AMOUNT=").append(parameters[1]).append("|");
        }
    }
    
    /**
     * Logs customer creation event details
     */
    private void logCustomerCreationEvent(StringBuilder event, Object[] parameters, Object result) {
        event.append("ENTITY_TYPE=CUSTOMER|");
        
        if (parameters != null && parameters.length > 0 && parameters[0] != null) {
            // Log customer ID if available (assuming first parameter is customer object)
            event.append("CUSTOMER_CREATED=TRUE|");
        }
    }
    
    /**
     * Logs account creation event details
     */
    private void logAccountCreationEvent(StringBuilder event, Object[] parameters, Object result) {
        event.append("ENTITY_TYPE=ACCOUNT|");
        
        if (parameters != null && parameters.length >= 2) {
            event.append("CUSTOMER_ID=").append(parameters[0]).append("|");
            event.append("ACCOUNT_TYPE=").append(parameters[1]).append("|");
        }
    }
} 