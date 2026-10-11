-- =====================================================================
--  NGO Donation, Beneficiary and Volunteer Management System
--  database.sql : DDL (database, tables, constraints, indexes, business-rule triggers)
--  Run order: database.sql -> sample_data.sql -> views.sql -> queries.sql
--  WARNING: drops and re-creates database ngo_db.
--  Target: MySQL 8.0+ (also tested on MariaDB 10.11)
-- =====================================================================
DROP DATABASE IF EXISTS ngo_db;
CREATE DATABASE ngo_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE ngo_db;

-- 1. PROGRAMME
CREATE TABLE programme (
    programme_id   INT AUTO_INCREMENT PRIMARY KEY,
    programme_name VARCHAR(100) NOT NULL,
    description    VARCHAR(500),
    start_date     DATE NOT NULL,
    end_date       DATE,
    status         ENUM('Planned','Active','Completed','On Hold') NOT NULL DEFAULT 'Planned',
    CONSTRAINT uq_programme_name UNIQUE (programme_name),
    CONSTRAINT ck_programme_dates CHECK (end_date IS NULL OR end_date >= start_date)
) ENGINE=InnoDB;

-- 2. CAMPAIGN (Programme 1:M Campaign)
CREATE TABLE campaign (
    campaign_id   INT AUTO_INCREMENT PRIMARY KEY,
    programme_id  INT NOT NULL,
    campaign_name VARCHAR(100) NOT NULL,
    description   VARCHAR(500),
    start_date    DATE NOT NULL,
    end_date      DATE,
    target_amount DECIMAL(12,2) NOT NULL,
    CONSTRAINT fk_campaign_programme FOREIGN KEY (programme_id)
        REFERENCES programme(programme_id) ON UPDATE CASCADE ON DELETE RESTRICT,
    CONSTRAINT uq_campaign_name UNIQUE (programme_id, campaign_name),
    CONSTRAINT uq_campaign_prog UNIQUE (campaign_id, programme_id),  -- target of EXPENSE composite FK
    CONSTRAINT ck_campaign_target CHECK (target_amount > 0),
    CONSTRAINT ck_campaign_dates CHECK (end_date IS NULL OR end_date >= start_date),
    INDEX idx_campaign_programme (programme_id)
) ENGINE=InnoDB;

-- 3. DONOR
CREATE TABLE donor (
    donor_id   INT AUTO_INCREMENT PRIMARY KEY,
    donor_name VARCHAR(100) NOT NULL,
    email      VARCHAR(100) NOT NULL,
    phone      VARCHAR(15)  NOT NULL,
    donor_type ENUM('Individual','Corporate','Foundation','Government') NOT NULL DEFAULT 'Individual',
    CONSTRAINT uq_donor_email UNIQUE (email),
    CONSTRAINT uq_donor_phone UNIQUE (phone),
    CONSTRAINT ck_donor_email CHECK (email REGEXP '^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+[.][A-Za-z]{2,}$'),
    CONSTRAINT ck_donor_phone CHECK (phone REGEXP '^[0-9]{10,15}$')
) ENGINE=InnoDB;

-- 4. DONATION (Donor 1:M Donation, Campaign 1:M Donation)
CREATE TABLE donation (
    donation_id    INT AUTO_INCREMENT PRIMARY KEY,
    donor_id       INT NOT NULL,
    campaign_id    INT NOT NULL,
    donation_date  DATE NOT NULL,
    amount         DECIMAL(12,2) NOT NULL,
    payment_method ENUM('Cash','Cheque','Bank Transfer','UPI','Card') NOT NULL DEFAULT 'UPI',
    CONSTRAINT fk_donation_donor FOREIGN KEY (donor_id)
        REFERENCES donor(donor_id) ON UPDATE CASCADE ON DELETE RESTRICT,
    CONSTRAINT fk_donation_campaign FOREIGN KEY (campaign_id)
        REFERENCES campaign(campaign_id) ON UPDATE CASCADE ON DELETE RESTRICT,
    CONSTRAINT ck_donation_amount CHECK (amount > 0),
    INDEX idx_donation_donor (donor_id),
    INDEX idx_donation_campaign (campaign_id),
    INDEX idx_donation_date (donation_date)
) ENGINE=InnoDB;

