package com.ngo.dao;

import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.Map;

/** Dashboard totals; every value comes from a SQL aggregate query. */
public final class DashboardDao {
    private DashboardDao() { }

    private static String one(String sql) throws SQLException {
        Object o = GenericDao.query(sql).rows.get(0)[0];
        return o == null ? "0" : String.format("%,.0f", ((Number) o).doubleValue());
    }

    public static Map<String, String> totals() throws SQLException {
        Map<String, String> m = new LinkedHashMap<>();
        m.put("Total Programmes", one("SELECT COUNT(*) FROM programme"));
        m.put("Total Campaigns", one("SELECT COUNT(*) FROM campaign"));
        m.put("Total Donors", one("SELECT COUNT(*) FROM donor"));
        m.put("Total Donations (count)", one("SELECT COUNT(*) FROM donation"));
        m.put("Total Donations (INR)", one("SELECT SUM(amount) FROM donation"));
        m.put("Total Beneficiaries", one("SELECT COUNT(*) FROM beneficiary"));
        m.put("Total Volunteers", one("SELECT COUNT(*) FROM volunteer"));
        m.put("Total Expenses (INR)", one("SELECT SUM(amount) FROM expense"));
        m.put("Pending Requests", one("SELECT COUNT(*) FROM request WHERE status = 'Pending'"));
        m.put("Aid Distributed (INR)", one("SELECT SUM(amount_or_quantity) FROM distribution"));
        return m;
    }
}
