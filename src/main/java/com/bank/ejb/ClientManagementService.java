package com.bank.ejb;

import com.bank.entity.Customer;
import com.bank.entity.CustomerStatus;
import com.bank.exception.BankingException;
import com.bank.exception.CustomerNotFoundException;
import com.bank.interceptor.AuditInterceptor;
import com.bank.interceptor.LoggingInterceptor;

import jakarta.annotation.security.RolesAllowed;
import jakarta.annotation.security.PermitAll;
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
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * ClientManagementService EJB - Handles all client-related operations
 * Implements transaction management, security, and audit logging
 */
@Stateless
@Interceptors({LoggingInterceptor.class, AuditInterceptor.class})
public class ClientManagementService {
    
    private static final Logger logger = Logger.getLogger(ClientManagementService.class.getName());
    
    @PersistenceContext(unitName = "bankingPU")
    private EntityManager entityManager;
    
    /**
     * Creates a new client account
     * @param customer Customer entity to create
     * @return Created customer with generated ID
     * @throws BankingException if customer creation fails
     */
    @PermitAll  // Allow public access for demo purposes
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public Customer registerNewClient(@Valid @NotNull Customer customer) throws BankingException {
        try {
            logger.info("Creating new client: " + customer.getCustomerId());
            
            // Check if customer ID already exists
            if (retrieveClientByCustomerId(customer.getCustomerId()) != null) {
                throw new BankingException("Customer ID already exists: " + customer.getCustomerId());
            }
            
            // Check if email already exists
            if (retrieveClientByEmail(customer.getEmail()) != null) {
                throw new BankingException("Customer with email already exists: " + customer.getEmail());
            }
            
            // Persist the customer
            entityManager.persist(customer);
            entityManager.flush();
            
            logger.info("Client created successfully: " + customer.getId());
            return customer;
            
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error creating client: " + e.getMessage(), e);
            throw new BankingException("Failed to create client: " + e.getMessage(), e);
        }
    }
    
    /**
     * Updates an existing client
     * @param customer Customer entity to update
     * @return Updated customer
     * @throws BankingException if update fails
     */
    @RolesAllowed({"ADMIN", "MANAGER", "EMPLOYEE"})
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public Customer modifyClientDetails(@Valid @NotNull Customer customer) throws BankingException {
        try {
            logger.info("Updating client: " + customer.getId());
            
            Customer existingCustomer = entityManager.find(Customer.class, customer.getId());
            if (existingCustomer == null) {
                throw new CustomerNotFoundException("Customer not found with ID: " + customer.getId());
            }
            
            // Update fields
            existingCustomer.setFirstName(customer.getFirstName());
            existingCustomer.setLastName(customer.getLastName());
            existingCustomer.setEmail(customer.getEmail());
            existingCustomer.setPhoneNumber(customer.getPhoneNumber());
            existingCustomer.setAddress(customer.getAddress());
            existingCustomer.setDateOfBirth(customer.getDateOfBirth());
            existingCustomer.setStatus(customer.getStatus());
            
            Customer updatedCustomer = entityManager.merge(existingCustomer);
            entityManager.flush();
            
            logger.info("Client updated successfully: " + updatedCustomer.getId());
            return updatedCustomer;
            
        } catch (CustomerNotFoundException e) {
            throw e;
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error updating client: " + e.getMessage(), e);
            throw new BankingException("Failed to update client: " + e.getMessage(), e);
        }
    }
    
    /**
     * Finds a client by ID
     * @param customerId Customer ID to search for
     * @return Customer entity or null if not found
     */
    @PermitAll  // Allow public access for demo purposes
    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public Customer retrieveClientByCustomerId(@NotNull String customerId) {
        try {
            TypedQuery<Customer> query = entityManager.createNamedQuery("Customer.findByCustomerId", Customer.class);
            query.setParameter("customerId", customerId);
            return query.getSingleResult();
        } catch (NoResultException e) {
            logger.info("Client not found with ID: " + customerId);
            return null;
        } catch (Exception e) {
            logger.log(Level.WARNING, "Error finding client by ID: " + e.getMessage(), e);
            return null;
        }
    }
    
