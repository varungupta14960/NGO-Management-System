package com.ngo.database;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

/** Single place where JDBC connections are created and credentials are configured. */
public final class DatabaseConnection {
    private static final Properties CONFIG = load();

    private DatabaseConnection() { }

    private static Properties load() {
        Properties p = new Properties();
        p.setProperty("db.url", "jdbc:mysql://localhost:3306/ngo_db?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC");
        p.setProperty("db.user", "root");
        p.setProperty("db.password", "");
        try (InputStream in = DatabaseConnection.class.getResourceAsStream("/db.properties")) {
            if (in != null) p.load(in);
        } catch (Exception ignored) { /* defaults are used */ }
        File external = new File("db.properties");
        if (external.exists()) {
            try (InputStream in = new FileInputStream(external)) { p.load(in); } catch (Exception ignored) { }
        }
        if (System.getenv("NGO_DB_URL") != null) p.setProperty("db.url", System.getenv("NGO_DB_URL"));
        if (System.getenv("NGO_DB_USER") != null) p.setProperty("db.user", System.getenv("NGO_DB_USER"));
        if (System.getenv("NGO_DB_PASSWORD") != null) p.setProperty("db.password", System.getenv("NGO_DB_PASSWORD"));
        return p;
    }

    /** Opens a new connection (callers close it with try-with-resources). */
    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(CONFIG.getProperty("db.url"),
                CONFIG.getProperty("db.user"), CONFIG.getProperty("db.password"));
    }

    /** Returns null when the database is reachable, otherwise a short problem description. */
    public static String testConnection() {
        try (Connection c = getConnection()) {
            return c.isValid(3) ? null : "Connection is not valid";
        } catch (SQLException e) {
            return e.getMessage();
        }
    }
}
