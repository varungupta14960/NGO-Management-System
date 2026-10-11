package com.ngo;

import com.ngo.dao.*;
import com.ngo.database.DatabaseConnection;
import com.ngo.service.*;
import com.ngo.ui.*;
import com.ngo.util.ErrorHandler;
import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDate;
import java.util.concurrent.Callable;

/**
 * Headless end-to-end test (no JUnit needed): drives the same DAO / service / panel code the GUI uses against a
 * freshly loaded database and prints "| Test Case | Input | Expected | Actual | Status |" rows.
 * Run:  java -Djava.awt.headless=true -cp target/classes:target/test-classes:<driver.jar> com.ngo.IntegrationTest
 */
public class IntegrationTest {
    static int pass = 0, fail = 0;
    static final String TODAY = LocalDate.now().toString();

    static long count(String sql, Object... p) throws Exception { return ((Number) GenericDao.query(sql, p).rows.get(0)[0]).longValue(); }

    static void exec(String sql, Object... p) throws SQLException {
        try (Connection c = DatabaseConnection.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            for (int i = 0; i < p.length; i++) ps.setObject(i + 1, p[i]);
            ps.executeUpdate();
        }
    }

    /** Runs an action; the returned (or exception) text is the actual result; expectedPart must appear in it. */
    static void t(String id, String name, String input, String expected, String expectedPart, Callable<String> action) {
        String actual;
        try { actual = action.call(); } catch (Throwable e) { actual = ErrorHandler.message(e); }
        boolean ok = actual.toLowerCase().contains(expectedPart.toLowerCase());
        if (ok) pass++; else fail++;
        System.out.println("| " + id + " " + name + " | " + input + " | " + expected + " | " + actual.replace("|", "/") + " | " + (ok ? "PASS" : "FAIL") + " |");
    }

