-- views.sql : reporting views (the application Reports module reads these views)
USE ngo_db;

-- 1. Donor contribution summary
CREATE OR REPLACE VIEW v_donor_contribution_summary AS
SELECT d.donor_id, d.donor_name, d.donor_type,
       COUNT(n.donation_id)                    AS donation_count,
       IFNULL(SUM(n.amount), 0)                AS total_contributed,
       COUNT(DISTINCT n.campaign_id)           AS campaigns_supported,
       MAX(n.donation_date)                    AS last_donation_date
FROM donor d
LEFT JOIN donation n ON n.donor_id = d.donor_id
GROUP BY d.donor_id, d.donor_name, d.donor_type;

-- 2. Campaign fund utilization (received vs expenses charged to the campaign)
CREATE OR REPLACE VIEW v_campaign_fund_utilization AS
SELECT p.programme_id, p.programme_name, c.campaign_id, c.campaign_name, c.target_amount,
       IFNULL(r.received, 0)                                  AS funds_received,
       IFNULL(e.spent, 0)                                     AS expenses,
       IFNULL(r.received, 0) - IFNULL(e.spent, 0)             AS remaining_funds,
       CASE WHEN IFNULL(r.received,0) = 0 THEN 0
            ELSE ROUND(IFNULL(e.spent,0) / r.received * 100, 2) END AS utilization_pct
FROM campaign c
JOIN programme p ON p.programme_id = c.programme_id
LEFT JOIN (SELECT campaign_id, SUM(amount) AS received FROM donation GROUP BY campaign_id) r
       ON r.campaign_id = c.campaign_id
LEFT JOIN (SELECT campaign_id, SUM(amount) AS spent FROM expense
            WHERE campaign_id IS NOT NULL GROUP BY campaign_id) e
       ON e.campaign_id = c.campaign_id;

-- 3. Beneficiary assistance summary
CREATE OR REPLACE VIEW v_beneficiary_assistance_summary AS
SELECT b.beneficiary_id, b.name AS beneficiary_name, b.category,
       IFNULL(rq.total_requests, 0)    AS total_requests,
       IFNULL(rq.approved_requests, 0) AS approved_requests,
       IFNULL(ap.approved_amount, 0)   AS approved_amount,
       IFNULL(ds.distributed, 0)       AS distributed_amount,
       CASE WHEN IFNULL(rq.approved_requests,0) = 0 THEN 'No approved request'
            WHEN IFNULL(ds.distributed,0) = 0 THEN 'Not started'
            WHEN ds.distributed < ap.approved_amount THEN 'Partially distributed'
            ELSE 'Fully distributed' END AS distribution_status
FROM beneficiary b
LEFT JOIN (SELECT beneficiary_id, COUNT(*) AS total_requests,
                  SUM(status IN ('Approved','Fulfilled')) AS approved_requests
             FROM request GROUP BY beneficiary_id) rq ON rq.beneficiary_id = b.beneficiary_id
LEFT JOIN (SELECT r.beneficiary_id, SUM(a.approved_amount) AS approved_amount
             FROM approval a JOIN request r ON r.request_id = a.request_id
            WHERE a.status = 'Approved' GROUP BY r.beneficiary_id) ap ON ap.beneficiary_id = b.beneficiary_id
LEFT JOIN (SELECT r.beneficiary_id, SUM(x.amount_or_quantity) AS distributed
             FROM distribution x JOIN request r ON r.request_id = x.request_id
            GROUP BY r.beneficiary_id) ds ON ds.beneficiary_id = b.beneficiary_id;

-- 4. Volunteer activity summary
CREATE OR REPLACE VIEW v_volunteer_activity_summary AS
SELECT v.volunteer_id, v.name AS volunteer_name, v.availability,
       COUNT(a.assignment_id)                                        AS assignment_count,
       COUNT(DISTINCT a.programme_id)                                AS programmes_involved,
       IFNULL(GROUP_CONCAT(DISTINCT s.skill_name ORDER BY s.skill_name SEPARATOR ', '), '-') AS skills_used,
       IFNULL(GROUP_CONCAT(DISTINCT a.role ORDER BY a.role SEPARATOR ', '), '-')             AS roles
FROM volunteer v
LEFT JOIN assignment a ON a.volunteer_id = v.volunteer_id
LEFT JOIN skill s ON s.skill_id = a.skill_id
GROUP BY v.volunteer_id, v.name, v.availability;

-- 5. Programme expense summary (per programme and category)
CREATE OR REPLACE VIEW v_programme_expense_summary AS
SELECT p.programme_id, p.programme_name, e.category,
       COUNT(*) AS expense_count, SUM(e.amount) AS total_expenses,
       MIN(e.expense_date) AS first_expense_date, MAX(e.expense_date) AS last_expense_date
FROM expense e JOIN programme p ON p.programme_id = e.programme_id
GROUP BY p.programme_id, p.programme_name, e.category;

-- 6. Programme impact summary
CREATE OR REPLACE VIEW v_programme_impact_summary AS
SELECT p.programme_id, p.programme_name, i.indicator_name, i.target_value, i.actual_value,
       ROUND(i.actual_value / i.target_value * 100, 2) AS achievement_pct, i.measurement_date
FROM impact_indicator i JOIN programme p ON p.programme_id = i.programme_id;

-- 7. Programme-level fund position (donations, distributions, expenses)
CREATE OR REPLACE VIEW v_programme_fund_position AS
SELECT p.programme_id, p.programme_name,
       IFNULL(f.funds, 0)  AS funds_received,
       IFNULL(x.spent, 0)  AS expenses,
       IFNULL(d.dist, 0)   AS distributed_aid,
       IFNULL(f.funds,0) - IFNULL(x.spent,0) - IFNULL(d.dist,0) AS available_balance
FROM programme p
LEFT JOIN (SELECT c.programme_id, SUM(n.amount) AS funds FROM donation n
             JOIN campaign c ON c.campaign_id = n.campaign_id GROUP BY c.programme_id) f ON f.programme_id = p.programme_id
LEFT JOIN (SELECT programme_id, SUM(amount) AS spent FROM expense GROUP BY programme_id) x ON x.programme_id = p.programme_id
LEFT JOIN (SELECT r.programme_id, SUM(s.amount_or_quantity) AS dist FROM distribution s
             JOIN request r ON r.request_id = s.request_id GROUP BY r.programme_id) d ON d.programme_id = p.programme_id;
