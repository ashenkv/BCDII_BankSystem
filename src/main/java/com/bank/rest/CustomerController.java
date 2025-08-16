package com.bank.rest;

import com.bank.entity.Customer;
import com.bank.entity.CustomerStatus;
import com.bank.ejb.ClientManagementService;
import com.bank.exception.BankingException;
import com.bank.exception.CustomerNotFoundException;

import jakarta.annotation.security.RolesAllowed;
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

/**
 * CustomerController - REST API for customer management
 * 
 * Provides endpoints for:
 * - Creating customers
 * - Retrieving customer information
 * - Updating customer details
 * - Managing customer status
 */
@Path("/customers")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@RequestScoped
public class CustomerController {
    
    private static final Logger logger = Logger.getLogger(CustomerController.class.getName());
    
    @EJB
    private ClientManagementService clientManagementService;
    
    @Context
    private UriInfo uriInfo;
    
    /**
     * Creates a new customer
     * POST /api/customers
     */
    @POST
    @RolesAllowed({"ADMIN", "MANAGER"})
    public Response createCustomer(@Valid @NotNull Customer customer) {
        try {
            Customer createdCustomer = clientManagementService.registerNewClient(customer);
            
            URI location = uriInfo.getAbsolutePathBuilder()
                .path(createdCustomer.getCustomerId())
                .build();
            
            return Response.created(location)
                .entity(createdCustomer)
                .build();
                
        } catch (BankingException e) {
            logger.log(Level.WARNING, "Error creating customer: " + e.getMessage(), e);
            return Response.status(Response.Status.BAD_REQUEST)
                .entity(new ErrorResponse("CREATION_FAILED", e.getUserMessage()))
                .build();
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Unexpected error creating customer: " + e.getMessage(), e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity(new ErrorResponse("INTERNAL_ERROR", "An unexpected error occurred"))
                .build();
        }
    }
    
    /**
     * Gets all customers
     * GET /api/customers
     */
    @GET
    @RolesAllowed({"ADMIN", "MANAGER"})
    public Response getAllCustomers() {
        try {
            List<Customer> customers = clientManagementService.retrieveAllClients();
            return Response.ok(customers).build();
            
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error retrieving customers: " + e.getMessage(), e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity(new ErrorResponse("RETRIEVAL_FAILED", "Failed to retrieve customers"))
                .build();
        }
    }
    