-- 5. BENEFICIARY
CREATE TABLE beneficiary (
    beneficiary_id INT AUTO_INCREMENT PRIMARY KEY,
    name           VARCHAR(100) NOT NULL,
    contact        VARCHAR(15)  NOT NULL,
    address        VARCHAR(255),
    category       ENUM('Child','Student','Women','Elderly','Disabled','Family','Other') NOT NULL DEFAULT 'Other',
    CONSTRAINT uq_beneficiary_contact UNIQUE (contact),
    CONSTRAINT ck_beneficiary_contact CHECK (contact REGEXP '^[0-9]{10,15}$')
) ENGINE=InnoDB;

-- 6. REQUEST (Beneficiary 1:M Request, Programme 1:M Request)
--    requested_amount: improvement over Review-1 so approvals can be checked against the ask.
CREATE TABLE request (
    request_id       INT AUTO_INCREMENT PRIMARY KEY,
    beneficiary_id   INT NOT NULL,
    programme_id     INT NOT NULL,
    request_date     DATE NOT NULL,
    description      VARCHAR(500) NOT NULL,
    requested_amount DECIMAL(12,2) NOT NULL,
    status           ENUM('Pending','Approved','Rejected','Fulfilled','Cancelled') NOT NULL DEFAULT 'Pending',
    CONSTRAINT fk_request_beneficiary FOREIGN KEY (beneficiary_id)
        REFERENCES beneficiary(beneficiary_id) ON UPDATE CASCADE ON DELETE RESTRICT,
    CONSTRAINT fk_request_programme FOREIGN KEY (programme_id)
        REFERENCES programme(programme_id) ON UPDATE CASCADE ON DELETE RESTRICT,
    CONSTRAINT ck_request_amount CHECK (requested_amount > 0),
    INDEX idx_request_beneficiary (beneficiary_id),
    INDEX idx_request_programme (programme_id),
    INDEX idx_request_status (status)
) ENGINE=InnoDB;

-- 7. APPROVAL (Request 1:M Approval; decisions are immutable audit rows)
CREATE TABLE approval (
    approval_id     INT AUTO_INCREMENT PRIMARY KEY,
    request_id      INT NOT NULL,
    approval_date   DATE NOT NULL,
    approved_amount DECIMAL(12,2) NOT NULL DEFAULT 0,
    status          ENUM('Approved','Rejected') NOT NULL,
    CONSTRAINT fk_approval_request FOREIGN KEY (request_id)
        REFERENCES request(request_id) ON UPDATE CASCADE ON DELETE RESTRICT,
    CONSTRAINT ck_approval_amount CHECK (
        (status = 'Approved' AND approved_amount > 0) OR (status = 'Rejected' AND approved_amount = 0)),
    INDEX idx_approval_request (request_id)
) ENGINE=InnoDB;

-- 8. DISTRIBUTION (Request 1:M Distribution)
--    Review-1 also stored beneficiary_id here: transitive dependency
--    (distribution_id -> request_id -> beneficiary_id) removed for 3NF.
CREATE TABLE distribution (
    distribution_id    INT AUTO_INCREMENT PRIMARY KEY,
    request_id         INT NOT NULL,
    distribution_date  DATE NOT NULL,
    amount_or_quantity DECIMAL(12,2) NOT NULL,
    distribution_type  ENUM('Cash','Food','Medical','Education','Shelter','Clothing','Equipment') NOT NULL,
    CONSTRAINT fk_distribution_request FOREIGN KEY (request_id)
        REFERENCES request(request_id) ON UPDATE CASCADE ON DELETE RESTRICT,
    CONSTRAINT ck_distribution_amount CHECK (amount_or_quantity > 0),
    INDEX idx_distribution_request (request_id)
) ENGINE=InnoDB;

-- 9. VOLUNTEER
CREATE TABLE volunteer (
    volunteer_id INT AUTO_INCREMENT PRIMARY KEY,
    name         VARCHAR(100) NOT NULL,
    email        VARCHAR(100) NOT NULL,
    phone        VARCHAR(15)  NOT NULL,
    availability ENUM('Weekdays','Weekends','Evenings','Full-time','Flexible') NOT NULL DEFAULT 'Flexible',
    CONSTRAINT uq_volunteer_email UNIQUE (email),
    CONSTRAINT uq_volunteer_phone UNIQUE (phone),
    CONSTRAINT ck_volunteer_email CHECK (email REGEXP '^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+[.][A-Za-z]{2,}$'),
    CONSTRAINT ck_volunteer_phone CHECK (phone REGEXP '^[0-9]{10,15}$')
) ENGINE=InnoDB;

