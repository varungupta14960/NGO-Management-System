package com.ngo.service;

import com.ngo.model.EntityDef;
import com.ngo.model.FieldDef;
import java.math.BigDecimal;
import java.sql.Date;
import java.util.Map;

/** Definition of every managed table (fields, list query, search, special insert logic). */
public final class EntityRegistry {
    private EntityRegistry() { }

    private static final String LK_PROGRAMME = "SELECT programme_id, programme_name FROM programme ORDER BY programme_name";
    private static final String LK_CAMPAIGN = "SELECT c.campaign_id, CONCAT(c.campaign_name, ' [', p.programme_name, ']') FROM campaign c JOIN programme p ON p.programme_id = c.programme_id ORDER BY c.campaign_name";
    private static final String LK_DONOR = "SELECT donor_id, CONCAT(donor_name, ' (#', donor_id, ')') FROM donor ORDER BY donor_name";
    private static final String LK_BENEFICIARY = "SELECT beneficiary_id, CONCAT(name, ' (#', beneficiary_id, ')') FROM beneficiary ORDER BY name";
    private static final String LK_VOLUNTEER = "SELECT volunteer_id, CONCAT(name, ' (#', volunteer_id, ')') FROM volunteer ORDER BY name";
    private static final String LK_SKILL = "SELECT skill_id, skill_name FROM skill ORDER BY skill_name";
    public static final String LK_PENDING_REQUEST = "SELECT r.request_id, CONCAT('#', r.request_id, ' ', b.name, ' - ', r.description, ' (asked ', r.requested_amount, ')') "
            + "FROM request r JOIN beneficiary b ON b.beneficiary_id = r.beneficiary_id WHERE r.status = 'Pending' ORDER BY r.request_id";
    public static final String LK_APPROVED_REQUEST = "SELECT r.request_id, CONCAT('#', r.request_id, ' ', b.name, ' - ', r.description, ' (approved ', a.approved_amount, ')') "
            + "FROM request r JOIN beneficiary b ON b.beneficiary_id = r.beneficiary_id "
            + "JOIN approval a ON a.request_id = r.request_id AND a.status = 'Approved' WHERE r.status = 'Approved' ORDER BY r.request_id";
    public static final String LK_BENEFICIARY_PUBLIC = LK_BENEFICIARY;
    public static final String LK_PROGRAMME_PUBLIC = LK_PROGRAMME;

    public static final String[] DIST_TYPES = {"Cash", "Food", "Medical", "Education", "Shelter", "Clothing", "Equipment"};

    private static int i(Map<String, Object> v, String k) { return (Integer) v.get(k); }
    private static BigDecimal d(Map<String, Object> v, String k) { return (BigDecimal) v.get(k); }
    private static Date dt(Map<String, Object> v, String k) { return (Date) v.get(k); }

    public static EntityDef programme() {
        return new EntityDef("Programme", "programme", "programme_id",
                FieldDef.text("programme_name", "Programme Name", 100),
                FieldDef.text("description", "Description", 500).optional(),
                FieldDef.date("start_date", "Start Date (yyyy-MM-dd)"),
                FieldDef.date("end_date", "End Date (yyyy-MM-dd)").optional(),
                FieldDef.choice("status", "Status", "Planned", "Active", "Completed", "On Hold"))
            .list("SELECT programme_id AS ID, programme_name AS Name, status AS Status, start_date AS Start, end_date AS End, description AS Description FROM programme",
                  "CONCAT_WS(' ', programme_name, status, description)", "programme_id");
    }

    public static EntityDef campaign() {
        return new EntityDef("Campaign", "campaign", "campaign_id",
                FieldDef.lookup("programme_id", "Programme", LK_PROGRAMME),
                FieldDef.text("campaign_name", "Campaign Name", 100),
                FieldDef.text("description", "Description", 500).optional(),
                FieldDef.date("start_date", "Start Date (yyyy-MM-dd)"),
                FieldDef.date("end_date", "End Date (yyyy-MM-dd)").optional(),
                FieldDef.money("target_amount", "Target Amount"))
            .list("SELECT c.campaign_id AS ID, c.campaign_name AS Campaign, p.programme_name AS Programme, c.start_date AS Start, c.end_date AS End, c.target_amount AS Target FROM campaign c JOIN programme p ON p.programme_id = c.programme_id",
                  "CONCAT_WS(' ', c.campaign_name, p.programme_name, c.description)", "c.campaign_id");
    }

    public static EntityDef donor() {
        return new EntityDef("Donor", "donor", "donor_id",
                FieldDef.text("donor_name", "Donor Name", 100),
                FieldDef.email("email", "Email"),
                FieldDef.phone("phone", "Phone (10-15 digits)"),
                FieldDef.choice("donor_type", "Donor Type", "Individual", "Corporate", "Foundation", "Government"))
            .list("SELECT donor_id AS ID, donor_name AS Name, email AS Email, phone AS Phone, donor_type AS Type FROM donor",
                  "CONCAT_WS(' ', donor_name, email, phone, donor_type)", "donor_id");
    }

