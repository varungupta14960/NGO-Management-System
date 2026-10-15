# Final Presentation Content (22 slides)

**1. Title** - Design and Implementation of a DBMS for NGO Donation, Beneficiary and Volunteer Management. Varun Manoj Gupta - 25WU0101151, CSE Panthers, School of Technology.

**2. Problem Statement** - Records scattered over spreadsheets/paper; donations and aid hard to trace; low transparency; poor coordination of volunteers and requests; financial and impact data hard to consolidate.

**3. Objectives** - Centralise data; accurate donor/donation records; track requests -> approvals -> distributions; manage volunteers, skills, assignments; record expenses; track impact.

**4. Proposed Solution** - One normalised MySQL database + Java Swing application with validation, transactions and SQL reports.

**5. System Architecture** - Swing UI -> services (transactions, rules) -> DAO (PreparedStatement) -> JDBC -> MySQL (tables, constraints, triggers, views). Layers: ui / service / dao / database / model / util.

**6. Functional Requirements** - Programme and campaign management; donors and donations; beneficiaries, requests, approvals, distributions; volunteers, skills, assignments; expenses and impact; reports.

**7. System Users** - NGO Administrator, Programme Manager, Donation Officer, Beneficiary Coordinator, Volunteer Coordinator, Management User.

**8. ER Diagram** - 13 entities, all relationships 1:M (see normalization.md, Mermaid diagram). Changes from Review 1: distribution.beneficiary_id removed; requested_amount, assignment end_date, expense.campaign_id added.

**9. Normalization / 3NF** - UNF -> 1NF (atomic skills) -> 2NF (single-column keys) -> 3NF (donation_flat split; distribution.beneficiary_id removed). FDs listed per table.

**10. Relational Schema** - The 13 relations with PK / FK / UQ (normalization.md section 5).

**11. Database Constraints** - PK, FK (RESTRICT/CASCADE), UNIQUE (email, phone, names), NOT NULL, CHECK (amount > 0, dates, e-mail/phone regex), ENUM domains, DEFAULTs; 20 triggers + 7 procedures for cross-row rules.

**12. Sample Data** - 8 programmes, 13 campaigns, 18 donors, 36 donations, 16 beneficiaries, 22 requests, 18 approvals, 16 distributions, 16 volunteers, 10 skills, 23 assignments, 26 expenses, 16 indicators.

**13. SQL Queries** - SELECT/WHERE/ORDER BY/DISTINCT; INNER and LEFT joins, 5-table join; COUNT, SUM, AVG, MIN, MAX, GROUP BY, HAVING; IN, NOT EXISTS, scalar and correlated subqueries.

**14. Views and Reports** - 7 views; 7 reports: fund utilisation, donor contributions, beneficiary assistance, volunteer activity, programme expenses, impact, programme fund position.

**15. Java Application** - 16 modules: dashboard, 12 management screens, assistance workflow, reports. Swing + JDBC + MySQL Connector/J.

**16. CRUD Operations** - Add / view / update / delete / search on every master table; read-only approvals (audit); controlled distributions.

**17. Transactions** - setAutoCommit(false) -> commit / rollback in donation, approval, distribution, assignment and the 3-step workflow.

**18. Validation and Error Handling** - Required, e-mail, phone, numeric, positive, date, FK, duplicate checks; SQL errors translated to plain messages; no stack traces.

**19. Testing** - 56 automated checks (valid + invalid cases, rule bypass attempts, rollback, reports, SQL-injection attempt): all passed.

**20. Results** - Working integrated system; every business rule enforced twice (app + database); reports computed live from views.

**21. Conclusion** - Transparent, consistent NGO data in 3NF with enforced integrity and traceable money flow from donation to distribution.

**22. Future Scope** - User login with role-based access (hashed passwords), audit log, document attachments, e-mail receipts, web/mobile front-end, dashboards/charts, multi-currency.
