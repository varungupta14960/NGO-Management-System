package com.ngo.service;

import com.ngo.database.DatabaseConnection;
import com.ngo.util.AppException;
import java.math.BigDecimal;
import java.sql.*;

/** Donation entry as one JDBC transaction: validate donor, validate campaign, insert, commit (else rollback). */
public final class DonationService {
    private DonationService() { }

    public static int recordDonation(int donorId, int campaignId, Date date, BigDecimal amount, String method) throws Exception {
        if (amount == null || amount.signum() <= 0) throw new AppException("Donation amount must be greater than zero.");
        try (Connection c = DatabaseConnection.getConnection()) {
            c.setAutoCommit(false);                                   // begin transaction
            try {
                try (PreparedStatement ps = c.prepareStatement("SELECT donor_id FROM donor WHERE donor_id = ? FOR UPDATE")) {
                    ps.setInt(1, donorId);
                    try (ResultSet rs = ps.executeQuery()) { if (!rs.next()) throw new AppException("Donor does not exist."); }
                }
                try (PreparedStatement ps = c.prepareStatement("SELECT start_date, end_date FROM campaign WHERE campaign_id = ? FOR UPDATE")) {
                    ps.setInt(1, campaignId);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (!rs.next()) throw new AppException("Campaign does not exist.");
                        Date start = rs.getDate(1), end = rs.getDate(2);
                        if (date.before(start)) throw new AppException("Donation date is before the campaign start date (" + start + ").");
                        if (end != null && date.after(end)) throw new AppException("Campaign ended on " + end + "; donations after that date are not accepted.");
                    }
                }
                int id;
                try (PreparedStatement ps = c.prepareStatement(
                        "INSERT INTO donation (donor_id, campaign_id, donation_date, amount, payment_method) VALUES (?,?,?,?,?)",
                        Statement.RETURN_GENERATED_KEYS)) {
                    ps.setInt(1, donorId); ps.setInt(2, campaignId); ps.setDate(3, date);
                    ps.setBigDecimal(4, amount); ps.setString(5, method);
                    ps.executeUpdate();
                    try (ResultSet k = ps.getGeneratedKeys()) { k.next(); id = k.getInt(1); }
                }
                c.commit();                                           // all steps succeeded
                return id;
            } catch (Exception e) {
                c.rollback();                                         // undo everything
                throw e;
            } finally {
                c.setAutoCommit(true);
            }
        }
    }
}
