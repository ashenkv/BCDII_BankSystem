package com.bank.rest;

import com.bank.entity.Customer;
import com.bank.entity.CustomerStatus;
import com.bank.ejb.ClientManagementService;
import com.bank.exception.BankingException;

import jakarta.ejb.EJB;
import jakarta.enterprise.context.RequestScoped;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import java.net.URI;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.time.LocalDateTime;

/**
 * PublicApiController - Public REST API endpoints for testing without authentication
 * 
 * This controller provides public endpoints for demonstration and testing purposes.
 * In a production environment, these would be secured with proper authentication.
 */
@Path("/public")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@RequestScoped
public class PublicApiController {
    
    private static final Logger logger = Logger.getLogger(PublicApiController.class.getName());
    
    @EJB
    private ClientManagementService clientManagementService;
    
    @Context
    private UriInfo uriInfo;
    
    /**
     * Test endpoint to verify API connectivity
     * GET /api/public/test
     */
    @GET
    @Path("/test")
    public Response testConnection() {
        try {
            logger.info("Test endpoint accessed");
            return Response.ok()
                .entity(new SuccessResponse("Banking System API is running successfully! " + 
                    "Current time: " + LocalDateTime.now()))
                .build();
                
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error in test endpoint: " + e.getMessage(), e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity(new ErrorResponse("TEST_FAILED", "Test endpoint failed: " + e.getMessage()))
                .build();
        }
    }
    
    /**
     * Gets customer count (public for testing)
     * GET /api/public/customers/count
     */
    @GET
    @Path("/customers/count")
    public Response getCustomerCount() {
        try {
            long count = clientManagementService.getPublicClientCount();
            logger.info("Customer count requested: " + count);
            return Response.ok()
                .entity(new CountResponse(count))
                .build();
                
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error getting customer count: " + e.getMessage(), e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity(new ErrorResponse("RETRIEVAL_FAILED", "Failed to get customer count: " + e.getMessage()))
                .build();
        }
    }
    
    /**
     * Gets all customers (public for testing)
     * GET /api/public/customers
     */
    @GET
    @Path("/customers")
    public Response getAllCustomers() {
        try {
            List<Customer> customers = clientManagementService.retrieveActiveClientsPublic();
            logger.info("All customers requested, count: " + customers.size());
            return Response.ok(customers).build();
            
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error retrieving customers: " + e.getMessage(), e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity(new ErrorResponse("RETRIEVAL_FAILED", "Failed to retrieve customers: " + e.getMessage()))
                .build();
        }
    }
    
    // Sample customer creation method removed - using real data only
    
    // Sample customer creation endpoint removed - using real data only
    
