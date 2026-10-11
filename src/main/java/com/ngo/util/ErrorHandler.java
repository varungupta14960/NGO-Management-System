package com.ngo.util;

import java.sql.SQLException;

/** Converts technical exceptions into short user-friendly messages (no stack traces on screen). */
public final class ErrorHandler {
    private ErrorHandler() { }

    public static String message(Throwable t) {
        if (t instanceof AppException) return t.getMessage();
        if (t instanceof SQLException) return fromSql((SQLException) t);
        System.err.println("Unexpected error: " + t);
        return "An unexpected error occurred: " + t.getClass().getSimpleName();
    }

    private static String fromSql(SQLException e) {
        String raw = clean(e.getMessage());
        String state = e.getSQLState() == null ? "" : e.getSQLState();
        int code = e.getErrorCode();
        System.err.println("SQL error [" + state + "/" + code + "]: " + raw);
        if ("45000".equals(state)) return raw;                       // business-rule trigger message
        if (state.startsWith("08") || raw.contains("Communications link") || raw.contains("Could not connect")
                || raw.contains("Connection refused"))
            return "Cannot connect to the database. Check that MySQL is running and the settings in db.properties are correct.";
        if (code == 1062) return "Duplicate record: a record with the same unique value (name, e-mail, phone, ...) already exists.";
        if (code == 1451 || code == 1217) return "This record cannot be deleted/changed because other records depend on it.";
        if (code == 1452 || code == 1216) return "Invalid reference: the selected related record does not exist.";
        if (code == 3819 || code == 4025 || raw.contains("CONSTRAINT")) {
            String rule = raw.replaceAll("(?s).*?[`']?(ck_[a-z_]+)[`']?.*", "$1");
            return "A value violates a database rule" + (rule.startsWith("ck_") ? " (" + rule + ")" : "")
                    + ". Check amounts are positive, dates are in order and the e-mail/phone format is valid.";
        }
        if (code == 1048) return "A required value is missing.";
        if (code == 1292 || code == 1366 || code == 1265 || code == 1264) return "A value has an invalid format or is out of range.";
        return "Database error: " + raw;
    }

    private static String clean(String m) {
        if (m == null) return "unknown error";
        m = m.replaceAll("^\\(conn=\\d+\\)\\s*", "");
        int q = m.indexOf("\nQuery is:");
        if (q > 0) m = m.substring(0, q);
        return m.trim();
    }
}