    public static EntityDef donation() {
        return new EntityDef("Donation", "donation", "donation_id",
                FieldDef.lookup("donor_id", "Donor", LK_DONOR),
                FieldDef.lookup("campaign_id", "Campaign", LK_CAMPAIGN),
                FieldDef.date("donation_date", "Donation Date (yyyy-MM-dd)"),
                FieldDef.money("amount", "Amount"),
                FieldDef.choice("payment_method", "Payment Method", "UPI", "Cash", "Cheque", "Bank Transfer", "Card"))
            .list("SELECT n.donation_id AS ID, d.donor_name AS Donor, c.campaign_name AS Campaign, n.donation_date AS Date, n.amount AS Amount, n.payment_method AS Method FROM donation n JOIN donor d ON d.donor_id = n.donor_id JOIN campaign c ON c.campaign_id = n.campaign_id",
                  "CONCAT_WS(' ', d.donor_name, c.campaign_name, n.payment_method, n.donation_date)", "n.donation_date DESC, n.donation_id DESC")
            .onInsert(v -> DonationService.recordDonation(i(v, "donor_id"), i(v, "campaign_id"), dt(v, "donation_date"), d(v, "amount"), (String) v.get("payment_method")));
    }

    public static EntityDef beneficiary() {
        return new EntityDef("Beneficiary", "beneficiary", "beneficiary_id",
                FieldDef.text("name", "Name", 100),
                FieldDef.phone("contact", "Contact (10-15 digits)"),
                FieldDef.text("address", "Address", 255).optional(),
                FieldDef.choice("category", "Category", "Family", "Child", "Student", "Women", "Elderly", "Disabled", "Other"))
            .list("SELECT beneficiary_id AS ID, name AS Name, contact AS Contact, category AS Category, address AS Address FROM beneficiary",
                  "CONCAT_WS(' ', name, contact, category, address)", "beneficiary_id");
    }

    public static EntityDef request() {
        return new EntityDef("Request", "request", "request_id",
                FieldDef.lookup("beneficiary_id", "Beneficiary", LK_BENEFICIARY),
                FieldDef.lookup("programme_id", "Programme", LK_PROGRAMME),
                FieldDef.date("request_date", "Request Date (yyyy-MM-dd)"),
                FieldDef.text("description", "Description", 500),
                FieldDef.money("requested_amount", "Requested Amount"),
                FieldDef.choice("status", "Status", "Pending", "Approved", "Rejected", "Fulfilled", "Cancelled"))
            .list("SELECT r.request_id AS ID, b.name AS Beneficiary, p.programme_name AS Programme, r.request_date AS Date, r.description AS Description, r.requested_amount AS Requested, r.status AS Status FROM request r JOIN beneficiary b ON b.beneficiary_id = r.beneficiary_id JOIN programme p ON p.programme_id = r.programme_id",
                  "CONCAT_WS(' ', b.name, p.programme_name, r.description, r.status)", "r.request_id DESC");
    }

    public static EntityDef approval() {
        return new EntityDef("Approval", "approval", "approval_id",
                FieldDef.lookup("request_id", "Pending Request", LK_PENDING_REQUEST),
                FieldDef.date("approval_date", "Approval Date (yyyy-MM-dd)"),
                FieldDef.amount0("approved_amount", "Approved Amount (0 if rejecting)").optional(),
                FieldDef.choice("status", "Decision", "Approved", "Rejected"))
            .list("SELECT a.approval_id AS ID, a.request_id AS Request, b.name AS Beneficiary, r.description AS Description, r.requested_amount AS Requested, a.approved_amount AS Approved, a.status AS Decision, a.approval_date AS Date FROM approval a JOIN request r ON r.request_id = a.request_id JOIN beneficiary b ON b.beneficiary_id = r.beneficiary_id",
                  "CONCAT_WS(' ', b.name, r.description, a.status)", "a.approval_id DESC")
            .readOnlyRows()
            .onInsert(v -> AssistanceService.decide(i(v, "request_id"), (String) v.get("status"),
                    v.get("approved_amount") == null ? BigDecimal.ZERO : d(v, "approved_amount"), dt(v, "approval_date")));
    }

    public static EntityDef distribution() {
        return new EntityDef("Distribution", "distribution", "distribution_id",
                FieldDef.lookup("request_id", "Approved Request", LK_APPROVED_REQUEST),
                FieldDef.date("distribution_date", "Distribution Date (yyyy-MM-dd)"),
                FieldDef.money("amount_or_quantity", "Amount / Quantity"),
                FieldDef.choice("distribution_type", "Type", DIST_TYPES))
            .list("SELECT x.distribution_id AS ID, x.request_id AS Request, b.name AS Beneficiary, r.description AS Description, x.distribution_date AS Date, x.amount_or_quantity AS Amount, x.distribution_type AS Type, r.status AS `Request Status` FROM distribution x JOIN request r ON r.request_id = x.request_id JOIN beneficiary b ON b.beneficiary_id = r.beneficiary_id",
                  "CONCAT_WS(' ', b.name, r.description, x.distribution_type)", "x.distribution_id DESC")
            .noUpdate()
            .onInsert(v -> AssistanceService.distribute(i(v, "request_id"), dt(v, "distribution_date"), d(v, "amount_or_quantity"), (String) v.get("distribution_type")));
    }

