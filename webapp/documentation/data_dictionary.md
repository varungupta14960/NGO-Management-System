# Data Dictionary

Generated from the live `ngo_db` schema (information_schema), so it always matches `database.sql`. PK = primary key, FK = foreign key, UQ = unique.


## PROGRAMME

| Table | Attribute | Data type | Size | Key | NULL? | Description |
|---|---|---|---|---|---|---|
| programme | programme_id | INT | 11 | PK | NOT NULL | Unique programme identifier (auto-generated) |
| programme | programme_name | VARCHAR | 100 | UQ | NOT NULL | Name of the programme (unique) |
| programme | description | VARCHAR | 500 | - | NULL | Free-text description |
| programme | start_date | DATE | - | - | NOT NULL | Start date |
| programme | end_date | DATE | - | - | NULL | End date (NULL = open-ended) |
| programme | status | ENUM | Planned / Active / Completed / On Hold | - | NOT NULL | Planned/Active/Completed/On Hold |

## CAMPAIGN

| Table | Attribute | Data type | Size | Key | NULL? | Description |
|---|---|---|---|---|---|---|
| campaign | campaign_id | INT | 11 | PK | NOT NULL | Unique campaign identifier |
| campaign | programme_id | INT | 11 | FK | NOT NULL | Programme the campaign belongs to |
| campaign | campaign_name | VARCHAR | 100 | - | NOT NULL | Campaign name (unique within programme) |
| campaign | description | VARCHAR | 500 | - | NULL | Campaign description |
| campaign | start_date | DATE | - | - | NOT NULL | Campaign start date |
| campaign | end_date | DATE | - | - | NULL | Campaign end date (NULL = open) |
| campaign | target_amount | DECIMAL | 12,2 | - | NOT NULL | Fundraising target in INR (> 0) |

## DONOR

| Table | Attribute | Data type | Size | Key | NULL? | Description |
|---|---|---|---|---|---|---|
| donor | donor_id | INT | 11 | PK | NOT NULL | Unique donor identifier |
| donor | donor_name | VARCHAR | 100 | - | NOT NULL | Donor / organisation name |
| donor | email | VARCHAR | 100 | UQ | NOT NULL | E-mail address (unique, format-checked) |
| donor | phone | VARCHAR | 15 | UQ | NOT NULL | Phone number, 10-15 digits (unique) |
| donor | donor_type | ENUM | Individual / Corporate / Foundation / Government | - | NOT NULL | Category of donor |

## DONATION

| Table | Attribute | Data type | Size | Key | NULL? | Description |
|---|---|---|---|---|---|---|
| donation | donation_id | INT | 11 | PK | NOT NULL | Unique donation identifier |
| donation | donor_id | INT | 11 | FK | NOT NULL | Donor who gave |
| donation | campaign_id | INT | 11 | FK | NOT NULL | Campaign supported |
| donation | donation_date | DATE | - | - | NOT NULL | Date donation received (not future, inside campaign period) |
| donation | amount | DECIMAL | 12,2 | - | NOT NULL | Amount in INR (> 0) |
| donation | payment_method | ENUM | Cash / Cheque / Bank Transfer / UPI / Card | - | NOT NULL | Mode of payment |

## BENEFICIARY

| Table | Attribute | Data type | Size | Key | NULL? | Description |
|---|---|---|---|---|---|---|
| beneficiary | beneficiary_id | INT | 11 | PK | NOT NULL | Unique beneficiary identifier |
| beneficiary | name | VARCHAR | 100 | - | NOT NULL | Full name |
| beneficiary | contact | VARCHAR | 15 | UQ | NOT NULL | Contact number, 10-15 digits (unique) |
| beneficiary | address | VARCHAR | 255 | - | NULL | Postal address |
| beneficiary | category | ENUM | Child / Student / Women / Elderly / Disabled / Family / Othe | - | NOT NULL | Beneficiary / expense category |

## REQUEST

| Table | Attribute | Data type | Size | Key | NULL? | Description |
|---|---|---|---|---|---|---|
| request | request_id | INT | 11 | PK | NOT NULL | Unique request identifier |
| request | beneficiary_id | INT | 11 | FK | NOT NULL | Requesting beneficiary |
| request | programme_id | INT | 11 | FK | NOT NULL | Programme asked for help |
| request | request_date | DATE | - | - | NOT NULL | Date request raised |
| request | description | VARCHAR | 500 | - | NOT NULL | What help is needed |
| request | requested_amount | DECIMAL | 12,2 | - | NOT NULL | Amount asked for (> 0) |
| request | status | ENUM | Pending / Approved / Rejected / Fulfilled / Cancelled | - | NOT NULL | Request life-cycle: Pending/Approved/Rejected/Fulfilled/Cancelled |

## APPROVAL

