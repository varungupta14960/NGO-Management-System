-- queries.sql : demonstration of SELECT, joins, aggregation, subqueries, views
USE ngo_db;

-- ===== A. BASIC RETRIEVAL =====
-- A1 SELECT / WHERE / ORDER BY
SELECT donation_id, donor_id, amount, donation_date FROM donation WHERE amount >= 100000 ORDER BY amount DESC;
-- A2 DISTINCT
SELECT DISTINCT payment_method FROM donation ORDER BY payment_method;
-- A3 LIKE + ORDER BY
SELECT programme_name, status, start_date FROM programme WHERE status = 'Active' AND (programme_name LIKE '%Water%' OR programme_name LIKE '%Food%') ORDER BY start_date;
-- A4 BETWEEN
SELECT expense_id, category, amount, expense_date FROM expense WHERE expense_date BETWEEN '2026-01-01' AND '2026-06-30' ORDER BY expense_date;

-- ===== B. JOINS =====
-- B1 INNER JOIN: donations with donor and campaign
SELECT n.donation_id, d.donor_name, c.campaign_name, n.amount, n.donation_date
FROM donation n INNER JOIN donor d ON d.donor_id = n.donor_id
                INNER JOIN campaign c ON c.campaign_id = n.campaign_id
ORDER BY n.donation_date DESC LIMIT 10;
-- B2 LEFT JOIN: donors who have never donated
SELECT d.donor_id, d.donor_name FROM donor d LEFT JOIN donation n ON n.donor_id = d.donor_id WHERE n.donation_id IS NULL;
-- B3 LEFT JOIN: volunteers with no assignment
SELECT v.volunteer_id, v.name FROM volunteer v LEFT JOIN assignment a ON a.volunteer_id = v.volunteer_id WHERE a.assignment_id IS NULL;
-- B4 Multi-table join (5 tables): request -> beneficiary -> programme -> approval -> distribution
SELECT r.request_id, b.name AS beneficiary, p.programme_name, r.requested_amount, a.approved_amount,
       IFNULL(SUM(x.amount_or_quantity),0) AS distributed, r.status
FROM request r
JOIN beneficiary b ON b.beneficiary_id = r.beneficiary_id
JOIN programme p ON p.programme_id = r.programme_id
LEFT JOIN approval a ON a.request_id = r.request_id
LEFT JOIN distribution x ON x.request_id = r.request_id
GROUP BY r.request_id, b.name, p.programme_name, r.requested_amount, a.approved_amount, r.status
ORDER BY r.request_id;
-- B5 Volunteers with skill, programme and role
SELECT v.name, s.skill_name, p.programme_name, a.role, a.assignment_date, a.end_date
FROM assignment a JOIN volunteer v ON v.volunteer_id = a.volunteer_id
                  JOIN skill s ON s.skill_id = a.skill_id
                  JOIN programme p ON p.programme_id = a.programme_id
ORDER BY v.name, a.assignment_date;

-- ===== C. AGGREGATION =====
-- C1 COUNT / SUM / AVG / MIN / MAX on donations
SELECT COUNT(*) AS donations, SUM(amount) AS total, AVG(amount) AS average, MIN(amount) AS smallest, MAX(amount) AS largest FROM donation;
-- C2 GROUP BY: funds per campaign
SELECT c.campaign_name, COUNT(*) AS donations, SUM(n.amount) AS received
FROM donation n JOIN campaign c ON c.campaign_id = n.campaign_id GROUP BY c.campaign_id, c.campaign_name ORDER BY received DESC;
-- C3 GROUP BY + HAVING: donors who gave more than 200000
SELECT d.donor_name, SUM(n.amount) AS total FROM donor d JOIN donation n ON n.donor_id = d.donor_id
GROUP BY d.donor_id, d.donor_name HAVING SUM(n.amount) > 200000 ORDER BY total DESC;
-- C4 Expenses per category with HAVING
SELECT category, COUNT(*) AS entries, SUM(amount) AS total FROM expense GROUP BY category HAVING SUM(amount) > 100000 ORDER BY total DESC;
-- C5 Requests per status
SELECT status, COUNT(*) AS requests, SUM(requested_amount) AS asked FROM request GROUP BY status;