    public static EntityDef volunteer() {
        return new EntityDef("Volunteer", "volunteer", "volunteer_id",
                FieldDef.text("name", "Name", 100),
                FieldDef.email("email", "Email"),
                FieldDef.phone("phone", "Phone (10-15 digits)"),
                FieldDef.choice("availability", "Availability", "Flexible", "Weekdays", "Weekends", "Evenings", "Full-time"))
            .list("SELECT volunteer_id AS ID, name AS Name, email AS Email, phone AS Phone, availability AS Availability FROM volunteer",
                  "CONCAT_WS(' ', name, email, phone, availability)", "volunteer_id");
    }

    public static EntityDef skill() {
        return new EntityDef("Skill", "skill", "skill_id",
                FieldDef.text("skill_name", "Skill Name", 60),
                FieldDef.text("description", "Description", 255).optional())
            .list("SELECT skill_id AS ID, skill_name AS Skill, description AS Description FROM skill",
                  "CONCAT_WS(' ', skill_name, description)", "skill_id");
    }

    public static EntityDef assignment() {
        return new EntityDef("Assignment", "assignment", "assignment_id",
                FieldDef.lookup("volunteer_id", "Volunteer", LK_VOLUNTEER),
                FieldDef.lookup("skill_id", "Skill", LK_SKILL),
                FieldDef.lookup("programme_id", "Programme", LK_PROGRAMME),
                FieldDef.text("role", "Role", 60),
                FieldDef.date("assignment_date", "Assignment Date (yyyy-MM-dd)"),
                FieldDef.date("end_date", "End Date (yyyy-MM-dd)"))
            .list("SELECT a.assignment_id AS ID, v.name AS Volunteer, s.skill_name AS Skill, p.programme_name AS Programme, a.role AS Role, a.assignment_date AS Start, a.end_date AS End FROM assignment a JOIN volunteer v ON v.volunteer_id = a.volunteer_id JOIN skill s ON s.skill_id = a.skill_id JOIN programme p ON p.programme_id = a.programme_id",
                  "CONCAT_WS(' ', v.name, s.skill_name, p.programme_name, a.role)", "a.assignment_id DESC")
            .onInsert(v -> AssignmentService.assign(i(v, "volunteer_id"), i(v, "skill_id"), i(v, "programme_id"), dt(v, "assignment_date"), dt(v, "end_date"), (String) v.get("role")));
    }

    public static EntityDef expense() {
        return new EntityDef("Expense", "expense", "expense_id",
                FieldDef.lookup("programme_id", "Programme", LK_PROGRAMME),
                FieldDef.lookup("campaign_id", "Campaign (optional, must belong to programme)", LK_CAMPAIGN).optional(),
                FieldDef.date("expense_date", "Expense Date (yyyy-MM-dd)"),
                FieldDef.money("amount", "Amount"),
                FieldDef.text("description", "Description", 255),
                FieldDef.choice("category", "Category", "Supplies", "Food", "Medical", "Education", "Logistics", "Salaries", "Administration", "Events", "Construction"))
            .list("SELECT e.expense_id AS ID, p.programme_name AS Programme, c.campaign_name AS Campaign, e.expense_date AS Date, e.category AS Category, e.amount AS Amount, e.description AS Description FROM expense e JOIN programme p ON p.programme_id = e.programme_id LEFT JOIN campaign c ON c.campaign_id = e.campaign_id",
                  "CONCAT_WS(' ', p.programme_name, c.campaign_name, e.category, e.description)", "e.expense_date DESC, e.expense_id DESC");
    }

    public static EntityDef impact() {
        return new EntityDef("Impact Indicator", "impact_indicator", "impact_id",
                FieldDef.lookup("programme_id", "Programme", LK_PROGRAMME),
                FieldDef.text("indicator_name", "Indicator Name", 100),
                FieldDef.money("target_value", "Target Value"),
                FieldDef.amount0("actual_value", "Actual Value").dflt("0"),
                FieldDef.date("measurement_date", "Measurement Date (yyyy-MM-dd)"))
            .list("SELECT i.impact_id AS ID, p.programme_name AS Programme, i.indicator_name AS Indicator, i.target_value AS Target, i.actual_value AS Actual, ROUND(i.actual_value / i.target_value * 100, 2) AS `Achievement %`, i.measurement_date AS Date FROM impact_indicator i JOIN programme p ON p.programme_id = i.programme_id",
                  "CONCAT_WS(' ', p.programme_name, i.indicator_name)", "i.impact_id");
    }
}