-- 10. SKILL
CREATE TABLE skill (
    skill_id    INT AUTO_INCREMENT PRIMARY KEY,
    skill_name  VARCHAR(60) NOT NULL,
    description VARCHAR(255),
    CONSTRAINT uq_skill_name UNIQUE (skill_name)
) ENGINE=InnoDB;

-- 11. ASSIGNMENT (Volunteer / Skill / Programme). assignment_date = start; end_date added for overlap detection.
CREATE TABLE assignment (
    assignment_id   INT AUTO_INCREMENT PRIMARY KEY,
    volunteer_id    INT NOT NULL,
    skill_id        INT NOT NULL,
    programme_id    INT NOT NULL,
    assignment_date DATE NOT NULL,
    end_date        DATE NOT NULL,
    role            VARCHAR(60) NOT NULL,
    CONSTRAINT fk_assignment_volunteer FOREIGN KEY (volunteer_id)
        REFERENCES volunteer(volunteer_id) ON UPDATE CASCADE ON DELETE RESTRICT,
    CONSTRAINT fk_assignment_skill FOREIGN KEY (skill_id)
        REFERENCES skill(skill_id) ON UPDATE CASCADE ON DELETE RESTRICT,
    CONSTRAINT fk_assignment_programme FOREIGN KEY (programme_id)
        REFERENCES programme(programme_id) ON UPDATE CASCADE ON DELETE RESTRICT,
    CONSTRAINT ck_assignment_dates CHECK (end_date >= assignment_date),
    INDEX idx_assignment_volunteer (volunteer_id, assignment_date, end_date),
    INDEX idx_assignment_programme (programme_id),
    INDEX idx_assignment_skill (skill_id)
) ENGINE=InnoDB;

-- 12. EXPENSE (Programme 1:M Expense; optional campaign link).
--     Composite FK (campaign_id, programme_id) forces a campaign-level expense to use a campaign of the
--     SAME programme. campaign_id NULL = programme-level expense (FK not checked for NULL).
CREATE TABLE expense (
    expense_id   INT AUTO_INCREMENT PRIMARY KEY,
    programme_id INT NOT NULL,
    campaign_id  INT NULL,
    expense_date DATE NOT NULL,
    amount       DECIMAL(12,2) NOT NULL,
    description  VARCHAR(255) NOT NULL,
    category     ENUM('Food','Medical','Education','Logistics','Salaries','Administration',
                      'Supplies','Events','Construction') NOT NULL,
    CONSTRAINT fk_expense_programme FOREIGN KEY (programme_id)
        REFERENCES programme(programme_id) ON UPDATE CASCADE ON DELETE RESTRICT,
    CONSTRAINT fk_expense_campaign FOREIGN KEY (campaign_id, programme_id)
        REFERENCES campaign(campaign_id, programme_id) ON UPDATE CASCADE ON DELETE RESTRICT,
    CONSTRAINT ck_expense_amount CHECK (amount > 0),
    INDEX idx_expense_programme_date (programme_id, expense_date),
    INDEX idx_expense_campaign (campaign_id)
) ENGINE=InnoDB;

-- 13. IMPACT_INDICATOR (Programme 1:M Impact_Indicator)
CREATE TABLE impact_indicator (
    impact_id        INT AUTO_INCREMENT PRIMARY KEY,
    programme_id     INT NOT NULL,
    indicator_name   VARCHAR(100) NOT NULL,
    target_value     DECIMAL(12,2) NOT NULL,
    actual_value     DECIMAL(12,2) NOT NULL DEFAULT 0,
    measurement_date DATE NOT NULL,
    CONSTRAINT fk_impact_programme FOREIGN KEY (programme_id)
        REFERENCES programme(programme_id) ON UPDATE CASCADE ON DELETE RESTRICT,
    CONSTRAINT uq_impact UNIQUE (programme_id, indicator_name, measurement_date),
    CONSTRAINT ck_impact_target CHECK (target_value > 0),
    CONSTRAINT ck_impact_actual CHECK (actual_value >= 0),
    INDEX idx_impact_programme (programme_id)
) ENGINE=InnoDB;

-- =====================================================================
--  BUSINESS RULES that CHECK constraints cannot express (cross-row /
--  cross-table): stored procedures + triggers.  Errors use SQLSTATE 45000.
-- =====================================================================
DELIMITER $$

