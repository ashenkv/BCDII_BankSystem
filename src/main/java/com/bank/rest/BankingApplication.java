package com.bank.rest;

import jakarta.ws.rs.ApplicationPath;
import jakarta.ws.rs.core.Application;

/**
 * JAX-RS Application Configuration
 * Configures the REST API endpoints at /api/* path
 */
@ApplicationPath("/api")
public class BankingApplication extends Application {
    // The class body can be empty - the @ApplicationPath annotation
    // is sufficient to configure the application
} 