    /**
     * Creates a real customer with proper data validation
     * POST /api/public/customers/create
     */
    @POST
    @Path("/customers/create")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response createRealCustomer(CustomerRequest customerRequest) {
        try {
            logger.info("Creating real customer: " + customerRequest.getFirstName() + " " + customerRequest.getLastName());
            
            // Validate required fields
            if (customerRequest.getFirstName() == null || customerRequest.getFirstName().trim().isEmpty()) {
                return Response.status(Response.Status.BAD_REQUEST)
                    .entity(new ErrorResponse("INVALID_INPUT", "First name is required"))
                    .build();
            }
            
            if (customerRequest.getLastName() == null || customerRequest.getLastName().trim().isEmpty()) {
                return Response.status(Response.Status.BAD_REQUEST)
                    .entity(new ErrorResponse("INVALID_INPUT", "Last name is required"))
                    .build();
            }
            
            if (customerRequest.getEmail() == null || customerRequest.getEmail().trim().isEmpty()) {
                return Response.status(Response.Status.BAD_REQUEST)
                    .entity(new ErrorResponse("INVALID_INPUT", "Email is required"))
                    .build();
            }
            
            // Create customer entity
            Customer customer = new Customer();
            
            // Generate unique customer ID
            String customerId = "CUST" + System.currentTimeMillis();
            customer.setCustomerId(customerId);
            customer.setFirstName(customerRequest.getFirstName().trim());
            customer.setLastName(customerRequest.getLastName().trim());
            customer.setEmail(customerRequest.getEmail().trim().toLowerCase());
            customer.setPhoneNumber(customerRequest.getPhoneNumber() != null ? customerRequest.getPhoneNumber().trim() : null);
            customer.setAddress(customerRequest.getAddress() != null ? customerRequest.getAddress().trim() : null);
            
            // Parse date of birth if provided
            if (customerRequest.getDateOfBirth() != null && !customerRequest.getDateOfBirth().trim().isEmpty()) {
                try {
                    java.time.LocalDate localDate = java.time.LocalDate.parse(customerRequest.getDateOfBirth());
                    customer.setDateOfBirth(java.sql.Date.valueOf(localDate));
                } catch (Exception e) {
                    logger.warning("Invalid date format: " + customerRequest.getDateOfBirth());
                }
            }
            
            // Set status
            try {
                customer.setStatus(CustomerStatus.valueOf(customerRequest.getStatus().toUpperCase()));
            } catch (Exception e) {
                customer.setStatus(CustomerStatus.ACTIVE); // Default to ACTIVE
            }
            
            // Set timestamps
            customer.setCreatedAt(LocalDateTime.now());
            customer.setUpdatedAt(LocalDateTime.now());
            
            // Save customer
            Customer savedCustomer = clientManagementService.registerNewClient(customer);
            
            logger.info("Real customer created successfully: " + savedCustomer.getCustomerId());
            
            return Response.status(Response.Status.CREATED)
                .entity(savedCustomer)
                .build();
                
        } catch (BankingException e) {
            logger.log(Level.WARNING, "Business error creating customer: " + e.getMessage(), e);
            return Response.status(Response.Status.BAD_REQUEST)
                .entity(new ErrorResponse("BUSINESS_ERROR", e.getUserMessage()))
                .build();
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error creating real customer: " + e.getMessage(), e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity(new ErrorResponse("CREATION_FAILED", "Failed to create customer: " + e.getMessage()))
                .build();
        }
    }
    
    // Sample data population method removed - using real data only
    
    /**
     * Gets database statistics
     * GET /api/public/customers/stats
     */
    @GET
    @Path("/customers/stats")
    public Response getDatabaseStats() {
        try {
            long totalCustomers = clientManagementService.getPublicClientCount();
            List<Customer> allCustomers = clientManagementService.retrieveActiveClientsPublic();
            
            // Count customers by status
            long activeCount = allCustomers.stream().mapToLong(c -> c.getStatus() == CustomerStatus.ACTIVE ? 1 : 0).sum();
            long inactiveCount = allCustomers.stream().mapToLong(c -> c.getStatus() == CustomerStatus.INACTIVE ? 1 : 0).sum();
            
            DatabaseStatsResponse stats = new DatabaseStatsResponse();
            stats.setTotalCustomers(totalCustomers);
            stats.setActiveCustomers(activeCount);
            stats.setInactiveCustomers(inactiveCount);
            stats.setLastUpdated(LocalDateTime.now().toString());
            
            return Response.ok(stats).build();
            
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error getting database stats: " + e.getMessage(), e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity(new ErrorResponse("STATS_ERROR", "Failed to get database statistics: " + e.getMessage()))
                .build();
        }
    }
    
    /**
     * Gets system status and timer information
     * GET /api/public/status
     */
    @GET
    @Path("/status")
    public Response getSystemStatus() {
        try {
            SystemStatusResponse status = new SystemStatusResponse();
            status.setApiStatus("Running");
            status.setCurrentTime(LocalDateTime.now().toString());
            status.setTimerServicesEnabled(true);
            status.setDatabaseConnected(true);
            
            // Try to get customer count to verify database connectivity
            try {
                long customerCount = clientManagementService.getPublicClientCount();
                status.setCustomerCount(customerCount);
            } catch (Exception e) {
                status.setDatabaseConnected(false);
                status.setCustomerCount(0);
            }
            
            return Response.ok(status).build();
            
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error getting system status: " + e.getMessage(), e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity(new ErrorResponse("STATUS_ERROR", "Failed to get system status: " + e.getMessage()))
                .build();
        }
    }
    
