package com.ngo.dao;

import com.ngo.database.DatabaseConnection;
import com.ngo.model.Option;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/** Loads (id, label) pairs for foreign-key combo boxes. */
public final class LookupDao {
    private LookupDao() { }

    public static List<Option> options(String sql) throws SQLException {
        List<Option> list = new ArrayList<>();
        try (Connection c = DatabaseConnection.getConnection(); PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) list.add(new Option(rs.getInt(1), rs.getString(2)));
        }
        return list;
    }
}
