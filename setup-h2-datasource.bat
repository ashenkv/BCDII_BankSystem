@echo off
echo ================================================
echo Banking System H2 Database Setup for Payara
echo ================================================
echo.

REM Change to Payara bin directory
cd /d "C:\payara6\bin"

echo Step 1: Copying H2 JAR file...
if exist "C:\Users\DELL USER\IdeaProjects\BankSystem\h2-2.2.224.jar" (
    copy "C:\Users\DELL USER\IdeaProjects\BankSystem\h2-2.2.224.jar" "..\glassfish\lib\"
    echo H2 JAR copied successfully.
) else (
    echo WARNING: H2 JAR file not found in project directory.
    echo Please manually copy h2-2.2.224.jar to C:\payara6\glassfish\lib\
)
echo.

echo Step 2: Creating JDBC Connection Pool...
asadmin create-jdbc-connection-pool --datasourceclassname org.h2.jdbcx.JdbcDataSource --restype javax.sql.DataSource --property url="jdbc\\:h2\\:~/banking-db;AUTO_SERVER=TRUE;AUTO_SERVER_PORT=9090":user=sa:password=sa:databaseName=banking-db BankingConnectionPool

if %ERRORLEVEL% EQU 0 (
    echo Connection pool created successfully.
) else (
    echo Connection pool creation failed or already exists.
)
echo.

echo Step 3: Creating JDBC Resource...
asadmin create-jdbc-resource --connectionpoolid BankingConnectionPool jdbc/BankingDS

if %ERRORLEVEL% EQU 0 (
    echo JDBC resource created successfully.
) else (
    echo JDBC resource creation failed or already exists.
)
echo.

echo Step 4: Testing connection...
asadmin ping-connection-pool BankingConnectionPool

if %ERRORLEVEL% EQU 0 (
    echo ✓ Connection test successful!
) else (
    echo ✗ Connection test failed. Check configuration.
)
echo.

echo Step 5: Listing configured resources...
echo Connection Pools:
asadmin list-jdbc-connection-pools
echo.
echo JDBC Resources:
asadmin list-jdbc-resources
echo.

echo ================================================
echo Setup Complete!
echo ================================================
echo.
echo Next steps:
echo 1. Deploy your banking-system.war file
echo 2. Access the application at http://localhost:8080/banking-system
echo 3. Check server logs for any deployment issues
echo.
echo Admin Console: http://localhost:4848
echo Application URL: http://localhost:8080/banking-system
echo.
pause 