# NGO Donation, Beneficiary and Volunteer Management System

Design and Implementation of a Database Management System (MySQL + Java Swing/JDBC).
Student: Varun Manoj Gupta (25WU0101151), CSE Panthers, School of Technology.

## 1. Project overview
A centralised relational database and desktop application that manages an NGO's **programmes, campaigns, donors, donations,
beneficiaries, assistance requests, approvals, distributions, volunteers, skills, assignments, expenses and impact indicators**,
with business rules enforced by the database (constraints + triggers) and reports produced by SQL views.

## 2. Features
* 13 normalised tables (3NF), PK/FK/UNIQUE/NOT NULL/CHECK/DEFAULT constraints, 20 triggers + 7 stored procedures for cross-row rules
* 7 reporting views, 30+ demonstration queries (joins, aggregation, subqueries)
* Java Swing GUI: dashboard, 12 CRUD modules with search, assistance workflow, 7 live reports
* JDBC transactions with commit / rollback, PreparedStatements only, full input validation, friendly error messages
* 56 automated end-to-end tests, realistic sample data

## 3. Technologies
MySQL 8.0+ (SQL, InnoDB) - Java 17+ - Swing - JDBC - MySQL Connector/J 8.3 - Maven 3.8+ (optional).

## 4. Database requirements
MySQL Server 8.0.16 or newer (needed for enforced CHECK constraints). A MySQL user that may create databases (e.g. `root`).
MariaDB 10.5+ also works.

## 5. Database setup
Open a terminal in the project folder (`NGOManagementSystem/`). Run **in this order**:
```
mysql -u root -p < database/database.sql        -- creates database ngo_db, tables, constraints, indexes, triggers
mysql -u root -p < database/sample_data.sql     -- realistic sample data
mysql -u root -p < database/views.sql           -- reporting views
mysql -u root -p ngo_db < database/queries.sql  -- (optional) demo queries
```
(MySQL Workbench: File -> Open SQL Script -> run each file with the lightning button, same order.)
`database.sql` **drops and re-creates `ngo_db`**, so running it again resets everything.

## 6. Sample data
Loaded by `sample_data.sql` (through the real triggers, so it also proves the rules): 8 programmes, 13 campaigns, 18 donors, 36 donations,
16 beneficiaries, 22 requests, 18 approvals, 16 distributions, 16 volunteers, 10 skills, 23 assignments, 26 expenses, 16 impact indicators.
Request statuses cover Pending, Approved, Rejected, Fulfilled and Cancelled.

## 7. MySQL configuration
Make sure the MySQL service is running (default port 3306). No other server setting is required.

## 8. JDBC configuration
All settings are in one place: `src/main/resources/db.properties`
```
db.url=jdbc:mysql://localhost:3306/ngo_db?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC
db.user=root
db.password=root        <- change to YOUR MySQL password
```
You can also override without rebuilding: put a `db.properties` file next to the jar, or set environment variables
`NGO_DB_URL`, `NGO_DB_USER`, `NGO_DB_PASSWORD`. The class that opens connections is `com.ngo.database.DatabaseConnection`.

## 9. How to run
**Option A - Maven (recommended).** Install JDK 17+ and Maven, then:
```
mvn clean package
java -jar target/ngo-management-system.jar
```
**Option B - IDE (IntelliJ / Eclipse / VS Code).** Open the folder as a Maven project, let it download dependencies, run `com.ngo.Main`.

**Option C - plain javac.** Download `mysql-connector-j-8.3.0.jar`, then
```
javac -d out -cp mysql-connector-j-8.3.0.jar $(find src/main/java -name "*.java")
cp src/main/resources/db.properties out/
java -cp out:mysql-connector-j-8.3.0.jar com.ngo.Main        (use ';' instead of ':' on Windows)
```
If MySQL is not reachable the application shows a dialog with the exact checklist and exits cleanly.