    /**
     * Finds a client by email
     * @param email Email to search for
     * @return Customer entity or null if not found
     */
    @RolesAllowed({"ADMIN", "MANAGER", "EMPLOYEE"})
    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public Customer retrieveClientByEmail(@NotNull String email) {
        try {
            TypedQuery<Customer> query = entityManager.createNamedQuery("Customer.findByEmail", Customer.class);
            query.setParameter("email", email);
            return query.getSingleResult();
        } catch (NoResultException e) {
            logger.info("Client not found with email: " + email);
            return null;
        } catch (Exception e) {
            logger.log(Level.WARNING, "Error finding client by email: " + e.getMessage(), e);
            return null;
        }
    }
    
    /**
     * Finds a client by database ID
     * @param id Database ID
     * @return Customer entity or null if not found
     */
    @RolesAllowed({"ADMIN", "MANAGER", "EMPLOYEE", "CUSTOMER"})
    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public Customer retrieveClientById(@NotNull Long id) {
        try {
            return entityManager.find(Customer.class, id);
        } catch (Exception e) {
            logger.log(Level.WARNING, "Error finding client by database ID: " + e.getMessage(), e);
            return null;
        }
    }
    
    /**
     * Gets all clients
     * @return List of all customers
     */
    @RolesAllowed({"ADMIN", "MANAGER"})
    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public List<Customer> retrieveAllClients() throws BankingException {
        try {
            TypedQuery<Customer> query = entityManager.createNamedQuery("Customer.findAll", Customer.class);
            return query.getResultList();
        } catch (Exception e) {
            logger.log(Level.WARNING, "Error getting all clients: " + e.getMessage(), e);
            throw new BankingException("Failed to retrieve clients: " + e.getMessage(), e);
        }
    }
    
    /**
     * Gets clients by status
     * @param status Customer status to filter by
     * @return List of customers with the specified status
     */
    @RolesAllowed({"ADMIN", "MANAGER"})
    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public List<Customer> retrieveClientsByStatus(@NotNull CustomerStatus status) throws BankingException {
        try {
            TypedQuery<Customer> query = entityManager.createQuery(
                "SELECT c FROM Customer c WHERE c.status = :status", Customer.class);
            query.setParameter("status", status);
            return query.getResultList();
        } catch (Exception e) {
            logger.log(Level.WARNING, "Error getting clients by status: " + e.getMessage(), e);
            throw new BankingException("Failed to retrieve clients by status: " + e.getMessage(), e);
        }
    }
    
    /**
     * Deactivates a client account
     * @param customerId Customer ID to deactivate
     * @throws BankingException if deactivation fails
     */
    @RolesAllowed({"ADMIN", "MANAGER"})
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public void suspendClientAccount(@NotNull String customerId) throws BankingException {
        try {
            logger.info("Deactivating client: " + customerId);
            
            Customer customer = retrieveClientByCustomerId(customerId);
            if (customer == null) {
                throw new CustomerNotFoundException("Customer not found: " + customerId);
            }
            
            customer.setStatus(CustomerStatus.INACTIVE);
            entityManager.merge(customer);
            entityManager.flush();
            
            logger.info("Client deactivated successfully: " + customerId);
            
        } catch (CustomerNotFoundException e) {
            throw e;
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error deactivating client: " + e.getMessage(), e);
            throw new BankingException("Failed to deactivate client: " + e.getMessage(), e);
        }
    }
    
    /**
     * Activates a client account
     * @param customerId Customer ID to activate
     * @throws BankingException if activation fails
     */
    @RolesAllowed({"ADMIN", "MANAGER"})
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public void reactivateClientAccount(@NotNull String customerId) throws BankingException {
        try {
            logger.info("Activating client: " + customerId);
            
            Customer customer = retrieveClientByCustomerId(customerId);
            if (customer == null) {
                throw new CustomerNotFoundException("Customer not found: " + customerId);
            }
            
            customer.setStatus(CustomerStatus.ACTIVE);
            entityManager.merge(customer);
            entityManager.flush();
            
            logger.info("Client activated successfully: " + customerId);
            
        } catch (CustomerNotFoundException e) {
            throw e;
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error activating client: " + e.getMessage(), e);
            throw new BankingException("Failed to activate client: " + e.getMessage(), e);
        }
    }
    