-- Campaign must lie inside its programme's period
CREATE PROCEDURE sp_check_campaign(IN p_programme INT, IN p_start DATE, IN p_end DATE)
BEGIN
    DECLARE v_pstart DATE; DECLARE v_pend DATE;
    SELECT start_date, end_date INTO v_pstart, v_pend FROM programme WHERE programme_id = p_programme;
    IF p_start < v_pstart THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Campaign cannot start before its programme starts';
    END IF;
    IF v_pend IS NOT NULL AND (p_start > v_pend OR (p_end IS NOT NULL AND p_end > v_pend)) THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Campaign cannot extend beyond its programme end date';
    END IF;
END$$
CREATE TRIGGER trg_campaign_bi BEFORE INSERT ON campaign FOR EACH ROW
    CALL sp_check_campaign(NEW.programme_id, NEW.start_date, NEW.end_date)$$
CREATE TRIGGER trg_campaign_bu BEFORE UPDATE ON campaign FOR EACH ROW
    CALL sp_check_campaign(NEW.programme_id, NEW.start_date, NEW.end_date)$$

-- Donation: date valid and inside campaign window
CREATE PROCEDURE sp_check_donation(IN p_campaign INT, IN p_date DATE)
BEGIN
    DECLARE v_start DATE; DECLARE v_end DATE;
    SELECT start_date, end_date INTO v_start, v_end FROM campaign WHERE campaign_id = p_campaign;
    IF p_date > CURDATE() THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Donation date cannot be in the future';
    END IF;
    IF v_start IS NOT NULL AND p_date < v_start THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Donation date is before the campaign start date';
    END IF;
    IF v_end IS NOT NULL AND p_date > v_end THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Campaign has ended; donation date is after campaign end date';
    END IF;
END$$

-- Programme funds must always cover what has already been distributed
CREATE PROCEDURE sp_check_funds_cover(IN p_programme INT, IN p_removed DECIMAL(12,2))
BEGIN
    DECLARE v_funds DECIMAL(14,2); DECLARE v_dist DECIMAL(14,2);
    SELECT IFNULL(SUM(d.amount),0) INTO v_funds FROM donation d
      JOIN campaign c ON c.campaign_id = d.campaign_id WHERE c.programme_id = p_programme;
    SELECT IFNULL(SUM(x.amount_or_quantity),0) INTO v_dist FROM distribution x
      JOIN request r ON r.request_id = x.request_id WHERE r.programme_id = p_programme;
    IF v_funds - p_removed < v_dist THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Change rejected: programme funds would fall below the amount already distributed';
    END IF;
END$$

CREATE TRIGGER trg_donation_bi BEFORE INSERT ON donation FOR EACH ROW
    CALL sp_check_donation(NEW.campaign_id, NEW.donation_date)$$
CREATE TRIGGER trg_donation_bu BEFORE UPDATE ON donation FOR EACH ROW
BEGIN
    DECLARE v_prog INT;
    IF NEW.campaign_id <> OLD.campaign_id THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Campaign of an existing donation cannot be changed';
    END IF;
    CALL sp_check_donation(NEW.campaign_id, NEW.donation_date);
    IF NEW.amount < OLD.amount THEN
        SELECT programme_id INTO v_prog FROM campaign WHERE campaign_id = OLD.campaign_id;
        CALL sp_check_funds_cover(v_prog, OLD.amount - NEW.amount);
    END IF;
END$$
CREATE TRIGGER trg_donation_bd BEFORE DELETE ON donation FOR EACH ROW
BEGIN
    DECLARE v_prog INT;
    SELECT programme_id INTO v_prog FROM campaign WHERE campaign_id = OLD.campaign_id;
    CALL sp_check_funds_cover(v_prog, OLD.amount);
END$$

-- Request: starts Pending, only for Active programmes, controlled status life-cycle
CREATE TRIGGER trg_request_bi BEFORE INSERT ON request FOR EACH ROW
BEGIN
    DECLARE v_pstatus VARCHAR(20);
    SELECT status INTO v_pstatus FROM programme WHERE programme_id = NEW.programme_id;
    IF NEW.status <> 'Pending' THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'A new request must start with status Pending';
    END IF;
    IF NEW.request_date > CURDATE() THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Request date cannot be in the future';
    END IF;
    IF v_pstatus IS NOT NULL AND v_pstatus <> 'Active' THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Requests can only be raised against an Active programme';
    END IF;
