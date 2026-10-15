# Indexing, Transactions, Business Rules and Validation

## 1. Indexing strategy
InnoDB automatically indexes primary keys and UNIQUE columns. The extra indexes below were added only for columns that are
**joined or filtered frequently**; low-selectivity or rarely searched columns are not indexed.

| Index | Table(column) | Why it exists |
|---|---|---|
| idx_campaign_programme | campaign(programme_id) | Join PROGRAMME -> CAMPAIGN; fund utilisation per programme; FK lookup |
| idx_donation_donor | donation(donor_id) | Donor contribution report; "donations of donor X" |
| idx_donation_campaign | donation(campaign_id) | Funds received per campaign (SUM by campaign) |
| idx_donation_date | donation(donation_date) | Date-range reports / newest-first listing |
| idx_request_beneficiary | request(beneficiary_id) | Beneficiary assistance report |
| idx_request_programme | request(programme_id) | Requests per programme; programme-funds check in the distribution trigger |
| idx_request_status | request(status) | Dashboard "pending requests", approval/distribution drop-downs |
| idx_approval_request | approval(request_id) | Find approval of a request (every distribution check) |
| idx_distribution_request | distribution(request_id) | SUM of distributions per request (over-distribution rule) |
| idx_assignment_volunteer | assignment(volunteer_id, assignment_date, end_date) | **Composite**: overlap check reads only this index |
| idx_assignment_programme / _skill | assignment(programme_id) / (skill_id) | Joins to programme and skill |
| idx_expense_programme_date | expense(programme_id, expense_date) | Expenses per programme ordered/filtered by date |
| idx_expense_campaign | expense(campaign_id) | Campaign-level expenses; composite FK |
| idx_impact_programme | impact_indicator(programme_id) | Impact per programme |

Check with `EXPLAIN SELECT * FROM donation WHERE donor_id = 7;` (queries.sql section G) - the `key` column shows `idx_donation_donor`.

## 2. Transactions (JDBC)
All use `conn.setAutoCommit(false)` -> work -> `conn.commit()`; any exception -> `conn.rollback()`; `finally` restores auto-commit.

| Service method | Steps in ONE transaction |
|---|---|
| `DonationService.recordDonation` | lock + validate donor -> lock + validate campaign (and its date window) -> insert donation -> commit |
| `AssistanceService.decide` | lock request row -> check Pending, amount <= requested -> insert approval (trigger moves request to Approved/Rejected) -> commit |
| `AssistanceService.distribute` | lock request row -> check Approved -> compute remaining approved amount -> insert distribution -> commit |
| `AssistanceService.fullWorkflow` | insert request -> insert approval -> insert distribution -> commit. If the distribution is invalid, **the request and approval are rolled back too** (tested: T27) |
| `AssignmentService.assign` | lock volunteer row -> check no overlapping period -> insert assignment -> commit (two users cannot double-book the same volunteer concurrently) |

`SELECT ... FOR UPDATE` row locks make the check-then-insert sequence safe under concurrency.

## 3. Business rules and where they are enforced
Defence in depth: the application checks first for friendly messages; the database enforces the rule again, so even raw SQL cannot break it.

| Rule | Declarative (CHECK/FK/UNIQUE) | Trigger / procedure | Application |
|---|---|---|---|
| Donation amount > 0 | `ck_donation_amount` | - | Validator + DonationService |
| Donation references valid donor & campaign | FKs | - | DonationService (lock + exists) |
| Donation date not future, inside campaign period | - | `sp_check_donation` | DonationService |
| Donation cannot be deleted/reduced below distributed funds | - | `sp_check_funds_cover` (`trg_donation_bu/bd`) | error message |
| Request belongs to valid beneficiary & programme | FKs | - | combo boxes |
| New request must be Pending, programme must be Active | - | `trg_request_bi` | - |
| Request status life-cycle (no Pending revert; Approved needs approval record) | - | `trg_request_bu` | - |
| Only Pending requests can be approved/rejected; approved <= requested | `ck_approval_amount` | `trg_approval_bi` | AssistanceService |
| Approvals are permanent audit records | - | `trg_approval_bu/bd` | Approvals screen is read-only |
| Distribution amount > 0 | `ck_distribution_amount` | - | Validator |
| Distribution only for Approved request; sum <= approved amount; total <= programme funds | - | `sp_check_distribution` (`trg_distribution_bi/bu`) | AssistanceService |
| Request becomes Fulfilled when fully distributed | - | `sp_sync_request_status` | - |
| No overlapping volunteer assignment | `ck_assignment_dates` | `sp_check_assignment` (`trg_assignment_bi/bu`) | AssignmentService |
| Expense amount > 0 | `ck_expense_amount` | - | Validator |
| Expense date valid (not future, not before programme start) | - | `sp_check_expense` | Validator (format) |
| Expense belongs to valid programme; campaign belongs to same programme | FK + composite FK | - | combo boxes |
| Campaign inside programme period | `ck_campaign_dates` | `sp_check_campaign` | - |
| E-mail / phone format, unique donor/volunteer e-mail & phone | `ck_*_email`, `ck_*_phone`, `uq_*` | - | Validator + duplicate message |

## 4. Validation and error handling in the application
* `Validator`: required fields, e-mail regex, phone 10-15 digits, numeric/decimal (max 2 decimals), positive amounts, strict `yyyy-MM-dd` dates, max lengths.
* `ErrorHandler`: converts `SQLException` codes to plain English: duplicate (1062), FK parent missing (1452), FK child exists (1451), CHECK violation (3819/4025),
  trigger messages (SQLSTATE 45000 shown as written), connection failure (SQLSTATE 08xxx). Stack traces are never shown (only a one-line log on the console).
* Every database call uses `PreparedStatement`; user text is only ever bound as a parameter (search box tested with `'; DROP TABLE donor; --`, T45).