    /**
     * Gets a customer by ID
     * GET /api/customers/{customerId}
     */
    @GET
    @Path("/{customerId}")
    @RolesAllowed({"ADMIN", "MANAGER", "EMPLOYEE", "CUSTOMER"})
    public Response getCustomer(@PathParam("customerId") String customerId) {
        try {
            Customer customer = clientManagementService.retrieveClientByCustomerId(customerId);
            
            if (customer == null) {
                return Response.status(Response.Status.NOT_FOUND)
                    .entity(new ErrorResponse("CUSTOMER_NOT_FOUND", "Customer not found: " + customerId))
                    .build();
            }
            
            return Response.ok(customer).build();
            
        } catch (Exception e) {
            logger.log(Level.WARNING, "Error retrieving customer: " + e.getMessage(), e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity(new ErrorResponse("RETRIEVAL_FAILED", "Failed to retrieve customer"))
                .build();
        }
    }
    
    /**
     * Updates a customer
     * PUT /api/customers/{customerId}
     */
    @PUT
    @Path("/{customerId}")
    @RolesAllowed({"ADMIN", "MANAGER", "EMPLOYEE"})
    public Response updateCustomer(@PathParam("customerId") String customerId, 
                                 @Valid @NotNull Customer customer) {
        try {
            // Ensure the customer ID in the path matches the entity
            customer.setCustomerId(customerId);
            
            Customer updatedCustomer = clientManagementService.modifyClientDetails(customer);
            return Response.ok(updatedCustomer).build();
            
        } catch (CustomerNotFoundException e) {
            return Response.status(Response.Status.NOT_FOUND)
                .entity(new ErrorResponse("CUSTOMER_NOT_FOUND", e.getMessage()))
                .build();
        } catch (BankingException e) {
            logger.log(Level.WARNING, "Error updating customer: " + e.getMessage(), e);
            return Response.status(Response.Status.BAD_REQUEST)
                .entity(new ErrorResponse("UPDATE_FAILED", e.getUserMessage()))
                .build();
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Unexpected error updating customer: " + e.getMessage(), e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity(new ErrorResponse("INTERNAL_ERROR", "An unexpected error occurred"))
                .build();
        }
    }
    
    /**
     * Gets customers by status
     * GET /api/customers/status/{status}
     */
    @GET
    @Path("/status/{status}")
    @RolesAllowed({"ADMIN", "MANAGER"})
    public Response getCustomersByStatus(@PathParam("status") String statusString) {
        try {
            CustomerStatus status = CustomerStatus.valueOf(statusString.toUpperCase());
            List<Customer> customers = clientManagementService.retrieveClientsByStatus(status);
            return Response.ok(customers).build();
            
        } catch (IllegalArgumentException e) {
            return Response.status(Response.Status.BAD_REQUEST)
                .entity(new ErrorResponse("INVALID_STATUS", "Invalid customer status: " + statusString))
                .build();
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error retrieving customers by status: " + e.getMessage(), e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity(new ErrorResponse("RETRIEVAL_FAILED", "Failed to retrieve customers"))
                .build();
        }
    }
    
    /**
     * Activates a customer
     * POST /api/customers/{customerId}/activate
     */
    @POST
    @Path("/{customerId}/activate")
    @RolesAllowed({"ADMIN", "MANAGER"})
    public Response activateCustomer(@PathParam("customerId") String customerId) {
        try {
            clientManagementService.reactivateClientAccount(customerId);
            return Response.ok()
                .entity(new SuccessResponse("Customer activated successfully"))
                .build();
                
        } catch (CustomerNotFoundException e) {
            return Response.status(Response.Status.NOT_FOUND)
                .entity(new ErrorResponse("CUSTOMER_NOT_FOUND", e.getMessage()))
                .build();
        } catch (BankingException e) {
            logger.log(Level.WARNING, "Error activating customer: " + e.getMessage(), e);
            return Response.status(Response.Status.BAD_REQUEST)
                .entity(new ErrorResponse("ACTIVATION_FAILED", e.getUserMessage()))
                .build();
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Unexpected error activating customer: " + e.getMessage(), e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity(new ErrorResponse("INTERNAL_ERROR", "An unexpected error occurred"))
                .build();
        }
    }
    
    /**
     * Deactivates a customer
     * POST /api/customers/{customerId}/deactivate
     */
    @POST
    @Path("/{customerId}/deactivate")
    @RolesAllowed({"ADMIN", "MANAGER"})
    public Response deactivateCustomer(@PathParam("customerId") String customerId) {
        try {
            clientManagementService.suspendClientAccount(customerId);
            return Response.ok()
                .entity(new SuccessResponse("Customer deactivated successfully"))
                .build();
                
        } catch (CustomerNotFoundException e) {
            return Response.status(Response.Status.NOT_FOUND)
                .entity(new ErrorResponse("CUSTOMER_NOT_FOUND", e.getMessage()))
                .build();
        } catch (BankingException e) {
            logger.log(Level.WARNING, "Error deactivating customer: " + e.getMessage(), e);
            return Response.status(Response.Status.BAD_REQUEST)
                .entity(new ErrorResponse("DEACTIVATION_FAILED", e.getUserMessage()))
                .build();
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Unexpected error deactivating customer: " + e.getMessage(), e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity(new ErrorResponse("INTERNAL_ERROR", "An unexpected error occurred"))
                .build();
        }
    }
    
    /**
     * Deletes a customer (soft delete)
     * DELETE /api/customers/{customerId}
     */
    @DELETE
    @Path("/{customerId}")
    @RolesAllowed({"ADMIN"})
    public Response deleteCustomer(@PathParam("customerId") String customerId) {
        try {
            clientManagementService.removeClientAccount(customerId);
            return Response.ok()
                .entity(new SuccessResponse("Customer deleted successfully"))
                .build();
                
        } catch (CustomerNotFoundException e) {
            return Response.status(Response.Status.NOT_FOUND)
                .entity(new ErrorResponse("CUSTOMER_NOT_FOUND", e.getMessage()))
                .build();
        } catch (BankingException e) {
            logger.log(Level.WARNING, "Error deleting customer: " + e.getMessage(), e);
            return Response.status(Response.Status.BAD_REQUEST)
                .entity(new ErrorResponse("DELETION_FAILED", e.getUserMessage()))
                .build();
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Unexpected error deleting customer: " + e.getMessage(), e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity(new ErrorResponse("INTERNAL_ERROR", "An unexpected error occurred"))
                .build();
        }
    }
    
    /**
     * Gets customer count
     * GET /api/customers/count
     */
    @GET
    @Path("/count")
    @RolesAllowed({"ADMIN", "MANAGER"})
    public Response getCustomerCount() {
        try {
            long count = clientManagementService.getClientCount();
            return Response.ok()
                .entity(new CountResponse(count))
                .build();
                
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error getting customer count: " + e.getMessage(), e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity(new ErrorResponse("RETRIEVAL_FAILED", "Failed to get customer count"))
                .build();
        }
    }
    
    // Response DTOs
    public static class ErrorResponse {
        private String errorCode;
        private String message;
        
        public ErrorResponse(String errorCode, String message) {
            this.errorCode = errorCode;
            this.message = message;
        }
        
        public String getErrorCode() { return errorCode; }
        public String getMessage() { return message; }
    }
    
    public static class SuccessResponse {
        private String message;
        
        public SuccessResponse(String message) {
            this.message = message;
        }
        
        public String getMessage() { return message; }
    }
    
    public static class CountResponse {
        private long count;
        
        public CountResponse(long count) {
            this.count = count;
        }
        
        public long getCount() { return count; }
    }
} 