    public static void main(String[] args) throws Exception {
        if (args.length > 0 && args[0].equals("connfail")) {
            String r = DatabaseConnection.testConnection();
            System.out.println(r == null ? "connected" : ErrorHandler.message(new SQLException(r, "08S01")));
            return;
        }
        System.out.println("| Test Case | Input | Expected Result | Actual Result | Status |\n|---|---|---|---|---|");
        t("T01", "Java connects to MySQL", "DatabaseConnection.testConnection()", "Connection succeeds", "connected",
            () -> DatabaseConnection.testConnection() == null ? "connected" : "failed");

        // ---------------- donations
        t("T02", "Valid donation", "Donor 17, Campaign 6, 5000, UPI", "Donation saved and committed", "committed", () -> {
            long before = count("SELECT COUNT(*) FROM donation");
            int id = DonationService.recordDonation(17, 6, Date.valueOf(TODAY), new BigDecimal("5000"), "UPI");
            return "donation #" + id + " committed; rows " + before + " -> " + count("SELECT COUNT(*) FROM donation");
        });
        t("T03", "Negative donation (service)", "Amount -500", "Rejected: amount must be > 0", "greater than zero",
            () -> { DonationService.recordDonation(1, 6, Date.valueOf(TODAY), new BigDecimal("-500"), "UPI"); return "saved?!"; });
        t("T04", "Negative donation (CHECK constraint, raw SQL)", "INSERT amount = -500", "Database CHECK rejects", "violates a database rule",
            () -> { exec("INSERT INTO donation (donor_id,campaign_id,donation_date,amount,payment_method) VALUES (1,6,?, -500,'UPI')", TODAY); return "saved?!"; });
        t("T05", "Invalid donor", "Donor 999", "Rejected, rollback", "Donor does not exist",
            () -> { DonationService.recordDonation(999, 6, Date.valueOf(TODAY), new BigDecimal("100"), "UPI"); return "saved?!"; });
        t("T06", "Invalid campaign", "Campaign 999", "Rejected, rollback", "Campaign does not exist",
            () -> { DonationService.recordDonation(1, 999, Date.valueOf(TODAY), new BigDecimal("100"), "UPI"); return "saved?!"; });
        t("T07", "Donation to ended campaign", "Campaign 5 (ended 2025-10-31), today", "Rejected by date rule", "ended",
            () -> { DonationService.recordDonation(1, 5, Date.valueOf(TODAY), new BigDecimal("100"), "UPI"); return "saved?!"; });
        t("T08", "FK violation (raw SQL, donor 999)", "INSERT donor_id 999", "Foreign key rejects", "does not exist",
            () -> { exec("INSERT INTO donation (donor_id,campaign_id,donation_date,amount,payment_method) VALUES (999,6,?,100,'UPI')", TODAY); return "saved?!"; });
        t("T09", "Donation form validation (GUI panel)", "amount 'abc'", "Message, no crash", "valid number", () -> {
            CrudPanel p = new CrudPanel(EntityRegistry.donation()); p.refresh();
            p.selectLookup("donor_id", 1); p.selectLookup("campaign_id", 6); p.setText("amount", "abc"); p.doAdd(); return p.getStatus(); });
        t("T10", "Donation recorded through GUI panel", "Donor 2, Campaign 6, 2500", "Saved successfully", "saved successfully", () -> {
            CrudPanel p = new CrudPanel(EntityRegistry.donation()); p.refresh();
            p.selectLookup("donor_id", 2); p.selectLookup("campaign_id", 6); p.setText("amount", "2500"); p.doAdd(); return p.getStatus(); });

        // ---------------- request / approval / distribution
        t("T11", "Valid beneficiary request (GUI)", "Beneficiary 16, Programme 1, 6000", "Request saved as Pending", "saved successfully", () -> {
            CrudPanel p = new CrudPanel(EntityRegistry.request()); p.refresh();
            p.selectLookup("beneficiary_id", 16); p.selectLookup("programme_id", 1);
            p.setText("description", "Exam fees"); p.setText("requested_amount", "6000"); p.doAdd(); return p.getStatus(); });
        final long rid = count("SELECT MAX(request_id) FROM request");
        t("T12", "New request status", "request #" + rid, "Pending", "pending", () -> GenericDao.query("SELECT status FROM request WHERE request_id=?", rid).rows.get(0)[0].toString());
        t("T13", "Invalid request: inactive programme", "Programme 7 (Completed)", "Rejected by trigger", "active programme", () -> {
            CrudPanel p = new CrudPanel(EntityRegistry.request()); p.refresh();
            p.selectLookup("beneficiary_id", 16); p.selectLookup("programme_id", 7);
            p.setText("description", "x"); p.setText("requested_amount", "100"); p.doAdd(); return p.getStatus(); });
        t("T14", "Invalid request: missing beneficiary (raw SQL)", "beneficiary 999", "Foreign key rejects", "does not exist",
            () -> { exec("INSERT INTO request (beneficiary_id,programme_id,request_date,description,requested_amount) VALUES (999,1,?,'x',100)", TODAY); return "saved?!"; });
        t("T15", "Request must start Pending", "status Approved on insert", "Rejected by trigger", "must start with status pending",
            () -> { exec("INSERT INTO request (beneficiary_id,programme_id,request_date,description,requested_amount,status) VALUES (16,1,?,'x',100,'Approved')", TODAY); return "saved?!"; });
        t("T16", "Approve request (GUI Approvals)", "Request #" + rid + ", approve 5000", "Approval saved, request becomes Approved", "approved", () -> {
            CrudPanel p = new CrudPanel(EntityRegistry.approval()); p.refresh();
            p.selectLookup("request_id", (int) rid); p.setText("approved_amount", "5000"); p.setChoice("status", "Approved"); p.doAdd();
            return p.getStatus() + " | request status = " + GenericDao.query("SELECT status FROM request WHERE request_id=?", rid).rows.get(0)[0]; });
        t("T17", "Approve an already approved request", "Request 1", "Rejected: only Pending", "only pending",
            () -> { AssistanceService.decide(1, "Approved", new BigDecimal("100"), Date.valueOf(TODAY)); return "saved?!"; });
        t("T18", "Approved amount above requested", "Request 9: approve 99999 > 14000", "Rejected", "cannot exceed the requested",
            () -> { AssistanceService.decide(9, "Approved", new BigDecimal("99999"), Date.valueOf(TODAY)); return "saved?!"; });
        t("T19", "Approval is immutable (raw UPDATE)", "UPDATE approval", "Rejected by trigger", "cannot be edited",
            () -> { exec("UPDATE approval SET approved_amount = 1 WHERE approval_id = 1"); return "saved?!"; });
        t("T20", "Partial distribution (GUI)", "Request #" + rid + ", 2000 of 5000", "Saved; request stays Approved", "approved", () -> {
            CrudPanel p = new CrudPanel(EntityRegistry.distribution()); p.refresh();
            p.selectLookup("request_id", (int) rid); p.setText("amount_or_quantity", "2000"); p.setChoice("distribution_type", "Education"); p.doAdd();
            return p.getStatus() + " | request status = " + GenericDao.query("SELECT status FROM request WHERE request_id=?", rid).rows.get(0)[0]; });
        t("T21", "Distribution greater than approved", "Request #" + rid + ", 4000 (remaining 3000)", "Rejected, nothing saved", "exceeds the remaining approved", () -> {
            long before = count("SELECT COUNT(*) FROM distribution");
            try { AssistanceService.distribute((int) rid, Date.valueOf(TODAY), new BigDecimal("4000"), "Cash"); return "saved?!"; }
            catch (Exception e) { return ErrorHandler.message(e) + " [rows " + before + " -> " + count("SELECT COUNT(*) FROM distribution") + "]"; } });
        t("T22", "Over-distribution bypassing the app (raw SQL)", "INSERT 4000 directly", "Trigger blocks it", "exceeds the approved amount",
            () -> { exec("INSERT INTO distribution (request_id,distribution_date,amount_or_quantity,distribution_type) VALUES (?,?,4000,'Cash')", rid, TODAY); return "saved?!"; });
        t("T23", "Negative distribution", "Amount -1", "Rejected", "greater than zero",
            () -> { AssistanceService.distribute((int) rid, Date.valueOf(TODAY), new BigDecimal("-1"), "Cash"); return "saved?!"; });
        t("T24", "Distribution on Pending request", "Request 9 (Pending)", "Rejected", "needs an approved request",
            () -> { AssistanceService.distribute(9, Date.valueOf(TODAY), new BigDecimal("100"), "Cash"); return "saved?!"; });
        t("T25", "Final distribution completes request", "Request #" + rid + ", remaining 3000", "Status becomes Fulfilled", "fulfilled", () -> {
            AssistanceService.distribute((int) rid, Date.valueOf(TODAY), new BigDecimal("3000"), "Education");
            return GenericDao.query("SELECT status FROM request WHERE request_id=?", rid).rows.get(0)[0].toString(); });

        // ---------------- transactions (workflow)
        t("T26", "Workflow COMMIT", "Beneficiary 16, Programme 2, ask 4000 / approve 4000 / give 4000", "All 3 rows saved", "committed", () -> {
            long r0 = count("SELECT COUNT(*) FROM request"), a0 = count("SELECT COUNT(*) FROM approval"), d0 = count("SELECT COUNT(*) FROM distribution");
            WorkflowPanel w = new WorkflowPanel(); w.refresh(); w.set(16, 2, "Medicines", "4000", "4000", "4000"); w.run();
            return w.lastMessage() + " rows +" + (count("SELECT COUNT(*) FROM request") - r0) + "/+" + (count("SELECT COUNT(*) FROM approval") - a0) + "/+" + (count("SELECT COUNT(*) FROM distribution") - d0); });
        t("T27", "Workflow ROLLBACK", "ask 4000 / approve 4000 / distribute 9000", "Everything rolled back (0 new rows)", "rolled back", () -> {
            long r0 = count("SELECT COUNT(*) FROM request"), a0 = count("SELECT COUNT(*) FROM approval"), d0 = count("SELECT COUNT(*) FROM distribution");
            WorkflowPanel w = new WorkflowPanel(); w.refresh(); w.set(16, 2, "Rollback demo", "4000", "4000", "9000"); w.run();
            return w.lastMessage() + " rows +" + (count("SELECT COUNT(*) FROM request") - r0) + "/+" + (count("SELECT COUNT(*) FROM approval") - a0) + "/+" + (count("SELECT COUNT(*) FROM distribution") - d0); });

        // ---------------- volunteers / assignments
        t("T28", "Valid volunteer assignment (GUI)", "Volunteer 16, Skill 1, Programme 1, 2026-11-01..2026-12-31", "Saved", "saved successfully", () -> {
            CrudPanel p = new CrudPanel(EntityRegistry.assignment()); p.refresh();
            p.selectLookup("volunteer_id", 16); p.selectLookup("skill_id", 1); p.selectLookup("programme_id", 1);
            p.setText("role", "Tutor"); p.setText("assignment_date", "2026-11-01"); p.setText("end_date", "2026-12-31"); p.doAdd(); return p.getStatus(); });
        t("T29", "Overlapping assignment (service)", "Volunteer 16 again, 2026-12-01..2027-01-15", "Rejected", "overlapping assignment",
            () -> { AssignmentService.assign(16, 2, 1, Date.valueOf("2026-12-01"), Date.valueOf("2027-01-15"), "Overlap"); return "saved?!"; });
        t("T30", "Overlapping assignment (trigger, raw SQL)", "INSERT overlapping row", "Trigger blocks it", "overlapping assignment",
            () -> { exec("INSERT INTO assignment (volunteer_id,skill_id,programme_id,assignment_date,end_date,role) VALUES (16,2,1,'2026-12-01','2027-01-15','x')"); return "saved?!"; });
        t("T31", "Adjacent (non-overlapping) assignment", "Volunteer 16, 2027-01-01..2027-03-31", "Saved", "assignment #",
            () -> "assignment #" + AssignmentService.assign(16, 2, 1, Date.valueOf("2027-01-01"), Date.valueOf("2027-03-31"), "Follow-up"));
        t("T32", "End date before start", "2026-05-10..2026-05-01", "Rejected", "before",
            () -> { AssignmentService.assign(15, 2, 1, Date.valueOf("2026-05-10"), Date.valueOf("2026-05-01"), "x"); return "saved?!"; });

        // ---------------- expenses
        t("T33", "Valid expense (GUI)", "Programme 3, Campaign 6, 7500 Food", "Saved", "saved successfully", () -> {
            CrudPanel p = new CrudPanel(EntityRegistry.expense()); p.refresh();
            p.selectLookup("programme_id", 3); p.selectLookup("campaign_id", 6); p.setText("amount", "7500"); p.setText("description", "Vegetables"); p.setChoice("category", "Food"); p.doAdd(); return p.getStatus(); });
        t("T34", "Negative expense (GUI)", "-100", "Rejected by validation", "greater than zero", () -> {
            CrudPanel p = new CrudPanel(EntityRegistry.expense()); p.refresh();
            p.selectLookup("programme_id", 3); p.setText("amount", "-100"); p.setText("description", "x"); p.doAdd(); return p.getStatus(); });
        t("T35", "Invalid expense date (GUI)", "2026-13-45", "Rejected by validation", "valid date", () -> {
            CrudPanel p = new CrudPanel(EntityRegistry.expense()); p.refresh();
            p.selectLookup("programme_id", 3); p.setText("amount", "100"); p.setText("description", "x"); p.setText("expense_date", "2026-13-45"); p.doAdd(); return p.getStatus(); });
        t("T36", "Future expense date (trigger)", "2099-01-01", "Rejected", "future",
            () -> { exec("INSERT INTO expense (programme_id,expense_date,amount,description,category) VALUES (3,'2099-01-01',10,'x','Food')"); return "saved?!"; });
        t("T37", "Expense with campaign of another programme", "Programme 1 + Campaign 6", "Composite FK rejects", "does not exist",
            () -> { exec("INSERT INTO expense (programme_id,campaign_id,expense_date,amount,description,category) VALUES (1,6,?,10,'x','Food')", TODAY); return "saved?!"; });
        t("T38", "Expense for missing programme", "Programme 999", "Foreign key rejects", "does not exist",
            () -> { exec("INSERT INTO expense (programme_id,expense_date,amount,description,category) VALUES (999,?,10,'x','Food')", TODAY); return "saved?!"; });

        // ---------------- validation / duplicates / injection
        t("T39", "Invalid e-mail (GUI)", "abc@", "Rejected", "not a valid e-mail", () -> {
            CrudPanel p = new CrudPanel(EntityRegistry.donor()); p.refresh();
            p.setText("donor_name", "Test"); p.setText("email", "abc@"); p.setText("phone", "9000000001"); p.doAdd(); return p.getStatus(); });
        t("T40", "Invalid phone (GUI)", "12ab", "Rejected", "10 to 15 digits", () -> {
            CrudPanel p = new CrudPanel(EntityRegistry.donor()); p.refresh();
            p.setText("donor_name", "Test"); p.setText("email", "t@example.com"); p.setText("phone", "12ab"); p.doAdd(); return p.getStatus(); });
        t("T41", "Missing required field (GUI)", "empty donor name", "Rejected", "is required", () -> {
            CrudPanel p = new CrudPanel(EntityRegistry.donor()); p.refresh();
            p.setText("email", "t@example.com"); p.setText("phone", "9000000002"); p.doAdd(); return p.getStatus(); });
        t("T42", "Duplicate donor (GUI)", "existing e-mail ananya.sharma@gmail.com", "Duplicate message", "duplicate record", () -> {
            CrudPanel p = new CrudPanel(EntityRegistry.donor()); p.refresh();
            p.setText("donor_name", "Copy"); p.setText("email", "ananya.sharma@gmail.com"); p.setText("phone", "9000000003"); p.doAdd(); return p.getStatus(); });
        t("T43", "Valid donor CRUD cycle", "add, update, delete", "All succeed", "deleted", () -> {
            CrudPanel p = new CrudPanel(EntityRegistry.donor()); p.refresh();
            p.setText("donor_name", "Cycle Donor"); p.setText("email", "cycle@example.com"); p.setText("phone", "9000000004"); p.doAdd();
            p.search("Cycle"); p.selectRowById((int) count("SELECT donor_id FROM donor WHERE email='cycle@example.com'"));
            p.setText("donor_name", "Cycle Donor Updated"); p.doUpdate();
            String upd = count("SELECT COUNT(*) FROM donor WHERE donor_name='Cycle Donor Updated'") == 1 ? "updated" : "update failed";
            p.selectRowById((int) count("SELECT donor_id FROM donor WHERE email='cycle@example.com'")); p.doDelete(); return upd + ", " + p.getStatus(); });
        t("T44", "Delete referenced programme", "Programme 1 (has campaigns)", "Friendly FK message", "other records depend", () -> {
            CrudPanel p = new CrudPanel(EntityRegistry.programme()); p.refresh(); p.selectRowById(1); p.doDelete(); return p.getStatus(); });
        t("T45", "SQL injection attempt in search", "'; DROP TABLE donor; --", "Treated as text: 0 rows, table intact", "0 rows, donor table intact", () -> {
            CrudPanel p = new CrudPanel(EntityRegistry.donor()); p.refresh(); p.search("'; DROP TABLE donor; --");
            return p.getRowCount() + " rows, donor table " + (count("SELECT COUNT(*) FROM donor") > 0 ? "intact" : "GONE"); });
        t("T46", "Search works", "'Hope'", "Matching donors only", "1 rows", () -> {
            CrudPanel p = new CrudPanel(EntityRegistry.donor()); p.refresh(); p.search("Hope Trust"); return p.getRowCount() + " rows"; });

        // ---------------- reports + dashboard
        for (String name : ReportDao.REPORTS.keySet())
            t("T47", "Report: " + name, "Run report", "Rows from live SQL", "rows", () -> {
                ReportsPanel rp = new ReportsPanel(); rp.select(name); int n = rp.run(); return n + " rows"; });
        t("T48", "Fund report equals raw SQL", "SUM(Funds Received) vs SUM(donation)", "Values match", "match", () -> {
            Object a = GenericDao.query("SELECT SUM(funds_received) FROM v_campaign_fund_utilization").rows.get(0)[0];
            Object b = GenericDao.query("SELECT SUM(amount) FROM donation").rows.get(0)[0];
            return (((BigDecimal) a).compareTo((BigDecimal) b) == 0 ? "match " : "MISMATCH ") + a + " = " + b; });
        t("T49", "Dashboard", "10 summary cards", "10 cards from SQL", "10 cards", () -> {
            DashboardPanel d = new DashboardPanel(); d.refresh(); return d.cardCount() + " cards"; });
        t("T50", "All CRUD screens load data", "refresh every module", "Every screen lists rows", "all 13", () -> {
            EntityDef_Loader l = new EntityDef_Loader(); return l.run(); });
        System.out.println("\nTOTAL: " + pass + " passed, " + fail + " failed");
        if (fail > 0) System.exit(1);
    }

    /** Loads every CRUD panel once. */
    static class EntityDef_Loader {
        String run() {
            com.ngo.model.EntityDef[] all = {EntityRegistry.programme(), EntityRegistry.campaign(), EntityRegistry.donor(), EntityRegistry.donation(),
                EntityRegistry.beneficiary(), EntityRegistry.request(), EntityRegistry.approval(), EntityRegistry.distribution(),
                EntityRegistry.volunteer(), EntityRegistry.skill(), EntityRegistry.assignment(), EntityRegistry.expense(), EntityRegistry.impact()};
            int ok = 0; StringBuilder bad = new StringBuilder();
            for (com.ngo.model.EntityDef e : all) { CrudPanel p = new CrudPanel(e); p.refresh(); if (p.getRowCount() > 0) ok++; else bad.append(e.title).append(' '); }
            return ok == all.length ? "all " + ok + " entity screens loaded with data" : "empty: " + bad;
        }
    }
}
