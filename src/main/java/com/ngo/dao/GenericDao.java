package com.ngo.dao;

import com.ngo.database.DatabaseConnection;
import com.ngo.model.EntityDef;
import com.ngo.model.FieldDef;
import java.sql.*;
import java.util.*;

/**
 * CRUD for any EntityDef using PreparedStatement only. Table/column names come from the
 * EntityDef constants in the code (never from user input); all user values are bound as parameters.
 */
public final class GenericDao {
    private GenericDao() { }

    public static ResultTable query(String sql, Object... params) throws SQLException {
        try (Connection c = DatabaseConnection.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            for (int i = 0; i < params.length; i++) ps.setObject(i + 1, params[i]);
            try (ResultSet rs = ps.executeQuery()) {
                ResultSetMetaData md = rs.getMetaData();
                String[] cols = new String[md.getColumnCount()];
                for (int i = 0; i < cols.length; i++) cols[i] = md.getColumnLabel(i + 1);
                List<Object[]> rows = new ArrayList<>();
                while (rs.next()) {
                    Object[] row = new Object[cols.length];
                    for (int i = 0; i < cols.length; i++) row[i] = rs.getObject(i + 1);
                    rows.add(row);
                }
                return new ResultTable(cols, rows);
            }
        }
    }

    public static ResultTable list(EntityDef e, String search) throws SQLException {
        String sql = e.listSql;
        if (search != null && !search.trim().isEmpty())
            return query(sql + " WHERE " + e.searchExpr + " LIKE ? ORDER BY " + e.orderBy, "%" + search.trim() + "%");
        return query(sql + " ORDER BY " + e.orderBy);
    }

    /** Reads the raw column values of one row (for filling the edit form). */
    public static Map<String, Object> find(EntityDef e, int id) throws SQLException {
        StringBuilder cols = new StringBuilder();
        for (FieldDef f : e.fields) cols.append(cols.length() == 0 ? "" : ", ").append(f.column);
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement("SELECT " + cols + " FROM " + e.table + " WHERE " + e.pk + " = ?")) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return null;
                Map<String, Object> m = new LinkedHashMap<>();
                for (FieldDef f : e.fields) m.put(f.column, rs.getObject(f.column));
                return m;
            }
        }
    }

    public static int insert(EntityDef e, Map<String, Object> v) throws SQLException {
        StringBuilder cols = new StringBuilder(), qs = new StringBuilder();
        for (FieldDef f : e.fields) { cols.append(cols.length() == 0 ? "" : ", ").append(f.column); qs.append(qs.length() == 0 ? "?" : ", ?"); }
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement("INSERT INTO " + e.table + " (" + cols + ") VALUES (" + qs + ")", Statement.RETURN_GENERATED_KEYS)) {
            int i = 1;
            for (FieldDef f : e.fields) ps.setObject(i++, v.get(f.column));
            ps.executeUpdate();
            try (ResultSet k = ps.getGeneratedKeys()) { return k.next() ? k.getInt(1) : 0; }
        }
    }

    public static void update(EntityDef e, int id, Map<String, Object> v) throws SQLException {
        StringBuilder set = new StringBuilder();
        for (FieldDef f : e.fields) set.append(set.length() == 0 ? "" : ", ").append(f.column).append(" = ?");
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement("UPDATE " + e.table + " SET " + set + " WHERE " + e.pk + " = ?")) {
            int i = 1;
            for (FieldDef f : e.fields) ps.setObject(i++, v.get(f.column));
            ps.setInt(i, id);
            ps.executeUpdate();
        }
    }

    public static void delete(EntityDef e, int id) throws SQLException {
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement("DELETE FROM " + e.table + " WHERE " + e.pk + " = ?")) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }
}