| Table | Attribute | Data type | Size | Key | NULL? | Description |
|---|---|---|---|---|---|---|
| approval | approval_id | INT | 11 | PK | NOT NULL | Unique approval identifier |
| approval | request_id | INT | 11 | FK | NOT NULL | Request decided |
| approval | approval_date | DATE | - | - | NOT NULL | Date of decision |
| approval | approved_amount | DECIMAL | 12,2 | - | NOT NULL | Amount sanctioned (0 when rejected) |
| approval | status | ENUM | Approved / Rejected | - | NOT NULL | Decision: Approved or Rejected |

## DISTRIBUTION

| Table | Attribute | Data type | Size | Key | NULL? | Description |
|---|---|---|---|---|---|---|
| distribution | distribution_id | INT | 11 | PK | NOT NULL | Unique distribution identifier |
| distribution | request_id | INT | 11 | FK | NOT NULL | Request being fulfilled |
| distribution | distribution_date | DATE | - | - | NOT NULL | Date aid delivered |
| distribution | amount_or_quantity | DECIMAL | 12,2 | - | NOT NULL | Value of aid delivered (> 0) |
| distribution | distribution_type | ENUM | Cash / Food / Medical / Education / Shelter / Clothing / Equ | - | NOT NULL | Kind of aid |

## VOLUNTEER

| Table | Attribute | Data type | Size | Key | NULL? | Description |
|---|---|---|---|---|---|---|
| volunteer | volunteer_id | INT | 11 | PK | NOT NULL | Unique volunteer identifier |
| volunteer | name | VARCHAR | 100 | - | NOT NULL | Full name |
| volunteer | email | VARCHAR | 100 | UQ | NOT NULL | E-mail address (unique, format-checked) |
| volunteer | phone | VARCHAR | 15 | UQ | NOT NULL | Phone number, 10-15 digits (unique) |
| volunteer | availability | ENUM | Weekdays / Weekends / Evenings / Full-time / Flexible | - | NOT NULL | When the volunteer is available |

## SKILL

| Table | Attribute | Data type | Size | Key | NULL? | Description |
|---|---|---|---|---|---|---|
| skill | skill_id | INT | 11 | PK | NOT NULL | Unique skill identifier |
| skill | skill_name | VARCHAR | 60 | UQ | NOT NULL | Skill name (unique) |
| skill | description | VARCHAR | 255 | - | NULL | Skill description |

## ASSIGNMENT

| Table | Attribute | Data type | Size | Key | NULL? | Description |
|---|---|---|---|---|---|---|
| assignment | assignment_id | INT | 11 | PK | NOT NULL | Unique assignment identifier |
| assignment | volunteer_id | INT | 11 | FK | NOT NULL | Volunteer assigned |
| assignment | skill_id | INT | 11 | FK | NOT NULL | Skill applied |
| assignment | programme_id | INT | 11 | FK | NOT NULL | Programme served |
| assignment | assignment_date | DATE | - | - | NOT NULL | Assignment start date |
| assignment | end_date | DATE | - | - | NOT NULL | Assignment end date (>= start) |
| assignment | role | VARCHAR | 60 | - | NOT NULL | Role played in the programme |

## EXPENSE

| Table | Attribute | Data type | Size | Key | NULL? | Description |
|---|---|---|---|---|---|---|
| expense | expense_id | INT | 11 | PK | NOT NULL | Unique expense identifier |
| expense | programme_id | INT | 11 | FK | NOT NULL | Programme charged |
| expense | campaign_id | INT | 11 | FK | NULL | Optional campaign charged (same programme) |
| expense | expense_date | DATE | - | - | NOT NULL | Date of expense (not future, not before programme start) |
| expense | amount | DECIMAL | 12,2 | - | NOT NULL | Amount in INR (> 0) |
| expense | description | VARCHAR | 255 | - | NOT NULL | What the money was spent on |
| expense | category | ENUM | Food / Medical / Education / Logistics / Salaries / Administ | - | NOT NULL | Beneficiary / expense category |

## IMPACT_INDICATOR

| Table | Attribute | Data type | Size | Key | NULL? | Description |
|---|---|---|---|---|---|---|
| impact_indicator | impact_id | INT | 11 | PK | NOT NULL | Unique indicator record identifier |
| impact_indicator | programme_id | INT | 11 | FK | NOT NULL | Programme measured |
| impact_indicator | indicator_name | VARCHAR | 100 | - | NOT NULL | Name of the measured outcome |
| impact_indicator | target_value | DECIMAL | 12,2 | - | NOT NULL | Planned value (> 0) |
| impact_indicator | actual_value | DECIMAL | 12,2 | - | NOT NULL | Achieved value (>= 0) |
| impact_indicator | measurement_date | DATE | - | - | NOT NULL | Date the actual value was measured |
