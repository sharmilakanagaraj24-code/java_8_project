USE elderly_care_db;
SHOW TABLES;

CREATE TABLE medications (
    medication_id INT PRIMARY KEY AUTO_INCREMENT,
    elderly_id INT NOT NULL,
    medication_name VARCHAR(100) NOT NULL,
    dosage VARCHAR(100) NOT NULL,
    instructions VARCHAR(255),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (elderly_id)
        REFERENCES elderly_users(elderly_id)
);


CREATE TABLE medication_slots (
    slot_id INT PRIMARY KEY AUTO_INCREMENT,
    elderly_id INT NOT NULL,
    medication_id INT NOT NULL,
    slot_number INT NOT NULL,
    capacity INT NOT NULL,
    current_stock INT NOT NULL DEFAULT 0,
    FOREIGN KEY (elderly_id)
        REFERENCES elderly_users(elderly_id),
    FOREIGN KEY (medication_id)
        REFERENCES medications(medication_id),
    UNIQUE (elderly_id, slot_number),
    CHECK (slot_number IN (1, 2)),
    CHECK (capacity >= 0),
    CHECK (current_stock >= 0 AND current_stock <= capacity)
);

CREATE TABLE medication_schedules (
    schedule_id INT PRIMARY KEY AUTO_INCREMENT,
    medication_id INT NOT NULL,
    slot_id INT NOT NULL,
    scheduled_time TIME NOT NULL,
    frequency VARCHAR(50),
    start_date DATE NOT NULL,
    end_date DATE,
    is_active BOOLEAN DEFAULT TRUE,
    FOREIGN KEY (medication_id)
        REFERENCES medications(medication_id),
    FOREIGN KEY (slot_id)
        REFERENCES medication_slots(slot_id)
);

CREATE TABLE scheduled_doses (
    dose_id INT PRIMARY KEY AUTO_INCREMENT,
    schedule_id INT NOT NULL,
    due_time DATETIME NOT NULL,
    dose_status VARCHAR(30) DEFAULT 'PENDING',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (schedule_id)
        REFERENCES medication_schedules(schedule_id)
);

CREATE TABLE dispensing_events (
    dispensing_id INT PRIMARY KEY AUTO_INCREMENT,
    dose_id INT NOT NULL,
    slot_id INT NOT NULL,
    dispensed_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    dispense_status VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    tablets_dispensed INT DEFAULT 0,
    device_message VARCHAR(255),
    FOREIGN KEY (dose_id)
        REFERENCES scheduled_doses(dose_id),
    FOREIGN KEY (slot_id)
        REFERENCES medication_slots(slot_id)
);

CREATE TABLE acknowledgment_events (
    acknowledgment_id INT PRIMARY KEY AUTO_INCREMENT,
    dose_id INT NOT NULL,
    dispensing_id INT,
    acknowledged_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    acknowledgment_method VARCHAR(30) DEFAULT 'BUTTON',
    acknowledgment_status VARCHAR(30) DEFAULT 'ACKNOWLEDGED',
    FOREIGN KEY (dose_id)
        REFERENCES scheduled_doses(dose_id),
    FOREIGN KEY (dispensing_id)
        REFERENCES dispensing_events(dispensing_id)
);

CREATE TABLE stock_changes (
    stock_change_id INT PRIMARY KEY AUTO_INCREMENT,
    slot_id INT NOT NULL,
    quantity_changed INT NOT NULL,
    change_type VARCHAR(30) NOT NULL,
    changed_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    notes VARCHAR(255),
    FOREIGN KEY (slot_id)
        REFERENCES medication_slots(slot_id)
);


CREATE TABLE alerts (
    alert_id INT PRIMARY KEY AUTO_INCREMENT,
    elderly_id INT NOT NULL,
    dose_id INT,
    slot_id INT,
    alert_type VARCHAR(50) NOT NULL,
    alert_message VARCHAR(255) NOT NULL,
    alert_status VARCHAR(30) DEFAULT 'ACTIVE',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    resolved_at DATETIME,
    FOREIGN KEY (elderly_id)
        REFERENCES elderly_users(elderly_id),
    FOREIGN KEY (dose_id)
        REFERENCES scheduled_doses(dose_id),
    FOREIGN KEY (slot_id)
        REFERENCES medication_slots(slot_id)
);

CREATE TABLE devices (
    device_id INT PRIMARY KEY AUTO_INCREMENT,
    elderly_id INT NOT NULL,
    device_name VARCHAR(100) NOT NULL,
    device_code VARCHAR(100) UNIQUE NOT NULL,
    device_status VARCHAR(30) DEFAULT 'OFFLINE',
    last_seen DATETIME,
    registered_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (elderly_id)
        REFERENCES elderly_users(elderly_id)
);

USE elderly_care_db;
SHOW TABLES;

USE elderly_care_db;

SHOW CREATE TABLE scheduled_doses;
SHOW CREATE TABLE medication_schedules;