    /**
     * Deletes a client account
     * @param customerId Customer ID to delete
     * @throws BankingException if deletion fails
     */
    @RolesAllowed({"ADMIN"})
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public void removeClientAccount(@NotNull String customerId) throws BankingException {
        try {
            logger.info("Deleting client: " + customerId);
            
            Customer customer = retrieveClientByCustomerId(customerId);
            if (customer == null) {
                throw new CustomerNotFoundException("Customer not found: " + customerId);
            }
            
            // Check if customer has active accounts
            // In a real system, you would check for active accounts here
            
            entityManager.remove(customer);
            entityManager.flush();
            
            logger.info("Client deleted successfully: " + customerId);
            
        } catch (CustomerNotFoundException e) {
            throw e;
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error deleting client: " + e.getMessage(), e);
            throw new BankingException("Failed to delete client: " + e.getMessage(), e);
        }
    }
    
    /**
     * Checks if a client is active
     * @param customer Customer to check
     * @return true if customer is active, false otherwise
     */
    @PermitAll
    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public boolean isClientActive(@NotNull Customer customer) {
        return customer.getStatus() == CustomerStatus.ACTIVE;
    }
    
    /**
     * Gets the total number of clients (admin only)
     * @return Total client count
     */
    @RolesAllowed({"ADMIN", "MANAGER"})
    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public long getClientCount() {
        try {
            TypedQuery<Long> query = entityManager.createQuery(
                "SELECT COUNT(c) FROM Customer c", Long.class);
            return query.getSingleResult();
        } catch (Exception e) {
            logger.log(Level.WARNING, "Error getting client count: " + e.getMessage(), e);
            return 0;
        }
    }
    
    /**
     * Gets the total number of clients (public access)
     * @return Total client count
     */
    @PermitAll
    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public long getPublicClientCount() {
        try {
            TypedQuery<Long> query = entityManager.createQuery(
                "SELECT COUNT(c) FROM Customer c WHERE c.status = :status", Long.class);
            query.setParameter("status", CustomerStatus.ACTIVE);
            return query.getSingleResult();
        } catch (Exception e) {
            logger.log(Level.WARNING, "Error getting public client count: " + e.getMessage(), e);
            return 0;
        }
    }
    
    /**
     * Gets all active clients (public access)
     * @return List of active customers
     * @throws BankingException if retrieval fails
     */
    @PermitAll
    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public List<Customer> retrieveActiveClientsPublic() throws BankingException {
        try {
            TypedQuery<Customer> query = entityManager.createQuery(
                "SELECT c FROM Customer c WHERE c.status = :status", Customer.class);
            query.setParameter("status", CustomerStatus.ACTIVE);
            return query.getResultList();
        } catch (Exception e) {
            logger.log(Level.WARNING, "Error getting active clients: " + e.getMessage(), e);
            throw new BankingException("Failed to retrieve active clients: " + e.getMessage(), e);
        }
    }
    
    /**
     * Finds a client by email (public access)
     * @param email Email to search for
     * @return Customer entity or null if not found
     */
    @PermitAll
    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public Customer retrieveClientByEmailPublic(@NotNull String email) {
        try {
            TypedQuery<Customer> query = entityManager.createQuery(
                "SELECT c FROM Customer c WHERE c.email = :email AND c.status = :status", Customer.class);
            query.setParameter("email", email);
            query.setParameter("status", CustomerStatus.ACTIVE);
            return query.getSingleResult();
        } catch (NoResultException e) {
            logger.info("Active client not found with email: " + email);
            return null;
        } catch (Exception e) {
            logger.log(Level.WARNING, "Error finding active client by email: " + e.getMessage(), e);
            return null;
        }
    }
} 