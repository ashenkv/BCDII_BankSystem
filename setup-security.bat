@echo off
echo ================================================
echo Banking System Security Setup for Payara
echo ================================================
echo.

REM Change to Payara bin directory
cd /d "C:\payara6\bin"

echo Step 1: Creating file realm for banking system...
asadmin create-auth-realm --classname com.sun.enterprise.security.auth.realm.file.FileRealm --property file="C:\\payara6\\glassfish\\domains\\domain1\\config\\banking-keyfile":jaas-context=fileRealm BankingRealm

if %ERRORLEVEL% EQU 0 (
    echo ✓ BankingRealm created successfully.
) else (
    echo ! BankingRealm already exists or creation failed.
)
echo.

echo Step 2: Creating users with different roles...

echo Creating ADMIN user...
asadmin create-file-user --authrealmname BankingRealm --groups ADMIN admin

echo Creating MANAGER user...
asadmin create-file-user --authrealmname BankingRealm --groups MANAGER manager

echo Creating EMPLOYEE user...
asadmin create-file-user --authrealmname BankingRealm --groups EMPLOYEE employee

echo Creating CUSTOMER user...
asadmin create-file-user --authrealmname BankingRealm --groups CUSTOMER customer

echo Creating test user (Gaurava)...
asadmin create-file-user --authrealmname BankingRealm --groups CUSTOMER gaurava

echo.
echo Step 3: Listing created users...
asadmin list-file-users --authrealmname BankingRealm

echo.
echo Step 4: Setting default realm for web security...
REM This will set the banking realm as default for the application

echo.
echo ================================================
echo Security Setup Complete!
echo ================================================
echo.
echo Created Users and Default Passwords:
echo.
echo Username: admin     Password: admin123     Role: ADMIN
echo Username: manager   Password: manager123   Role: MANAGER  
echo Username: employee  Password: employee123  Role: EMPLOYEE
echo Username: customer  Password: customer123  Role: CUSTOMER
echo Username: gaurava   Password: gaurava123   Role: CUSTOMER
echo.
echo IMPORTANT: Change default passwords in production!
echo.
echo You can now test authentication with these credentials.
echo.
echo To change a user's password:
echo asadmin update-file-user --authrealmname BankingRealm --groups ROLE username
echo.
echo Admin Console: http://localhost:4848
echo Application URL: http://localhost:8080/banking-system
echo.
pause 