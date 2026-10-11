# DBMS Viva - Questions and Short Answers (specific to this project)

**Why MySQL?** Free, widely used RDBMS with InnoDB (transactions, foreign keys), CHECK constraints, triggers, views and a mature JDBC driver.

**Why a relational database?** The data is structured and highly interrelated (donor-donation-campaign-programme...). Relations, keys and SQL joins give integrity, consistency and easy reporting.

**What is normalization / why 3NF?** Organising tables to remove redundancy and anomalies. 3NF: every non-key attribute depends on the key, the whole key and nothing but the key - so each fact is stored once.

**1NF?** Atomic values, no repeating groups, each row identifiable (e.g. volunteer skills are rows in ASSIGNMENT, not a comma list).

**2NF?** 1NF + no partial dependency on part of a composite key. All our tables have single-column primary keys.

**3NF?** 2NF + no transitive dependency. Example: `distribution_id -> request_id -> beneficiary_id`, so `beneficiary_id` was removed from DISTRIBUTION.

**Functional dependencies?** E.g. `donor_id -> donor_name, email, phone, donor_type`; `donation_id -> donor_id, campaign_id, amount, ...`; `request_id -> beneficiary_id, programme_id, status ...` (full list in normalization.md).

**Primary keys?** `<table>_id` auto-increment in each of the 13 tables.

**Foreign keys?** campaign.programme_id; donation.donor_id, campaign_id; request.beneficiary_id, programme_id; approval.request_id; distribution.request_id; assignment.volunteer_id, skill_id, programme_id; expense.programme_id and (campaign_id, programme_id); impact_indicator.programme_id.

**Relationships / why 1:M?** One programme has many campaigns, one donor many donations, one request many approvals/distributions (partial payments)... The FK sits on the "many" side. Volunteer-Skill-Programme is M:N resolved by ASSIGNMENT.

**Why CHECK / UNIQUE / NOT NULL?** CHECK: domain rules (amount > 0, end >= start, phone/e-mail format). UNIQUE: no duplicate donors (e-mail/phone), programme or skill names. NOT NULL: mandatory facts (amount, dates, names).

**What happens if a foreign key does not exist?** The INSERT fails (error 1452); the app shows "selected related record does not exist" and nothing is saved.

**What happens when a donation is negative?** Validator rejects it in Java; if bypassed, `ck_donation_amount` rejects it in MySQL.

**Indexes and why?** On FK/join columns: campaign.programme_id, donation.donor_id / campaign_id / donation_date, request.beneficiary_id / programme_id / status, approval.request_id, distribution.request_id, assignment(volunteer_id, dates), expense(programme_id, date), impact_indicator.programme_id - they speed joins, SUMs per parent and the overlap check.

**Which transactions?** Donation entry; approval; distribution; the request->approval->distribution workflow; volunteer assignment. Each: setAutoCommit(false), work, commit.

**What happens during rollback?** All statements of that transaction are undone; e.g. a distribution larger than approved also removes the just-created request and approval (nothing half-saved).

**Which joins?** INNER JOIN (donation-donor-campaign), LEFT JOIN (donors with no donation, volunteers without assignments), 5-table join request-beneficiary-programme-approval-distribution.

**Where are subqueries?** `amount > (SELECT AVG(amount) ...)`, `donor_id IN (SELECT ... HAVING COUNT(DISTINCT programme)>1)`, `NOT EXISTS` programmes with no request, correlated subquery for undistributed approvals, subquery in HAVING for top-spending programme, derived tables inside the views.

**Views?** v_donor_contribution_summary, v_campaign_fund_utilization, v_beneficiary_assistance_summary, v_volunteer_activity_summary, v_programme_expense_summary, v_programme_impact_summary, v_programme_fund_position. Reports read these views.

**SQL injection prevention?** Only PreparedStatement with `?` parameters; input validated; table/column names come from code constants, never from users.

**Why PreparedStatement?** Pre-compiled, parameters are data not SQL (prevents injection), handles types/quotes, reusable.

**How is overlapping assignment prevented?** Trigger `sp_check_assignment` rejects a row if the same volunteer has `assignment_date <= new_end AND end_date >= new_start`; AssignmentService checks the same inside a transaction with `SELECT ... FOR UPDATE` on the volunteer.

**How is distribution validated?** Request must be Approved; amount > 0; previous distributions + new <= approved amount; total distributed for the programme <= programme funds. Done in AssistanceService (friendly message) and again in trigger `sp_check_distribution`.

**How does Java communicate with MySQL?** Through JDBC: `DriverManager.getConnection(url, user, password)` with MySQL Connector/J; then PreparedStatement/ResultSet.

**What is JDBC?** Java Database Connectivity - the standard Java API (java.sql) for connecting to relational databases and running SQL; the driver translates calls to the DB protocol.

**Why triggers and not only CHECK?** CHECK cannot look at other rows/tables (sums, overlaps, status life-cycle); triggers can.

**Known design trade-off?** EXPENSE stores programme_id and optional campaign_id (a transitive dependency in strict theory); the composite FK guarantees they never disagree. Removing campaign_id gives pure 3NF.