END$$
CREATE TRIGGER trg_request_bu BEFORE UPDATE ON request FOR EACH ROW
BEGIN
    IF NEW.status <> OLD.status THEN
        IF OLD.status IN ('Rejected','Cancelled') THEN
            SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'A rejected or cancelled request is closed and cannot change status';
        ELSEIF NEW.status = 'Pending' THEN
            SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'A request cannot go back to Pending';
        ELSEIF NEW.status = 'Cancelled' AND OLD.status <> 'Pending' THEN
            SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Only a Pending request can be cancelled';
        ELSEIF NEW.status IN ('Approved','Fulfilled') AND
               NOT EXISTS (SELECT 1 FROM approval WHERE request_id = NEW.request_id AND status = 'Approved') THEN
            SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Status Approved/Fulfilled requires an approval record';
        ELSEIF NEW.status = 'Rejected' AND
               NOT EXISTS (SELECT 1 FROM approval WHERE request_id = NEW.request_id AND status = 'Rejected') THEN
            SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Status Rejected requires a rejection record in APPROVAL';
        END IF;
    END IF;
END$$

-- Approval: only valid Pending requests; approved <= requested
CREATE TRIGGER trg_approval_bi BEFORE INSERT ON approval FOR EACH ROW
BEGIN
    DECLARE v_status VARCHAR(20); DECLARE v_req DECIMAL(12,2); DECLARE v_rdate DATE;
    SELECT status, requested_amount, request_date INTO v_status, v_req, v_rdate
      FROM request WHERE request_id = NEW.request_id;
    IF v_status IS NULL THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Request does not exist';
    END IF;
    IF v_status <> 'Pending' THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Only a Pending request can be approved or rejected';
    END IF;
    IF NEW.approval_date < v_rdate OR NEW.approval_date > CURDATE() THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Approval date must be between the request date and today';
    END IF;
    IF NEW.status = 'Approved' AND NEW.approved_amount > v_req THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Approved amount cannot exceed the requested amount';
    END IF;
END$$
CREATE TRIGGER trg_approval_ai AFTER INSERT ON approval FOR EACH ROW
    UPDATE request SET status = NEW.status WHERE request_id = NEW.request_id$$
CREATE TRIGGER trg_approval_bu BEFORE UPDATE ON approval FOR EACH ROW
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Approval decisions are permanent audit records and cannot be edited'$$
CREATE TRIGGER trg_approval_bd BEFORE DELETE ON approval FOR EACH ROW
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Approval decisions are permanent audit records and cannot be deleted'$$

-- Distribution: approved request, total <= approved amount, total <= programme funds
CREATE PROCEDURE sp_check_distribution(IN p_id INT, IN p_request INT, IN p_date DATE, IN p_amount DECIMAL(12,2))
BEGIN
    DECLARE v_status VARCHAR(20); DECLARE v_prog INT;
    DECLARE v_approved DECIMAL(14,2); DECLARE v_adate DATE;
    DECLARE v_done DECIMAL(14,2); DECLARE v_funds DECIMAL(14,2); DECLARE v_progdist DECIMAL(14,2);
    SELECT status, programme_id INTO v_status, v_prog FROM request WHERE request_id = p_request;
    IF v_status IS NULL THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Request does not exist';
    END IF;
    IF v_status NOT IN ('Approved','Fulfilled') THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Distribution is allowed only for approved requests';
    END IF;
    SELECT approved_amount, approval_date INTO v_approved, v_adate FROM approval
      WHERE request_id = p_request AND status = 'Approved' ORDER BY approval_id DESC LIMIT 1;
    IF p_date < v_adate OR p_date > CURDATE() THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Distribution date must be between the approval date and today';
    END IF;
    SELECT IFNULL(SUM(amount_or_quantity),0) INTO v_done FROM distribution
      WHERE request_id = p_request AND distribution_id <> IFNULL(p_id,0);
    IF v_done + p_amount > v_approved THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Distribution exceeds the approved amount for this request';
    END IF;
    SELECT IFNULL(SUM(d.amount),0) INTO v_funds FROM donation d
      JOIN campaign c ON c.campaign_id = d.campaign_id WHERE c.programme_id = v_prog;
    SELECT IFNULL(SUM(x.amount_or_quantity),0) INTO v_progdist FROM distribution x
      JOIN request r ON r.request_id = x.request_id
      WHERE r.programme_id = v_prog AND x.distribution_id <> IFNULL(p_id,0);
    IF v_progdist + p_amount > v_funds THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Insufficient funds: programme donations do not cover this distribution';
    END IF;
