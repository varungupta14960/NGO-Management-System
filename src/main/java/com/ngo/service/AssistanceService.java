package com.ngo.service;

import com.ngo.database.DatabaseConnection;
import com.ngo.util.AppException;
import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDate;

/**
 * Beneficiary assistance workflow: request -> approval -> distribution.
 * Each public method is ONE transaction. The database triggers enforce the same rules a second time.
 */
public final class AssistanceService {
    private AssistanceService() { }

    /** Approve (approvedAmount > 0) or reject (status "Rejected") a pending request. */
    public static int decide(int requestId, String status, BigDecimal approvedAmount, Date date) throws Exception {
        try (Connection c = DatabaseConnection.getConnection()) {
            c.setAutoCommit(false);
            try {
                int id = insertApproval(c, requestId, status, approvedAmount, date);
                c.commit();
                return id;
            } catch (Exception e) { c.rollback(); throw e; }
            finally { c.setAutoCommit(true); }
        }
    }

    /** Record a (partial) distribution against an approved request. */
    public static int distribute(int requestId, Date date, BigDecimal amount, String type) throws Exception {
        try (Connection c = DatabaseConnection.getConnection()) {
            c.setAutoCommit(false);
            try {
                int id = insertDistribution(c, requestId, date, amount, type);
                c.commit();
                return id;
            } catch (Exception e) { c.rollback(); throw e; }
            finally { c.setAutoCommit(true); }
        }
    }

    /** Create request + approve + distribute atomically. If any step fails nothing is saved. Returns the request id. */
    public static int fullWorkflow(int beneficiaryId, int programmeId, String description, BigDecimal requested,
                                   BigDecimal approved, BigDecimal distributed, String type) throws Exception {
        Date today = Date.valueOf(LocalDate.now());
        try (Connection c = DatabaseConnection.getConnection()) {
            c.setAutoCommit(false);
            try {
                int requestId;
                try (PreparedStatement ps = c.prepareStatement(
                        "INSERT INTO request (beneficiary_id, programme_id, request_date, description, requested_amount) VALUES (?,?,?,?,?)",
                        Statement.RETURN_GENERATED_KEYS)) {
                    ps.setInt(1, beneficiaryId); ps.setInt(2, programmeId); ps.setDate(3, today);
                    ps.setString(4, description); ps.setBigDecimal(5, requested);
                    ps.executeUpdate();
                    try (ResultSet k = ps.getGeneratedKeys()) { k.next(); requestId = k.getInt(1); }
                }
                insertApproval(c, requestId, "Approved", approved, today);
                insertDistribution(c, requestId, today, distributed, type);
                c.commit();
                return requestId;
            } catch (Exception e) { c.rollback(); throw e; }
            finally { c.setAutoCommit(true); }
        }
    }

    // ---- helpers that run inside an existing transaction -------------------------------------------------

    private static int insertApproval(Connection c, int requestId, String status, BigDecimal amount, Date date) throws Exception {
        if (!"Approved".equals(status) && !"Rejected".equals(status)) throw new AppException("Decision must be Approved or Rejected.");
        BigDecimal requested; String reqStatus;
        try (PreparedStatement ps = c.prepareStatement("SELECT status, requested_amount FROM request WHERE request_id = ? FOR UPDATE")) {
            ps.setInt(1, requestId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) throw new AppException("Request does not exist.");
                reqStatus = rs.getString(1); requested = rs.getBigDecimal(2);
            }
        }
        if (!"Pending".equals(reqStatus)) throw new AppException("Request #" + requestId + " is " + reqStatus + "; only Pending requests can be approved or rejected.");
        if ("Rejected".equals(status)) amount = BigDecimal.ZERO;
        else {
            if (amount == null || amount.signum() <= 0) throw new AppException("Approved amount must be greater than zero.");
            if (amount.compareTo(requested) > 0) throw new AppException("Approved amount cannot exceed the requested amount (" + requested + ").");
        }
        try (PreparedStatement ps = c.prepareStatement(
                "INSERT INTO approval (request_id, approval_date, approved_amount, status) VALUES (?,?,?,?)", Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, requestId); ps.setDate(2, date); ps.setBigDecimal(3, amount); ps.setString(4, status);
            ps.executeUpdate();
            try (ResultSet k = ps.getGeneratedKeys()) { k.next(); return k.getInt(1); }
        }
    }

    private static int insertDistribution(Connection c, int requestId, Date date, BigDecimal amount, String type) throws Exception {
        if (amount == null || amount.signum() <= 0) throw new AppException("Distribution amount must be greater than zero.");
        String status;
        try (PreparedStatement ps = c.prepareStatement("SELECT status FROM request WHERE request_id = ? FOR UPDATE")) {
            ps.setInt(1, requestId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) throw new AppException("Request does not exist.");
                status = rs.getString(1);
            }
        }
        if (!"Approved".equals(status)) throw new AppException("Request #" + requestId + " is " + status + "; distribution needs an Approved request.");
        BigDecimal approved, done;
        try (PreparedStatement ps = c.prepareStatement(
                "SELECT a.approved_amount, (SELECT IFNULL(SUM(amount_or_quantity),0) FROM distribution WHERE request_id = ?) "
              + "FROM approval a WHERE a.request_id = ? AND a.status = 'Approved'")) {
            ps.setInt(1, requestId); ps.setInt(2, requestId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) throw new AppException("No approval found for this request.");
                approved = rs.getBigDecimal(1); done = rs.getBigDecimal(2);
            }
        }
        BigDecimal remaining = approved.subtract(done);
        if (amount.compareTo(remaining) > 0)
            throw new AppException("Distribution of " + amount + " exceeds the remaining approved amount. Approved: "
                    + approved + ", already distributed: " + done + ", remaining: " + remaining + ".");
        try (PreparedStatement ps = c.prepareStatement(
                "INSERT INTO distribution (request_id, distribution_date, amount_or_quantity, distribution_type) VALUES (?,?,?,?)",
                Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, requestId); ps.setDate(2, date); ps.setBigDecimal(3, amount); ps.setString(4, type);
            ps.executeUpdate();
            try (ResultSet k = ps.getGeneratedKeys()) { k.next(); return k.getInt(1); }
        }
    }
}
