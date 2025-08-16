package com.bank.interceptor;

import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.Interceptor;
import jakarta.interceptor.InvocationContext;
import java.io.Serializable;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * LoggingInterceptor - Handles cross-cutting logging concerns
 * 
 * This interceptor demonstrates:
 * - Method entry/exit logging
 * - Performance monitoring
 * - Exception logging
 * - Parameter sanitization for security
 */
@Interceptor
public class LoggingInterceptor implements Serializable {
    
    private static final long serialVersionUID = 1L;
    private static final Logger logger = Logger.getLogger(LoggingInterceptor.class.getName());
    
    /**
     * Intercepts method calls to provide logging functionality
     * 
     * @param context InvocationContext containing method information
     * @return The result of the intercepted method call
     * @throws Exception if the intercepted method throws an exception
     */
    @AroundInvoke
    public Object logMethodCall(InvocationContext context) throws Exception {
        
        // Get method information
        String className = context.getTarget().getClass().getSimpleName();
        String methodName = context.getMethod().getName();
        Object[] parameters = context.getParameters();
        
        // Start timing
        long startTime = System.currentTimeMillis();
        
        // Log method entry with sanitized parameters
        if (logger.isLoggable(Level.FINE)) {
            String sanitizedParams = sanitizeParameters(parameters);
            logger.fine(String.format("ENTERING: %s.%s(%s)", 
                className, methodName, sanitizedParams));
        }
        
        Object result = null;
        Exception thrownException = null;
        
        try {
            // Proceed with the actual method call
            result = context.proceed();
            
            return result;
            
        } catch (Exception e) {
            thrownException = e;
            
            // Log exception details
            logger.log(Level.WARNING, String.format("EXCEPTION in %s.%s: %s", 
                className, methodName, e.getMessage()), e);
            
            // Re-throw the exception
            throw e;
            
        } finally {
            // Calculate execution time
            long executionTime = System.currentTimeMillis() - startTime;
            
            // Log method exit with execution time
            if (thrownException == null) {
                // Successful execution
                if (logger.isLoggable(Level.FINE)) {
                    logger.fine(String.format("EXITING: %s.%s - Execution time: %d ms", 
                        className, methodName, executionTime));
                }
                
                // Log performance warning for slow operations
                if (executionTime > 5000) { // 5 seconds
                    logger.warning(String.format("SLOW OPERATION: %s.%s took %d ms to complete", 
                        className, methodName, executionTime));
                }
                
            } else {
                // Exception thrown
                logger.warning(String.format("FAILED: %s.%s - Execution time: %d ms, Exception: %s", 
                    className, methodName, executionTime, thrownException.getClass().getSimpleName()));
            }
            
            // Log detailed performance metrics for specific operations
            logPerformanceMetrics(className, methodName, executionTime, result, thrownException);
        }
    }
    
    /**
     * Sanitizes method parameters for logging to avoid logging sensitive information
     * 
     * @param parameters Array of method parameters
     * @return Sanitized string representation of parameters
     */
    private String sanitizeParameters(Object[] parameters) {
        if (parameters == null || parameters.length == 0) {
            return "";
        }
        
        StringBuilder sanitized = new StringBuilder();
        
        for (int i = 0; i < parameters.length; i++) {
            if (i > 0) {
                sanitized.append(", ");
            }
            
            Object param = parameters[i];
            
            if (param == null) {
                sanitized.append("null");
            } else {
                String paramString = param.toString();
                String paramType = param.getClass().getSimpleName();
                
                // Sanitize sensitive information
                if (isSensitiveParameter(paramType, paramString)) {
                    sanitized.append(paramType).append(":[***HIDDEN***]");
                } else if (paramString.length() > 100) {
                    // Truncate long parameters
                    sanitized.append(paramType).append(":[")
                            .append(paramString.substring(0, 97))
                            .append("...]");
                } else {
                    sanitized.append(paramType).append(":[").append(paramString).append("]");
                }
            }
        }
        
        return sanitized.toString();
    }
    
    /**
     * Determines if a parameter contains sensitive information that should not be logged
     * 
     * @param paramType The type of the parameter
     * @param paramString The string representation of the parameter
     * @return true if the parameter is sensitive
     */
    private boolean isSensitiveParameter(String paramType, String paramString) {
        // Check for sensitive parameter types
        if (paramType.toLowerCase().contains("password") || 
            paramType.toLowerCase().contains("credential") ||
            paramType.toLowerCase().contains("secret")) {
            return true;
        }
        
        // Check for sensitive parameter values (case-insensitive)
        String lowerParam = paramString.toLowerCase();
        return lowerParam.contains("password") || 
               lowerParam.contains("ssn") || 
               lowerParam.contains("social") ||
               lowerParam.contains("credit") ||
               lowerParam.contains("pin") ||
               lowerParam.contains("cvv");
    }
    
    /**
     * Logs detailed performance metrics for specific operations
     * 
     * @param className Name of the class
     * @param methodName Name of the method
     * @param executionTime Execution time in milliseconds
     * @param result Method result (if successful)
     * @param exception Exception thrown (if any)
     */
    private void logPerformanceMetrics(String className, String methodName, 
                                     long executionTime, Object result, Exception exception) {
        
        // Only log metrics for service classes (not for every method call)
        if (!className.endsWith("Service")) {
            return;
        }
        
        StringBuilder metrics = new StringBuilder();
        metrics.append("METRICS: ")
               .append(className).append(".").append(methodName)
               .append(" - Duration: ").append(executionTime).append("ms");
        
        // Add operation-specific metrics
        if (methodName.startsWith("transfer") || methodName.startsWith("deposit") || methodName.startsWith("withdraw")) {
            metrics.append(" [TRANSACTION]");
            
            if (executionTime > 2000) { // Transactions taking more than 2 seconds
                metrics.append(" [SLOW_TRANSACTION]");
            }
        }
        
        if (methodName.startsWith("find") || methodName.startsWith("get")) {
            metrics.append(" [QUERY]");
            
            if (executionTime > 1000) { // Queries taking more than 1 second
                metrics.append(" [SLOW_QUERY]");
            }
        }
        
        if (methodName.startsWith("create") || methodName.startsWith("update") || methodName.startsWith("delete")) {
            metrics.append(" [CRUD]");
        }
        
        // Add result information
        if (exception != null) {
            metrics.append(" [FAILED: ").append(exception.getClass().getSimpleName()).append("]");
        } else {
            metrics.append(" [SUCCESS]");
            
            // Add result type information if available
            if (result != null) {
                if (result instanceof java.util.List) {
                    metrics.append(" [LIST_SIZE: ").append(((java.util.List<?>) result).size()).append("]");
                } else if (result instanceof java.util.Collection) {
                    metrics.append(" [COLLECTION_SIZE: ").append(((java.util.Collection<?>) result).size()).append("]");
                }
            }
        }
        
        // Log as INFO for important operations, FINE for others
        if (methodName.contains("transfer") || methodName.contains("Transaction") || 
            executionTime > 1000 || exception != null) {
            logger.info(metrics.toString());
        } else {
            logger.fine(metrics.toString());
        }
    }
} 