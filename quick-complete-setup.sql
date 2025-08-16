-- Complete Banking Database Setup
-- Copy and paste this ENTIRE script into H2 Console and click "Run"

-- Create Account Table
CREATE TABLE IF NOT EXISTS ACCOUNT (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    account_number VARCHAR(20) UNIQUE NOT NULL,
    account_type VARCHAR(20) DEFAULT 'SAVINGS',
    balance DECIMAL(15,2) DEFAULT 0.00,
    status VARCHAR(20) DEFAULT 'ACTIVE',
    customer_id BIGINT NOT NULL,
    created_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (customer_id) REFERENCES CUSTOMER(id)
);

-- Create Transaction Table
CREATE TABLE IF NOT EXISTS TRANSACTION (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    transaction_id VARCHAR(50) UNIQUE NOT NULL,
    transaction_type VARCHAR(20) NOT NULL,
    amount DECIMAL(15,2) NOT NULL,
    description VARCHAR(200),
    status VARCHAR(20) DEFAULT 'COMPLETED',
    from_account_id BIGINT,
    to_account_id BIGINT,
    transaction_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (from_account_id) REFERENCES ACCOUNT(id),
    FOREIGN KEY (to_account_id) REFERENCES ACCOUNT(id)
);

-- Create Sequence Table for EclipseLink TABLE sequencing
CREATE TABLE IF NOT EXISTS BANKING_SEQUENCE (
    SEQ_NAME VARCHAR(50) PRIMARY KEY,
    SEQ_COUNT BIGINT DEFAULT 1
);

-- Add missing columns to CUSTOMER table if needed
ALTER TABLE CUSTOMER ADD COLUMN IF NOT EXISTS customer_id VARCHAR(20);
ALTER TABLE CUSTOMER ADD COLUMN IF NOT EXISTS phone_number VARCHAR(15);
ALTER TABLE CUSTOMER ADD COLUMN IF NOT EXISTS address VARCHAR(200);
ALTER TABLE CUSTOMER ADD COLUMN IF NOT EXISTS date_of_birth DATE;
ALTER TABLE CUSTOMER ADD COLUMN IF NOT EXISTS status VARCHAR(20) DEFAULT 'ACTIVE';
ALTER TABLE CUSTOMER ADD COLUMN IF NOT EXISTS created_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE CUSTOMER ADD COLUMN IF NOT EXISTS updated_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP;

-- Initialize sequence values
INSERT INTO BANKING_SEQUENCE (SEQ_NAME, SEQ_COUNT) VALUES ('SEQ_GEN', 1) ON DUPLICATE KEY UPDATE SEQ_COUNT = SEQ_COUNT;

-- Insert Sample Customers (if not already there)
INSERT INTO CUSTOMER (customer_id, first_name, last_name, email, phone_number, address, date_of_birth, status) 
VALUES 
('CUST001', 'John', 'Doe', 'john.doe@email.com', '+1234567890', '123 Main St, City', '1990-01-15', 'ACTIVE'),
('CUST002', 'Jane', 'Smith', 'jane.smith@email.com', '+1234567891', '456 Oak Ave, Town', '1985-05-20', 'ACTIVE'),
('CUST003', 'Bob', 'Johnson', 'bob.johnson@email.com', '+1234567892', '789 Pine Rd, Village', '1992-09-10', 'ACTIVE')
ON DUPLICATE KEY UPDATE customer_id = customer_id;

-- Insert Sample Accounts
INSERT INTO ACCOUNT (account_number, account_type, balance, customer_id, status)
VALUES 
('ACC001', 'SAVINGS', 1500.00, 1, 'ACTIVE'),
('ACC002', 'CHECKING', 2500.00, 1, 'ACTIVE'),
('ACC003', 'SAVINGS', 3000.00, 2, 'ACTIVE'),
('ACC004', 'CHECKING', 1200.00, 3, 'ACTIVE')
ON DUPLICATE KEY UPDATE account_number = account_number;

-- Insert Sample Transactions
INSERT INTO TRANSACTION (transaction_id, transaction_type, amount, description, from_account_id, to_account_id, status)
VALUES 
('TXN001', 'DEPOSIT', 500.00, 'Initial deposit', NULL, 1, 'COMPLETED'),
('TXN002', 'WITHDRAWAL', 100.00, 'ATM withdrawal', 1, NULL, 'COMPLETED'),
('TXN003', 'TRANSFER', 200.00, 'Transfer to savings', 2, 1, 'COMPLETED'),
('TXN004', 'DEPOSIT', 1000.00, 'Salary deposit', NULL, 3, 'COMPLETED')
ON DUPLICATE KEY UPDATE transaction_id = transaction_id;

-- Verify setup
SELECT 'Setup Complete! Tables and Data Created:' as STATUS;

SELECT 'CUSTOMER' as TABLE_NAME, COUNT(*) as RECORD_COUNT FROM CUSTOMER
UNION ALL
SELECT 'ACCOUNT' as TABLE_NAME, COUNT(*) as RECORD_COUNT FROM ACCOUNT
UNION ALL
SELECT 'TRANSACTION' as TABLE_NAME, COUNT(*) as RECORD_COUNT FROM TRANSACTION
UNION ALL
SELECT 'BANKING_SEQUENCE' as TABLE_NAME, COUNT(*) as RECORD_COUNT FROM BANKING_SEQUENCE;

-- Show sample data
SELECT 'Sample Customer Data:' as INFO;
SELECT customer_id, first_name, last_name, email FROM CUSTOMER LIMIT 3;

SELECT 'Sample Account Data:' as INFO;
SELECT a.account_number, a.account_type, a.balance, c.first_name, c.last_name 
FROM ACCOUNT a JOIN CUSTOMER c ON a.customer_id = c.id LIMIT 4; 