package com.ngo.service;

import com.ngo.database.DatabaseConnection;
import com.ngo.util.AppException;
import java.sql.*;

/** Volunteer assignment with an application-level overlap check inside a transaction (the DB trigger re-checks). */
public final class AssignmentService {
    private AssignmentService() { }

    public static int assign(int volunteerId, int skillId, int programmeId, Date start, Date end, String role) throws Exception {
        if (end.before(start)) throw new AppException("End date cannot be before the assignment date.");
        try (Connection c = DatabaseConnection.getConnection()) {
            c.setAutoCommit(false);
            try {
                try (PreparedStatement ps = c.prepareStatement("SELECT volunteer_id FROM volunteer WHERE volunteer_id = ? FOR UPDATE")) {
                    ps.setInt(1, volunteerId);              // lock the volunteer row: two clerks cannot assign at once
                    try (ResultSet rs = ps.executeQuery()) { if (!rs.next()) throw new AppException("Volunteer does not exist."); }
                }
                try (PreparedStatement ps = c.prepareStatement(
                        "SELECT p.programme_name, a.assignment_date, a.end_date FROM assignment a "
                      + "JOIN programme p ON p.programme_id = a.programme_id "
                      + "WHERE a.volunteer_id = ? AND a.assignment_date <= ? AND a.end_date >= ? LIMIT 1")) {
                    ps.setInt(1, volunteerId); ps.setDate(2, end); ps.setDate(3, start);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) throw new AppException("Overlapping assignment: volunteer is already assigned to '"
                                + rs.getString(1) + "' from " + rs.getDate(2) + " to " + rs.getDate(3) + ".");
                    }
                }
                int id;
                try (PreparedStatement ps = c.prepareStatement(
                        "INSERT INTO assignment (volunteer_id, skill_id, programme_id, assignment_date, end_date, role) VALUES (?,?,?,?,?,?)",
                        Statement.RETURN_GENERATED_KEYS)) {
                    ps.setInt(1, volunteerId); ps.setInt(2, skillId); ps.setInt(3, programmeId);
                    ps.setDate(4, start); ps.setDate(5, end); ps.setString(6, role);
                    ps.executeUpdate();
                    try (ResultSet k = ps.getGeneratedKeys()) { k.next(); id = k.getInt(1); }
                }
                c.commit();
                return id;
            } catch (Exception e) { c.rollback(); throw e; }
            finally { c.setAutoCommit(true); }
        }
    }
}