    /**
     * Gets comprehensive system information
     * GET /api/public/info
     */
    @GET
    @Path("/info")
    public Response getSystemInfo() {
        try {
            SystemInfoResponse info = new SystemInfoResponse();
            info.setSystemName("Enterprise Banking System");
            info.setVersion("1.0.0");
            info.setDescription("EJB-based banking application with JAX-RS REST API");
            info.setDatabaseEngine("H2 Database Engine");
            info.setSecurityFramework("JAAS with Role-Based Access Control");
            info.setTimerServices("EJB Timer Services for Scheduled Operations");
            info.setFeatures("Customer Management, Account Operations, Transaction Processing, Interest Calculations");
            info.setCurrentTime(LocalDateTime.now().toString());
            
            // Database connectivity check
            try {
                long customerCount = clientManagementService.getPublicClientCount();
                info.setDatabaseStatus("Connected");
                info.setTotalCustomers(customerCount);
            } catch (Exception e) {
                info.setDatabaseStatus("Connection Error");
                info.setTotalCustomers(0);
            }
            
            return Response.ok(info).build();
            
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error getting system info: " + e.getMessage(), e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity(new ErrorResponse("INFO_ERROR", "Failed to get system information: " + e.getMessage()))
                .build();
        }
    }
    
    // Response DTOs
    public static class ErrorResponse {
        private String errorCode;
        private String message;
        private String timestamp;
        
        public ErrorResponse(String errorCode, String message) {
            this.errorCode = errorCode;
            this.message = message;
            this.timestamp = LocalDateTime.now().toString();
        }
        
        public String getErrorCode() { return errorCode; }
        public String getMessage() { return message; }
        public String getTimestamp() { return timestamp; }
    }
    
    public static class SuccessResponse {
        private String message;
        private String timestamp;
        private String customerId;
        
        // Single parameter constructor for general responses
        public SuccessResponse(String message) {
            this.message = message;
            this.timestamp = LocalDateTime.now().toString();
            this.customerId = null;
        }
        
        // Two parameter constructor for customer creation responses
        public SuccessResponse(String message, String customerId) {
            this.message = message;
            this.timestamp = LocalDateTime.now().toString();
            this.customerId = customerId;
        }
        
        public String getMessage() { return message; }
        public String getTimestamp() { return timestamp; }
        public String getCustomerId() { return customerId; }
    }
    
    public static class CountResponse {
        private long count;
        private String timestamp;
        
        public CountResponse(long count) {
            this.count = count;
            this.timestamp = LocalDateTime.now().toString();
        }
        
        public long getCount() { return count; }
        public String getTimestamp() { return timestamp; }
    }
    
    public static class SystemStatusResponse {
        private String apiStatus;
        private String currentTime;
        private boolean timerServicesEnabled;
        private boolean databaseConnected;
        private long customerCount;
        
        // Getters and setters
        public String getApiStatus() { return apiStatus; }
        public void setApiStatus(String apiStatus) { this.apiStatus = apiStatus; }
        
        public String getCurrentTime() { return currentTime; }
        public void setCurrentTime(String currentTime) { this.currentTime = currentTime; }
        
        public boolean isTimerServicesEnabled() { return timerServicesEnabled; }
        public void setTimerServicesEnabled(boolean timerServicesEnabled) { this.timerServicesEnabled = timerServicesEnabled; }
        
        public boolean isDatabaseConnected() { return databaseConnected; }
        public void setDatabaseConnected(boolean databaseConnected) { this.databaseConnected = databaseConnected; }
        
