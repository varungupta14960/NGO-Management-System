# Demonstration Flow (8-10 minutes)

Preparation: MySQL running, scripts loaded (`database.sql`, `sample_data.sql`, `views.sql`), app started. Keep MySQL Workbench/CLI open with `USE ngo_db;`.

| Min | Step | What to do / say | DBMS concept shown |
|---|---|---|---|
| 0:00 | 1. Start application | `java -jar target/ngo-management-system.jar`; "Java talks to MySQL through JDBC; settings in db.properties" | JDBC, central connection class |
| 0:30 | 2. Dashboard | Show the 10 cards. "Every number is a SQL COUNT/SUM, nothing is hard-coded." | Aggregation |
| 1:00 | 3. Add donor | Donors -> add *Meera Krishnan*, `meera.k@example.com`, `9000011111`. Then repeat with a bad e-mail `abc@` and with an existing e-mail | Validation, UNIQUE / CHECK, duplicate handling |
| 2:00 | 4. Record donation | Donations -> donor Meera, campaign *Community Kitchen*, 5000. Try amount `-500` | Transaction (validate donor, campaign, insert, commit), CHECK amount > 0 |
| 2:45 | 5. Show it in the database | Workbench: `SELECT * FROM donation ORDER BY donation_id DESC LIMIT 3;` | Persistence |
| 3:15 | 6. Create request | Requests -> beneficiary *Geeta Kumari*, programme *Rural Education Support*, "Exam fees", 6000 (status Pending) | FK to beneficiary + programme, trigger: Active programme only |
| 3:50 | 7. Approve | Approvals -> pick the pending request, approve 5000 -> Requests screen now shows **Approved** | Trigger keeps REQUEST.status consistent; immutable audit rows |
| 4:30 | 8. Distribute | Distributions -> that request, 2000 Education -> saved, status still Approved | Business rule: sum of distributions |
| 5:00 | 9. Invalid distribution | Same request, 4000 -> red message "exceeds remaining approved amount (remaining 3000)". Then in Workbench run the raw `INSERT INTO distribution ... 4000` -> the **trigger** rejects it too | App validation + DB trigger (defence in depth) |
| 5:45 | 10. Transaction rollback | Assistance Workflow: ask 4000, approve 4000, distribute 9000 -> "ROLLED BACK ... nothing saved". Check `SELECT COUNT(*) FROM request;` unchanged | setAutoCommit(false), commit, rollback |
| 6:30 | 11. Volunteer + assignment | Assignments -> volunteer *Jayanth Reddy*, skill Teaching, programme Rural Education, 2026-11-01..2026-12-31 saved; repeat for 2026-12-01..2027-01-15 -> overlap message | Overlap trigger + transaction with row lock |
| 7:15 | 12. Expense | Expenses -> programme Food Security, campaign Community Kitchen, 7500; try campaign *Digital Classrooms* with that programme (rejected) | Composite FK |
| 7:45 | 13. Reports | Reports -> Fund Utilization, Donor Contributions, Impact Indicators | Views, joins, aggregation |
| 8:30 | 14. SQL & structure | Workbench: `SELECT * FROM v_campaign_fund_utilization;`, `SHOW CREATE TABLE expense;`, `SHOW TRIGGERS;`, `EXPLAIN SELECT * FROM donation WHERE donor_id=7;` | Views, constraints, triggers, indexes |
| 9:30 | Close | Mention 3NF fix (distribution.beneficiary_id removed) and 56/56 passed tests | Normalization, testing |

Tip: if time is short, skip steps 3-bad-input details and 12.