END$$

-- Approved <-> Fulfilled according to how much has been distributed
CREATE PROCEDURE sp_sync_request_status(IN p_request INT)
BEGIN
    DECLARE v_status VARCHAR(20); DECLARE v_approved DECIMAL(14,2); DECLARE v_done DECIMAL(14,2);
    SELECT status INTO v_status FROM request WHERE request_id = p_request;
    IF v_status IN ('Approved','Fulfilled') THEN
        SELECT approved_amount INTO v_approved FROM approval
          WHERE request_id = p_request AND status = 'Approved' ORDER BY approval_id DESC LIMIT 1;
        SELECT IFNULL(SUM(amount_or_quantity),0) INTO v_done FROM distribution WHERE request_id = p_request;
        IF v_done >= v_approved AND v_status = 'Approved' THEN
            UPDATE request SET status = 'Fulfilled' WHERE request_id = p_request;
        ELSEIF v_done < v_approved AND v_status = 'Fulfilled' THEN
            UPDATE request SET status = 'Approved' WHERE request_id = p_request;
        END IF;
    END IF;
END$$

CREATE TRIGGER trg_distribution_bi BEFORE INSERT ON distribution FOR EACH ROW
    CALL sp_check_distribution(NULL, NEW.request_id, NEW.distribution_date, NEW.amount_or_quantity)$$
CREATE TRIGGER trg_distribution_bu BEFORE UPDATE ON distribution FOR EACH ROW
    CALL sp_check_distribution(OLD.distribution_id, NEW.request_id, NEW.distribution_date, NEW.amount_or_quantity)$$
CREATE TRIGGER trg_distribution_ai AFTER INSERT ON distribution FOR EACH ROW
    CALL sp_sync_request_status(NEW.request_id)$$
CREATE TRIGGER trg_distribution_au AFTER UPDATE ON distribution FOR EACH ROW
BEGIN
    CALL sp_sync_request_status(OLD.request_id);
    IF NEW.request_id <> OLD.request_id THEN CALL sp_sync_request_status(NEW.request_id); END IF;
END$$
CREATE TRIGGER trg_distribution_ad AFTER DELETE ON distribution FOR EACH ROW
    CALL sp_sync_request_status(OLD.request_id)$$

-- Assignment: a volunteer cannot have overlapping periods
CREATE PROCEDURE sp_check_assignment(IN p_id INT, IN p_vol INT, IN p_start DATE, IN p_end DATE)
BEGIN
    IF EXISTS (SELECT 1 FROM assignment
                WHERE volunteer_id = p_vol AND assignment_id <> IFNULL(p_id,0)
                  AND assignment_date <= p_end AND end_date >= p_start) THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Overlapping assignment: volunteer is already assigned during this period';
    END IF;
END$$
CREATE TRIGGER trg_assignment_bi BEFORE INSERT ON assignment FOR EACH ROW
    CALL sp_check_assignment(NULL, NEW.volunteer_id, NEW.assignment_date, NEW.end_date)$$
CREATE TRIGGER trg_assignment_bu BEFORE UPDATE ON assignment FOR EACH ROW
    CALL sp_check_assignment(OLD.assignment_id, NEW.volunteer_id, NEW.assignment_date, NEW.end_date)$$

-- Expense: valid date (not future, not before programme start)
CREATE PROCEDURE sp_check_expense(IN p_programme INT, IN p_date DATE)
BEGIN
    DECLARE v_start DATE;
    SELECT start_date INTO v_start FROM programme WHERE programme_id = p_programme;
    IF p_date > CURDATE() THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Expense date cannot be in the future';
    END IF;
    IF v_start IS NOT NULL AND p_date < v_start THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Expense date is before the programme start date';
    END IF;
END$$
CREATE TRIGGER trg_expense_bi BEFORE INSERT ON expense FOR EACH ROW
    CALL sp_check_expense(NEW.programme_id, NEW.expense_date)$$
CREATE TRIGGER trg_expense_bu BEFORE UPDATE ON expense FOR EACH ROW
    CALL sp_check_expense(NEW.programme_id, NEW.expense_date)$$

DELIMITER ;
