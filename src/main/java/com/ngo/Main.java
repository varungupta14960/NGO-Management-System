package com.ngo;

import com.ngo.database.DatabaseConnection;
import com.ngo.ui.MainFrame;
import javax.swing.*;

/** Application entry point. */
public class Main {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); } catch (Exception ignored) { }
            String problem = DatabaseConnection.testConnection();
            if (problem != null) {
                JOptionPane.showMessageDialog(null,
                    "Cannot connect to the MySQL database.\n\n"
                  + "1. Make sure MySQL is running.\n"
                  + "2. Run database/database.sql, sample_data.sql and views.sql.\n"
                  + "3. Check user/password in src/main/resources/db.properties (or db.properties next to the jar).\n\n"
                  + "Details: " + problem, "Database connection failed", JOptionPane.ERROR_MESSAGE);
                System.exit(1);
            }
            new MainFrame().setVisible(true);
        });
    }
}