        public long getCustomerCount() { return customerCount; }
        public void setCustomerCount(long customerCount) { this.customerCount = customerCount; }
    }
    
    // Customer Request DTO for real customer creation
    public static class CustomerRequest {
        private String firstName;
        private String lastName;
        private String email;
        private String phoneNumber;
        private String address;
        private String dateOfBirth;
        private String status;
        
        // Default constructor
        public CustomerRequest() {}
        
        // Getters and setters
        public String getFirstName() { return firstName; }
        public void setFirstName(String firstName) { this.firstName = firstName; }
        
        public String getLastName() { return lastName; }
        public void setLastName(String lastName) { this.lastName = lastName; }
        
        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
        
        public String getPhoneNumber() { return phoneNumber; }
        public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }
        
        public String getAddress() { return address; }
        public void setAddress(String address) { this.address = address; }
        
        public String getDateOfBirth() { return dateOfBirth; }
        public void setDateOfBirth(String dateOfBirth) { this.dateOfBirth = dateOfBirth; }
        
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
    }

    // Data Population Response DTO
    public static class DataPopulationResponse {
        private String message;
        private int createdCount;
        private int skippedCount;
        private int totalProcessed;

        public DataPopulationResponse(String message, int createdCount, int skippedCount, int totalProcessed) {
            this.message = message;
            this.createdCount = createdCount;
            this.skippedCount = skippedCount;
            this.totalProcessed = totalProcessed;
        }

        public String getMessage() { return message; }
        public int getCreatedCount() { return createdCount; }
        public int getSkippedCount() { return skippedCount; }
        public int getTotalProcessed() { return totalProcessed; }
    }

    // Database Stats Response DTO
    public static class DatabaseStatsResponse {
        private long totalCustomers;
        private long activeCustomers;
        private long inactiveCustomers;
        private String lastUpdated;

        public long getTotalCustomers() { return totalCustomers; }
        public void setTotalCustomers(long totalCustomers) { this.totalCustomers = totalCustomers; }

        public long getActiveCustomers() { return activeCustomers; }
        public void setActiveCustomers(long activeCustomers) { this.activeCustomers = activeCustomers; }

        public long getInactiveCustomers() { return inactiveCustomers; }
        public void setInactiveCustomers(long inactiveCustomers) { this.inactiveCustomers = inactiveCustomers; }

        public String getLastUpdated() { return lastUpdated; }
        public void setLastUpdated(String lastUpdated) { this.lastUpdated = lastUpdated; }
    }
    
    // System Info Response DTO
    public static class SystemInfoResponse {
        private String systemName;
        private String version;
        private String description;
        private String databaseEngine;
        private String securityFramework;
        private String timerServices;
        private String features;
        private String currentTime;
        private String databaseStatus;
        private long totalCustomers;

        // Getters and setters
        public String getSystemName() { return systemName; }
        public void setSystemName(String systemName) { this.systemName = systemName; }

        public String getVersion() { return version; }
        public void setVersion(String version) { this.version = version; }

        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }

        public String getDatabaseEngine() { return databaseEngine; }
        public void setDatabaseEngine(String databaseEngine) { this.databaseEngine = databaseEngine; }

        public String getSecurityFramework() { return securityFramework; }
        public void setSecurityFramework(String securityFramework) { this.securityFramework = securityFramework; }

        public String getTimerServices() { return timerServices; }
        public void setTimerServices(String timerServices) { this.timerServices = timerServices; }

        public String getFeatures() { return features; }
        public void setFeatures(String features) { this.features = features; }

        public String getCurrentTime() { return currentTime; }
        public void setCurrentTime(String currentTime) { this.currentTime = currentTime; }

        public String getDatabaseStatus() { return databaseStatus; }
        public void setDatabaseStatus(String databaseStatus) { this.databaseStatus = databaseStatus; }

        public long getTotalCustomers() { return totalCustomers; }
        public void setTotalCustomers(long totalCustomers) { this.totalCustomers = totalCustomers; }
    }
} 