-- ===== D. SUBQUERIES =====
-- D1 Donations larger than the average donation (scalar subquery)
SELECT donation_id, donor_id, amount FROM donation WHERE amount > (SELECT AVG(amount) FROM donation) ORDER BY amount DESC;
-- D2 Donors who supported more than one programme (IN + nested)
SELECT donor_name FROM donor WHERE donor_id IN (
    SELECT n.donor_id FROM donation n JOIN campaign c ON c.campaign_id = n.campaign_id
    GROUP BY n.donor_id HAVING COUNT(DISTINCT c.programme_id) > 1);
-- D3 Programmes with no requests (NOT EXISTS)
SELECT programme_name FROM programme p WHERE NOT EXISTS (SELECT 1 FROM request r WHERE r.programme_id = p.programme_id);
-- D4 Approved requests that are not yet fully distributed (correlated subquery)
SELECT r.request_id, r.description, a.approved_amount,
       (SELECT IFNULL(SUM(x.amount_or_quantity),0) FROM distribution x WHERE x.request_id = r.request_id) AS distributed
FROM request r JOIN approval a ON a.request_id = r.request_id AND a.status = 'Approved'
WHERE a.approved_amount > (SELECT IFNULL(SUM(x.amount_or_quantity),0) FROM distribution x WHERE x.request_id = r.request_id);
-- D5 Programme with the highest total expenses (subquery in HAVING)
SELECT p.programme_name, SUM(e.amount) AS total FROM expense e JOIN programme p ON p.programme_id = e.programme_id
GROUP BY p.programme_id, p.programme_name
HAVING SUM(e.amount) = (SELECT MAX(t) FROM (SELECT SUM(amount) AS t FROM expense GROUP BY programme_id) q);
-- D6 Volunteers with more assignments than the average volunteer
SELECT volunteer_name, assignment_count FROM v_volunteer_activity_summary
WHERE assignment_count > (SELECT AVG(assignment_count) FROM v_volunteer_activity_summary);

-- ===== E. COMPLEX / REPORT QUERIES =====
-- E1 Fund utilization per programme (programme-level money trail)
SELECT * FROM v_programme_fund_position ORDER BY programme_id;
-- E2 Campaigns that reached their fundraising target
SELECT programme_name, campaign_name, target_amount, funds_received, ROUND(funds_received/target_amount*100,1) AS target_reached_pct
FROM v_campaign_fund_utilization WHERE funds_received >= target_amount;
-- E3 Programmes where achievement is below 75% on any indicator
SELECT DISTINCT programme_name FROM v_programme_impact_summary WHERE achievement_pct < 75;
-- E4 Cost per outcome: programme expenses divided by the actual value of its headline indicator
SELECT p.programme_name, (SELECT SUM(e.amount) FROM expense e WHERE e.programme_id = p.programme_id) AS total_expense,
       i.indicator_name, i.actual_value,
       ROUND((SELECT SUM(e.amount) FROM expense e WHERE e.programme_id = p.programme_id) / i.actual_value, 2) AS expense_per_unit
FROM programme p JOIN impact_indicator i ON i.programme_id = p.programme_id
WHERE i.indicator_name IN ('Students supported','Patients treated','Families supported','Women trained','Families relieved','People with clean water access');

-- ===== F. VIEWS =====
SELECT * FROM v_donor_contribution_summary ORDER BY total_contributed DESC;
SELECT * FROM v_campaign_fund_utilization ORDER BY programme_id, campaign_id;
SELECT * FROM v_beneficiary_assistance_summary ORDER BY beneficiary_id;
SELECT * FROM v_volunteer_activity_summary ORDER BY volunteer_id;
SELECT * FROM v_programme_expense_summary ORDER BY programme_id, category;
SELECT * FROM v_programme_impact_summary ORDER BY programme_id;

-- ===== G. INDEX USAGE (EXPLAIN) =====
EXPLAIN SELECT * FROM donation WHERE donor_id = 7;
EXPLAIN SELECT * FROM assignment WHERE volunteer_id = 1 AND assignment_date <= '2026-12-31' AND end_date >= '2026-01-01';