## 10. Application modules
Dashboard - Programmes - Campaigns - Donors - Donations (transactional "Record Donation") - Beneficiaries - Requests (create, update status, search) -
Approvals (approve / reject, read-only audit list) - Distributions (record, search; validated against approved amount) -
**Assistance Workflow** (request + approval + distribution in one transaction) - Volunteers - Skills - Assignments (overlap-safe) -
Expenses - Impact Indicators (target and actual values) - Reports.
Click a table row to load it into the form for Update/Delete; type in the Search box for a LIKE search over the visible text columns.
Dates are typed as `yyyy-MM-dd`.

## 11. Database architecture
```
Swing UI (ui/)  ->  Services (service/: transactions + rules)  ->  DAO (dao/: PreparedStatement)  ->  JDBC  ->  MySQL
                                                                                                   (tables, constraints, triggers, views)
```
Packages: `database` (connection), `model` (entity/field metadata), `dao`, `service`, `ui`, `util` (validation, error translation).
ER diagram, 3NF reasoning, functional dependencies: `documentation/normalization.md`. Data dictionary: `documentation/data_dictionary.md`.

## 12. Normalization
See `documentation/normalization.md`: UNF -> 1NF -> 2NF -> 3NF with the functional dependencies of every relation.
Review-1 correction: `beneficiary_id` removed from DISTRIBUTION (transitive dependency via request). One documented trade-off:
EXPENSE keeps an optional `campaign_id` next to `programme_id`, protected by a composite foreign key (section 6 of that file).

## 13. Business rules
Positive donation/expense/distribution amounts; donor, campaign, beneficiary, programme must exist; only Pending requests can be decided;
approved amount <= requested; distribution only for Approved requests and never above the approved amount or the programme's funds;
no overlapping volunteer assignments; valid expense/donation dates; campaigns inside programme dates; approvals immutable.
Full table (which rule is enforced where): `documentation/indexing_transactions_rules.md`.

## 14. Reports (Reports module, all from live SQL over views)
1 Fund Utilization (programme, campaign, funds received, expenses, remaining, utilisation %) - 2 Donor Contributions - 3 Beneficiary Assistance -
4 Volunteer Activity - 5 Programme Expenses (by category, with dates) - 6 Impact Indicators (target, actual, achievement %) - 7 Programme Fund Position.

## 15. Testing
Document: `documentation/testing.md` (table of all cases with actual results, 56/56 passed).
Re-run on your machine (after `mvn clean package`, with the database freshly loaded):
```
java -Djava.awt.headless=true -cp target/ngo-management-system.jar:target/test-classes com.ngo.IntegrationTest
```
It prints one result row per test and `TOTAL: 56 passed, 0 failed`. **Reload `database.sql`, `sample_data.sql`, `views.sql` afterwards**,
because the tests insert extra rows.

## 16. Other documents
`documentation/demo_flow.md` (5-10 min demo) - `documentation/viva.md` (viva Q&A) - `documentation/presentation.md` (22 slides) -
`documentation/indexing_transactions_rules.md` (indexes, transactions, rules, validation).

## 17. Project structure
```
NGOManagementSystem/
  pom.xml
  database/ database.sql  sample_data.sql  views.sql  queries.sql
  documentation/ normalization.md  data_dictionary.md  indexing_transactions_rules.md  testing.md  demo_flow.md  viva.md  presentation.md
  src/main/java/com/ngo/ Main.java  database/  model/  dao/  service/  ui/  util/
  src/main/resources/db.properties
  src/test/java/com/ngo/IntegrationTest.java
```

## 18. Troubleshooting
* *Access denied* -> fix `db.user` / `db.password`. * *Unknown database ngo_db* -> run the SQL files. * *CHECK constraints ignored* -> MySQL older than 8.0.16.
* *Time-zone error* -> keep `serverTimezone=UTC` in the URL. * Dates in the demo assume today's date is after the sample data (Sep 2026) and the
  "Community Kitchen" campaign (ends 2026-12-31) is still open; for later dates use a campaign with no end date (e.g. *Cyclone Emergency Kits*).