USE elderly_care_db;
SELECT DATABASE();
SHOW TABLES;

USE elderly_care_db;

CREATE TABLE IF NOT EXISTS elderly_users (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    full_name VARCHAR(100) NOT NULL,
    age INT NOT NULL,
    gender VARCHAR(20),
    phone VARCHAR(15),
    address VARCHAR(255),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

USE elderly_care_db;

SHOW TABLES;

DESCRIBE elderly_users;

USE elderly_care_db;

SELECT * FROM elderly_users;

USE elderly_care_db;

INSERT INTO elderly_users
    (full_name, date_of_birth, gender, address, phone)
VALUES
    ('Test User', '1950-05-15', 'Male',
     'Salem, Tamil Nadu', '9000000001');

SELECT * FROM elderly_users;
USE elderly_care_db;

SHOW TABLES;

DESCRIBE caregivers;
DESCRIBE devices;
DESCRIBE medications;
DESCRIBE medication_slots;
DESCRIBE medication_schedules;
DESCRIBE scheduled_doses;
DESCRIBE dispensing_events;
DESCRIBE acknowledgment_events;
DESCRIBE stock_changes;
DESCRIBE alerts;
USE elderly_care_db;

SELECT
    TABLE_NAME,
    COLUMN_NAME,
    COLUMN_TYPE,
    COLUMN_KEY
FROM INFORMATION_SCHEMA.COLUMNS
WHERE TABLE_SCHEMA = 'elderly_care_db'
ORDER BY TABLE_NAME, ORDINAL_POSITION;
USE elderly_care_db;

SELECT * FROM medications;
USE elderly_care_db;

SELECT * FROM medication_schedules;
SELECT * FROM medication_slots;
DESCRIBE medication_slots;
SELECT * FROM medication_slots;

DESCRIBE dispensing_events;
SELECT * FROM scheduled_doses;
SELECT dose_id, schedule_id, due_time, dose_status
FROM scheduled_doses;
SELECT schedule_id, medication_id, slot_id, start_date, is_active
FROM medication_schedules
WHERE schedule_id = 1;
SELECT schedule_id, medication_id, slot_id, start_date, is_active
FROM medication_schedules
WHERE is_active = 1;
INSERT INTO medication_schedules
    (medication_id, slot_id, scheduled_time, frequency,
     start_date, end_date, is_active)
VALUES
    (2, 1, '11:00:00', 'Daily',
     '2026-10-25', NULL, 1);
SELECT schedule_id, medication_id, slot_id, is_active
FROM medication_schedules;
INSERT INTO scheduled_doses
    (schedule_id, due_time, dose_status)
VALUES
    (3, '2026-10-25 11:00:00', 'PENDING');
SELECT dose_id, schedule_id, due_time, dose_status
FROM scheduled_doses;    
USE elderly_care_db;

SELECT *
FROM medication_slots;

SELECT *
FROM stock_changes;
SELECT * FROM stock_changes;

SELECT *
FROM scheduled_doses
ORDER BY dose_id;

SELECT *
FROM medication_schedules
ORDER BY schedule_id;

SELECT *
FROM medication_slots
ORDER BY slot_id;

INSERT INTO scheduled_doses (schedule_id, due_time, dose_status)
SELECT 3, NOW(), 'PENDING'
WHERE EXISTS (
    SELECT 1
    FROM medication_schedules
    WHERE schedule_id = 3
      AND is_active = 1
);
SELECT *
FROM scheduled_doses
ORDER BY dose_id;
SELECT *
FROM stock_changes
ORDER BY stock_change_id;
INSERT INTO scheduled_doses
    (schedule_id, due_time, dose_status)
VALUES
    (3, NOW(), 'PENDING');

 SELECT dose_id, schedule_id, due_time, dose_status
FROM scheduled_doses
ORDER BY dose_id DESC
LIMIT 1;

SELECT *
FROM stock_changes
WHERE slot_id = 1
ORDER BY stock_change_id DESC
LIMIT 5;

USE elderly_care_db;

DESCRIBE devices;

SELECT device_id, elderly_id, device_name, device_code
FROM devices;
SELECT elderly_id, full_name
FROM elderly_users
WHERE elderly_id = 2;

SELECT
    caregiver_id,
    email,
    CASE
        WHEN password_hash LIKE '$2a$%'
          OR password_hash LIKE '$2b$%'
          OR password_hash LIKE '$2y$%'
        THEN 'BCrypt hash format'
        ELSE 'Other format - needs review'
    END AS password_storage_check
FROM caregivers;
SHOW CREATE TABLE scheduled_doses;

USE elderly_care_db;
SELECT * FROM elderly_users;
SELECT *
FROM elderly_users
ORDER BY elderly_id DESC;
USE elderly_care_db;

DESCRIBE medication_schedules;

DESCRIBE medications;

DESCRIBE medication_slots;

DESCRIBE elderly_users;