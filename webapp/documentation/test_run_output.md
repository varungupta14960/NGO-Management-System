| Test Case | Input | Expected Result | Actual Result | Status |
|---|---|---|---|---|
| T01 Java connects to MySQL | DatabaseConnection.testConnection() | Connection succeeds | connected | PASS |
| T02 Valid donation | Donor 17, Campaign 6, 5000, UPI | Donation saved and committed | donation #37 committed; rows 36 -> 37 | PASS |
| T03 Negative donation (service) | Amount -500 | Rejected: amount must be > 0 | Donation amount must be greater than zero. | PASS |
| T04 Negative donation (CHECK constraint, raw SQL) | INSERT amount = -500 | Database CHECK rejects | A value violates a database rule (ck_donation_amount). Check amounts are positive, dates are in order and the e-mail/phone format is valid. | PASS |
| T05 Invalid donor | Donor 999 | Rejected, rollback | Donor does not exist. | PASS |
| T06 Invalid campaign | Campaign 999 | Rejected, rollback | Campaign does not exist. | PASS |
| T07 Donation to ended campaign | Campaign 5 (ended 2025-10-31), today | Rejected by date rule | Campaign ended on 2025-10-31; donations after that date are not accepted. | PASS |
| T08 FK violation (raw SQL, donor 999) | INSERT donor_id 999 | Foreign key rejects | Invalid reference: the selected related record does not exist. | PASS |
| T09 Donation form validation (GUI panel) | amount 'abc' | Message, no crash | Amount must be a valid number. | PASS |
| T10 Donation recorded through GUI panel | Donor 2, Campaign 6, 2500 | Saved successfully | Donation saved successfully. | PASS |
| T11 Valid beneficiary request (GUI) | Beneficiary 16, Programme 1, 6000 | Request saved as Pending | Request saved successfully. | PASS |
| T12 New request status | request #23 | Pending | Pending | PASS |
| T13 Invalid request: inactive programme | Programme 7 (Completed) | Rejected by trigger | Requests can only be raised against an Active programme | PASS |
| T14 Invalid request: missing beneficiary (raw SQL) | beneficiary 999 | Foreign key rejects | Invalid reference: the selected related record does not exist. | PASS |
| T15 Request must start Pending | status Approved on insert | Rejected by trigger | A new request must start with status Pending | PASS |
| T16 Approve request (GUI Approvals) | Request #23, approve 5000 | Approval saved, request becomes Approved | Approval saved successfully. / request status = Approved | PASS |
| T17 Approve an already approved request | Request 1 | Rejected: only Pending | Request #1 is Fulfilled; only Pending requests can be approved or rejected. | PASS |
| T18 Approved amount above requested | Request 9: approve 99999 > 14000 | Rejected | Approved amount cannot exceed the requested amount (14000.00). | PASS |
| T19 Approval is immutable (raw UPDATE) | UPDATE approval | Rejected by trigger | Approval decisions are permanent audit records and cannot be edited | PASS |
| T20 Partial distribution (GUI) | Request #23, 2000 of 5000 | Saved; request stays Approved | Distribution saved successfully. / request status = Approved | PASS |
| T21 Distribution greater than approved | Request #23, 4000 (remaining 3000) | Rejected, nothing saved | Distribution of 4000 exceeds the remaining approved amount. Approved: 5000.00, already distributed: 2000.00, remaining: 3000.00. [rows 17 -> 17] | PASS |
| T22 Over-distribution bypassing the app (raw SQL) | INSERT 4000 directly | Trigger blocks it | Distribution exceeds the approved amount for this request | PASS |
| T23 Negative distribution | Amount -1 | Rejected | Distribution amount must be greater than zero. | PASS |
| T24 Distribution on Pending request | Request 9 (Pending) | Rejected | Request #9 is Pending; distribution needs an Approved request. | PASS |
| T25 Final distribution completes request | Request #23, remaining 3000 | Status becomes Fulfilled | Fulfilled | PASS |
| T26 Workflow COMMIT | Beneficiary 16, Programme 2, ask 4000 / approve 4000 / give 4000 | All 3 rows saved | COMMITTED: request #25 created, approved and distributed. rows +1/+1/+1 | PASS |
| T27 Workflow ROLLBACK | ask 4000 / approve 4000 / distribute 9000 | Everything rolled back (0 new rows) | ROLLED BACK: Distribution of 9000 exceeds the remaining approved amount. Approved: 4000.00, already distributed: 0.00, remaining: 4000.00. (nothing was saved) rows +0/+0/+0 | PASS |
| T28 Valid volunteer assignment (GUI) | Volunteer 16, Skill 1, Programme 1, 2026-11-01..2026-12-31 | Saved | Assignment saved successfully. | PASS |
| T29 Overlapping assignment (service) | Volunteer 16 again, 2026-12-01..2027-01-15 | Rejected | Overlapping assignment: volunteer is already assigned to 'Rural Education Support' from 2026-11-01 to 2026-12-31. | PASS |
| T30 Overlapping assignment (trigger, raw SQL) | INSERT overlapping row | Trigger blocks it | Overlapping assignment: volunteer is already assigned during this period | PASS |
| T31 Adjacent (non-overlapping) assignment | Volunteer 16, 2027-01-01..2027-03-31 | Saved | assignment #25 | PASS |
| T32 End date before start | 2026-05-10..2026-05-01 | Rejected | End date cannot be before the assignment date. | PASS |
| T33 Valid expense (GUI) | Programme 3, Campaign 6, 7500 Food | Saved | Expense saved successfully. | PASS |
| T34 Negative expense (GUI) | -100 | Rejected by validation | Amount must be greater than zero. | PASS |
| T35 Invalid expense date (GUI) | 2026-13-45 | Rejected by validation | Expense Date (yyyy-MM-dd) must be a valid date in yyyy-MM-dd format. | PASS |
| T36 Future expense date (trigger) | 2099-01-01 | Rejected | Expense date cannot be in the future | PASS |
| T37 Expense with campaign of another programme | Programme 1 + Campaign 6 | Composite FK rejects | Invalid reference: the selected related record does not exist. | PASS |
| T38 Expense for missing programme | Programme 999 | Foreign key rejects | Invalid reference: the selected related record does not exist. | PASS |
| T39 Invalid e-mail (GUI) | abc@ | Rejected | Email is not a valid e-mail address (e.g. name@example.com). | PASS |
| T40 Invalid phone (GUI) | 12ab | Rejected | Phone (10-15 digits) must contain 10 to 15 digits only. | PASS |
| T41 Missing required field (GUI) | empty donor name | Rejected | Donor Name is required. | PASS |
| T42 Duplicate donor (GUI) | existing e-mail ananya.sharma@gmail.com | Duplicate message | Duplicate record: a record with the same unique value (name, e-mail, phone, ...) already exists. | PASS |
| T43 Valid donor CRUD cycle | add, update, delete | All succeed | updated, Donor #20 deleted. | PASS |
| T44 Delete referenced programme | Programme 1 (has campaigns) | Friendly FK message | This record cannot be deleted/changed because other records depend on it. | PASS |
| T45 SQL injection attempt in search | '; DROP TABLE donor; -- | Treated as text: 0 rows, table intact | 0 rows, donor table intact | PASS |
| T46 Search works | 'Hope' | Matching donors only | 1 rows | PASS |
| T47 Report: 1. Fund Utilization | Run report | Rows from live SQL | 13 rows | PASS |
| T47 Report: 2. Donor Contributions | Run report | Rows from live SQL | 18 rows | PASS |
| T47 Report: 3. Beneficiary Assistance | Run report | Rows from live SQL | 16 rows | PASS |
| T47 Report: 4. Volunteer Activity | Run report | Rows from live SQL | 16 rows | PASS |
| T47 Report: 5. Programme Expenses | Run report | Rows from live SQL | 23 rows | PASS |
| T47 Report: 6. Impact Indicators | Run report | Rows from live SQL | 16 rows | PASS |
| T47 Report: 7. Programme Fund Position | Run report | Rows from live SQL | 8 rows | PASS |
| T48 Fund report equals raw SQL | SUM(Funds Received) vs SUM(donation) | Values match | match 2997000.00 = 2997000.00 | PASS |
| T49 Dashboard | 10 summary cards | 10 cards from SQL | 10 cards | PASS |
| T50 All CRUD screens load data | refresh every module | Every screen lists rows | all 13 entity screens loaded with data | PASS |

TOTAL: 56 passed, 0 failed
