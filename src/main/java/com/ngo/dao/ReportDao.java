package com.ngo.dao;

import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.Map;

/** The reports. Each one is a real SQL query over the reporting views (see database/views.sql). */
public final class ReportDao {
    public static final Map<String, String> REPORTS = new LinkedHashMap<>();
    static {
        REPORTS.put("1. Fund Utilization",
            "SELECT programme_name AS `Programme`, campaign_name AS `Campaign`, funds_received AS `Funds Received`, "
          + "expenses AS `Expenses`, remaining_funds AS `Remaining Funds`, utilization_pct AS `Utilization %` "
          + "FROM v_campaign_fund_utilization ORDER BY programme_id, campaign_id");
        REPORTS.put("2. Donor Contributions",
            "SELECT v.donor_name AS `Donor`, v.donor_type AS `Type`, v.donation_count AS `No. of Donations`, "
          + "v.total_contributed AS `Total Contributed`, v.campaigns_supported AS `Campaigns Supported`, "
          + "IFNULL((SELECT GROUP_CONCAT(DISTINCT c.campaign_name ORDER BY c.campaign_name SEPARATOR ', ') "
          + "FROM donation n JOIN campaign c ON c.campaign_id = n.campaign_id WHERE n.donor_id = v.donor_id), '-') AS `Campaign Names` "
          + "FROM v_donor_contribution_summary v ORDER BY v.total_contributed DESC");
        REPORTS.put("3. Beneficiary Assistance",
            "SELECT beneficiary_name AS `Beneficiary`, category AS `Category`, total_requests AS `Requests`, "
          + "approved_requests AS `Approved Requests`, approved_amount AS `Approved Amount`, "
          + "distributed_amount AS `Distributed`, distribution_status AS `Distribution Status` "
          + "FROM v_beneficiary_assistance_summary ORDER BY beneficiary_id");
        REPORTS.put("4. Volunteer Activity",
            "SELECT volunteer_name AS `Volunteer`, availability AS `Availability`, skills_used AS `Skills`, "
          + "assignment_count AS `Assignments`, programmes_involved AS `Programmes Involved`, roles AS `Roles` "
          + "FROM v_volunteer_activity_summary ORDER BY volunteer_id");
        REPORTS.put("5. Programme Expenses",
            "SELECT programme_name AS `Programme`, category AS `Expense Category`, expense_count AS `Entries`, "
          + "total_expenses AS `Total Expenses`, first_expense_date AS `From`, last_expense_date AS `To` "
          + "FROM v_programme_expense_summary ORDER BY programme_id, category");
        REPORTS.put("6. Impact Indicators",
            "SELECT programme_name AS `Programme`, indicator_name AS `Indicator`, target_value AS `Target`, "
          + "actual_value AS `Actual`, achievement_pct AS `Achievement %`, measurement_date AS `Measured On` "
          + "FROM v_programme_impact_summary ORDER BY programme_id, indicator_name");
        REPORTS.put("7. Programme Fund Position",
            "SELECT programme_name AS `Programme`, funds_received AS `Funds Received`, expenses AS `Expenses`, "
          + "distributed_aid AS `Aid Distributed`, available_balance AS `Available Balance` "
          + "FROM v_programme_fund_position ORDER BY programme_id");
    }

    private ReportDao() { }

    public static ResultTable run(String name) throws SQLException {
        return GenericDao.query(REPORTS.get(name));
    }
}
