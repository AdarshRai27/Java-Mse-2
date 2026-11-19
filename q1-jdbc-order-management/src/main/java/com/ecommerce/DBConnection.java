package com.ecommerce;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Creates JDBC connections for the application.
 *
 * <p>Connection settings come from environment variables so that a real
 * password is never stored in the source code:</p>
 * <ul>
 *     <li>DB_URL  (default: jdbc:mysql://localhost:3306/ecommerce_db)</li>
 *     <li>DB_USER (default: root)</li>
 *     <li>DB_PASS (default: empty)</li>
 * </ul>
 */
public final class DBConnection {

    private static final String DEFAULT_URL = "jdbc:mysql://localhost:3306/ecommerce_db";
    private static final String DEFAULT_USER = "root";
    private static final String DEFAULT_PASSWORD = "";

    // Utility class: it should not be instantiated.
    private DBConnection() {
    }

    /**
     * Opens a new connection using DriverManager.
     *
     * <p>The MySQL driver is discovered automatically by JDBC 4+ (service
     * loader), so an explicit Class.forName(...) call is not required.</p>
     */
    public static Connection getConnection() throws SQLException {
        String url = getEnvOrDefault("DB_URL", DEFAULT_URL);
        String user = getEnvOrDefault("DB_USER", DEFAULT_USER);
        String password = getEnvOrDefault("DB_PASS", DEFAULT_PASSWORD);

        return DriverManager.getConnection(url, user, password);
    }

    private static String getEnvOrDefault(String name, String fallback) {
        String value = System.getenv(name);
        return (value == null || value.isBlank()) ? fallback : value;
    }
}
