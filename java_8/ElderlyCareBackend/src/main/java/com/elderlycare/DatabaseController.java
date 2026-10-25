
package com.elderlycare;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Time;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.sql.DataSource;
import java.sql.Timestamp;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@CrossOrigin(origins = "*")
public class DatabaseController {

    private final DataSource dataSource;
     private final BCryptPasswordEncoder passwordEncoder =
        new BCryptPasswordEncoder();
    public DatabaseController(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    // ---------------- DATABASE CONNECTION CHECK ----------------

    @GetMapping("/api/db-check")
    public String checkDatabase() {
        try (Connection con = dataSource.getConnection()) {
            return "MySQL connected successfully! Database: "
                    + con.getCatalog();
        } catch (SQLException e) {
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Database connection failed");
        }
    }

    // ---------------- ELDERLY USERS ----------------

    @GetMapping("/api/elderly-users")
    public List<ElderlyUser> getElderlyUsers() throws SQLException {
        List<ElderlyUser> users = new ArrayList<>();

        String sql = "SELECT elderly_id, full_name, date_of_birth, "
                + "gender, address, phone FROM elderly_users";

        try (Connection con = dataSource.getConnection();
             Statement stmt = con.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                ElderlyUser user = new ElderlyUser();

                user.setElderlyId(rs.getInt("elderly_id"));
                user.setFullName(rs.getString("full_name"));

                Date dob = rs.getDate("date_of_birth");
                user.setDateOfBirth(dob == null ? null : dob.toString());

                user.setGender(rs.getString("gender"));
                user.setAddress(rs.getString("address"));
                user.setPhone(rs.getString("phone"));

                users.add(user);
            }
        }

        return users;
    }

    @PostMapping("/api/elderly-users")
    public String addElderlyUser(@RequestBody ElderlyUser user) {
        String sql = "INSERT INTO elderly_users "
                + "(full_name, date_of_birth, gender, address, phone) "
                + "VALUES (?, ?, ?, ?, ?)";

        try (Connection con = dataSource.getConnection();
             PreparedStatement stmt = con.prepareStatement(sql)) {

            stmt.setString(1, user.getFullName());

            if (user.getDateOfBirth() == null
                    || user.getDateOfBirth().trim().isEmpty()) {
                stmt.setNull(2, java.sql.Types.DATE);
            } else {
                stmt.setDate(2, Date.valueOf(user.getDateOfBirth()));
            }

            stmt.setString(3, user.getGender());
            stmt.setString(4, user.getAddress());
            stmt.setString(5, user.getPhone());

            return stmt.executeUpdate() > 0
                    ? "Elderly user added successfully!"
                    : "Failed to add elderly user.";

        } catch (SQLException e) {
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Could not add elderly user");
        }
    }

    @PutMapping("/api/elderly-users/{id}")
    public String updateElderlyUser(
            @PathVariable int id,
            @RequestBody ElderlyUser user) {

        String sql = "UPDATE elderly_users SET full_name = ?, "
                + "date_of_birth = ?, gender = ?, address = ?, phone = ? "
                + "WHERE elderly_id = ?";

        try (Connection con = dataSource.getConnection();
             PreparedStatement stmt = con.prepareStatement(sql)) {

            stmt.setString(1, user.getFullName());

            if (user.getDateOfBirth() == null
                    || user.getDateOfBirth().trim().isEmpty()) {
                stmt.setNull(2, java.sql.Types.DATE);
            } else {
                stmt.setDate(2, Date.valueOf(user.getDateOfBirth()));
            }

            stmt.setString(3, user.getGender());
            stmt.setString(4, user.getAddress());
            stmt.setString(5, user.getPhone());
            stmt.setInt(6, id);

            return stmt.executeUpdate() > 0
                    ? "Elderly user updated successfully!"
                    : "No elderly user found with ID: " + id;

        } catch (SQLException e) {
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Could not update elderly user");
        }
    }

    @DeleteMapping("/api/elderly-users/{id}")
    public String deleteElderlyUser(@PathVariable int id) {
        String sql = "DELETE FROM elderly_users WHERE elderly_id = ?";

        try (Connection con = dataSource.getConnection();
             PreparedStatement stmt = con.prepareStatement(sql)) {

            stmt.setInt(1, id);

            return stmt.executeUpdate() > 0
                    ? "Elderly user deleted successfully!"
                    : "No elderly user found with ID: " + id;

        } catch (SQLException e) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Could not delete elderly user; related records may exist");
        }
    }

    // ---------------- CAREGIVERS ----------------

    // Never return password_hash in API responses
    private Map<String, Object> caregiverResponse(ResultSet rs)
            throws SQLException {

        Map<String, Object> result = new LinkedHashMap<>();

        result.put("caregiverId", rs.getInt("caregiver_id"));
        result.put("elderlyId", rs.getInt("elderly_id"));
        result.put("fullName", rs.getString("full_name"));
        result.put("email", rs.getString("email"));
        result.put("phone", rs.getString("phone"));
        result.put("createdAt", rs.getTimestamp("created_at"));

        return result;
    }

    @GetMapping("/api/caregivers")
    public List<Map<String, Object>> getCaregivers()
            throws SQLException {

        List<Map<String, Object>> caregivers = new ArrayList<>();

        String sql = "SELECT caregiver_id, elderly_id, full_name, "
                + "email, phone, created_at FROM caregivers";

        try (Connection con = dataSource.getConnection();
             Statement stmt = con.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                caregivers.add(caregiverResponse(rs));
            }
        }

        return caregivers;
    }

    @GetMapping("/api/caregivers/{id}")
    public Map<String, Object> getCaregiver(@PathVariable int id)
            throws SQLException {

        String sql = "SELECT caregiver_id, elderly_id, full_name, "
                + "email, phone, created_at FROM caregivers "
                + "WHERE caregiver_id = ?";

        try (Connection con = dataSource.getConnection();
             PreparedStatement stmt = con.prepareStatement(sql)) {

            stmt.setInt(1, id);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return caregiverResponse(rs);
                }
            }
        }

        throw new ResponseStatusException(
                HttpStatus.NOT_FOUND, "Caregiver not found");
    }

    
@PostMapping("/api/caregivers")
public String addCaregiver(@RequestBody Caregiver caregiver) {

    if (caregiver.getFullName() == null
            || caregiver.getFullName().trim().isEmpty()
            || caregiver.getEmail() == null
            || caregiver.getEmail().trim().isEmpty()
            || caregiver.getPasswordHash() == null
            || caregiver.getPasswordHash().trim().isEmpty()) {

        throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Full name, email and password are required");
    }

    // Hash the supplied password before storing it
    String hashedPassword =
            passwordEncoder.encode(caregiver.getPasswordHash());

    String sql = "INSERT INTO caregivers "
            + "(elderly_id, full_name, email, phone, password_hash) "
            + "VALUES (?, ?, ?, ?, ?)";

    try (Connection con = dataSource.getConnection();
         PreparedStatement stmt = con.prepareStatement(sql)) {

        stmt.setInt(1, caregiver.getElderlyId());
        stmt.setString(2, caregiver.getFullName().trim());
        stmt.setString(3, caregiver.getEmail().trim());
        stmt.setString(4, caregiver.getPhone());
        stmt.setString(5, hashedPassword);

        return stmt.executeUpdate() > 0
                ? "Caregiver added successfully!"
                : "Failed to add caregiver.";

    } catch (SQLException e) {

        if ("23000".equals(e.getSQLState())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Check email uniqueness and elderly user ID");
        }

        throw new ResponseStatusException(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Could not add caregiver");
    }
}


    @PutMapping("/api/caregivers/{id}")
    public String updateCaregiver(
            @PathVariable int id,
            @RequestBody Caregiver caregiver) {

        String sql = "UPDATE caregivers SET elderly_id = ?, "
                + "full_name = ?, email = ?, phone = ? "
                + "WHERE caregiver_id = ?";

        try (Connection con = dataSource.getConnection();
             PreparedStatement stmt = con.prepareStatement(sql)) {

            stmt.setInt(1, caregiver.getElderlyId());
            stmt.setString(2, caregiver.getFullName());
            stmt.setString(3, caregiver.getEmail());
            stmt.setString(4, caregiver.getPhone());
            stmt.setInt(5, id);

            return stmt.executeUpdate() > 0
                    ? "Caregiver updated successfully!"
                    : "No caregiver found with ID: " + id;

        } catch (SQLException e) {
            if ("23000".equals(e.getSQLState())) {
                throw new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        "Check email uniqueness and elderly user ID");
            }

            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Could not update caregiver");
        }
    }

@PostMapping("/api/caregivers/login")
public ResponseEntity<?> caregiverLogin(
        @RequestBody CaregiverLoginRequest request) {

    if (request.getEmail() == null
            || request.getEmail().trim().isEmpty()
            || request.getPassword() == null
            || request.getPassword().isEmpty()) {

        return ResponseEntity.badRequest()
                .body("Email and password are required.");
    }

    String sql = "SELECT caregiver_id, elderly_id, full_name, "
            + "email, phone, password_hash, created_at "
            + "FROM caregivers WHERE email = ?";

    try (Connection con = dataSource.getConnection();
         PreparedStatement stmt = con.prepareStatement(sql)) {

        stmt.setString(1, request.getEmail().trim());

        try (ResultSet rs = stmt.executeQuery()) {

            if (!rs.next()
                    || !passwordEncoder.matches(
                            request.getPassword(),
                            rs.getString("password_hash"))) {

                return ResponseEntity.status(401)
                        .body("Invalid email or password.");
            }

            return ResponseEntity.ok(caregiverResponse(rs));
        }

    } catch (SQLException e) {
        return ResponseEntity.status(500)
                .body("Unable to process login.");
    }
}

    // ---------------- MEDICATIONS ----------------

    private Map<String, Object> medicationResponse(ResultSet rs)
            throws SQLException {

        Map<String, Object> result = new LinkedHashMap<>();

        result.put("medicationId", rs.getInt("medication_id"));
        result.put("elderlyId", rs.getInt("elderly_id"));
        result.put("medicationName", rs.getString("medication_name"));
        result.put("dosage", rs.getString("dosage"));
        result.put("instructions", rs.getString("instructions"));
        result.put("createdAt", rs.getTimestamp("created_at"));

        return result;
    }

    @GetMapping("/api/medications")
    public List<Map<String, Object>> getMedications()
            throws SQLException {

        List<Map<String, Object>> medications = new ArrayList<>();

        String sql = "SELECT medication_id, elderly_id, medication_name, "
                + "dosage, instructions, created_at FROM medications";

        try (Connection con = dataSource.getConnection();
             Statement stmt = con.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                medications.add(medicationResponse(rs));
            }
        }

        return medications;
    }

    @GetMapping("/api/medications/{id}")
    public Map<String, Object> getMedication(@PathVariable int id)
            throws SQLException {

        String sql = "SELECT medication_id, elderly_id, medication_name, "
                + "dosage, instructions, created_at FROM medications "
                + "WHERE medication_id = ?";

        try (Connection con = dataSource.getConnection();
             PreparedStatement stmt = con.prepareStatement(sql)) {

            stmt.setInt(1, id);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return medicationResponse(rs);
                }
            }
        }

        throw new ResponseStatusException(
                HttpStatus.NOT_FOUND, "Medication not found");
    }

    @PostMapping("/api/medications")
    public String addMedication(@RequestBody Medication medication) {
        
if (medication.getElderlyId() <= 0
        || medication.getMedicationName() == null
        || medication.getMedicationName().trim().isEmpty()
        || medication.getDosage() == null
        || medication.getDosage().trim().isEmpty()) {

    throw new ResponseStatusException(
            HttpStatus.BAD_REQUEST,
            "Valid elderly ID, medication name and dosage are required");
}


        String sql = "INSERT INTO medications "
                + "(elderly_id, medication_name, dosage, instructions) "
                + "VALUES (?, ?, ?, ?)";

        try (Connection con = dataSource.getConnection();
             PreparedStatement stmt = con.prepareStatement(sql)) {

            stmt.setInt(1, medication.getElderlyId());
            stmt.setString(2, medication.getMedicationName());
            stmt.setString(3, medication.getDosage());
            stmt.setString(4, medication.getInstructions());

            return stmt.executeUpdate() > 0
                    ? "Medication added successfully!"
                    : "Failed to add medication.";

        } catch (SQLException e) {
            if ("23000".equals(e.getSQLState())) {
                throw new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        "Check that elderlyId refers to an existing user");
            }

            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Could not add medication");
        }
    }

    @PutMapping("/api/medications/{id}")
    public String updateMedication(
            @PathVariable int id,
            @RequestBody Medication medication) {

        
if (medication.getElderlyId() <= 0
        || medication.getMedicationName() == null
        || medication.getMedicationName().trim().isEmpty()
        || medication.getDosage() == null
        || medication.getDosage().trim().isEmpty()) {

    throw new ResponseStatusException(
            HttpStatus.BAD_REQUEST,
            "Valid elderly ID, medication name and dosage are required");
}

        String sql = "UPDATE medications SET elderly_id = ?, "
                + "medication_name = ?, dosage = ?, instructions = ? "
                + "WHERE medication_id = ?";

        try (Connection con = dataSource.getConnection();
             PreparedStatement stmt = con.prepareStatement(sql)) {

            stmt.setInt(1, medication.getElderlyId());
            stmt.setString(2, medication.getMedicationName());
            stmt.setString(3, medication.getDosage());
            stmt.setString(4, medication.getInstructions());
            stmt.setInt(5, id);

            return stmt.executeUpdate() > 0
                    ? "Medication updated successfully!"
                    : "No medication found with ID: " + id;

        } catch (SQLException e) {
            if ("23000".equals(e.getSQLState())) {
                throw new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        "Check that elderlyId refers to an existing user");
            }

            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Could not update medication");
        }
    }

    
@DeleteMapping("/api/medications/{id}")
public String deleteMedication(@PathVariable int id) {
    String sql = "DELETE FROM medications WHERE medication_id = ?";

    try (Connection con = dataSource.getConnection();
         PreparedStatement stmt = con.prepareStatement(sql)) {

        stmt.setInt(1, id);

        if (stmt.executeUpdate() > 0) {
            return "Medication deleted successfully!";
        }

        throw new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "No medication found with ID: " + id);

    } catch (SQLException e) {
        if ("23000".equals(e.getSQLState())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Medication may be referenced by schedules or slots");
        }

        throw new ResponseStatusException(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Could not delete medication");
    }
}


    // ---------------- MEDICATION SCHEDULES ----------------

    // Convert a medication schedule database row into an API response
    
private Map<String, Object> scheduleResponse(ResultSet rs)
        throws SQLException {

    Map<String, Object> result = new LinkedHashMap<>();

    // Schedule details
    result.put("scheduleId", rs.getInt("schedule_id"));
    result.put("medicationId", rs.getInt("medication_id"));
    result.put("slotId", rs.getInt("slot_id"));

    Time scheduledTime = rs.getTime("scheduled_time");
    result.put("scheduledTime",
            scheduledTime == null ? null : scheduledTime.toString());

    result.put("frequency", rs.getString("frequency"));

    Date startDate = rs.getDate("start_date");
    result.put("startDate",
            startDate == null ? null : startDate.toString());

    Date endDate = rs.getDate("end_date");
    result.put("endDate",
            endDate == null ? null : endDate.toString());

    result.put("isActive", rs.getBoolean("is_active"));

    // Elderly person and medication details
    result.put("elderlyName", rs.getString("elderly_name"));
    result.put("medicationName", rs.getString("medication_name"));
    result.put("dosage", rs.getString("dosage"));
    result.put("instructions", rs.getString("instructions"));

    // Medication slot details
    result.put("slotNumber", rs.getObject("slot_number"));
    result.put("capacity", rs.getObject("capacity"));
    result.put("currentStock", rs.getObject("current_stock"));

    return result;
}


    // Validate the schedule details received from the client
    private void validateSchedule(MedicationSchedule schedule) {
        if (schedule == null
                || schedule.getMedicationId() <= 0
                || schedule.getSlotId() <= 0
                || schedule.getScheduledTime() == null
                || schedule.getFrequency() == null
                || schedule.getFrequency().trim().isEmpty()
                || schedule.getStartDate() == null) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Medication ID, slot ID, time, frequency and start date are required");
        }

        if (schedule.getEndDate() != null
                && schedule.getEndDate().before(schedule.getStartDate())) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "End date cannot be before start date");
        }
    }

    // GET ALL medication schedules
    
@GetMapping("/api/medication-schedules")
public List<Map<String, Object>> getMedicationSchedules()
        throws SQLException {

    List<Map<String, Object>> schedules = new ArrayList<>();

    String sql =
            "SELECT " +
            "ms.schedule_id, " +
            "ms.medication_id, " +
            "ms.slot_id, " +
            "ms.scheduled_time, " +
            "ms.frequency, " +
            "ms.start_date, " +
            "ms.end_date, " +
            "ms.is_active, " +
            "eu.full_name AS elderly_name, " +
            "m.medication_name, " +
            "m.dosage, " +
            "m.instructions, " +
            "s.slot_number, " +
            "s.capacity, " +
            "s.current_stock " +
            "FROM medication_schedules ms " +
            "LEFT JOIN medications m " +
            "ON ms.medication_id = m.medication_id " +
            "LEFT JOIN elderly_users eu " +
            "ON m.elderly_id = eu.elderly_id " +
            "LEFT JOIN medication_slots s " +
            "ON ms.slot_id = s.slot_id " +
            "ORDER BY ms.schedule_id";

    try (Connection con = dataSource.getConnection();
         Statement stmt = con.createStatement();
         ResultSet rs = stmt.executeQuery(sql)) {

        while (rs.next()) {
            schedules.add(scheduleResponse(rs));
        }
    }

    return schedules;
}


    // GET a single medication schedule by ID
    @GetMapping("/api/medication-schedules/{id}")
    public Map<String, Object> getMedicationSchedule(
            @PathVariable int id) throws SQLException {

        String sql = "SELECT schedule_id, medication_id, slot_id, "
                + "scheduled_time, frequency, start_date, end_date, "
                + "is_active FROM medication_schedules "
                + "WHERE schedule_id = ?";

        try (Connection con = dataSource.getConnection();
             PreparedStatement stmt = con.prepareStatement(sql)) {

            stmt.setInt(1, id);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return scheduleResponse(rs);
                }
            }
        }

        throw new ResponseStatusException(
                HttpStatus.NOT_FOUND, "Medication schedule not found");
    }

    // ADD a new medication schedule
    @PostMapping("/api/medication-schedules")
    public String addMedicationSchedule(
            @RequestBody MedicationSchedule schedule) {

        validateSchedule(schedule);

        String sql = "INSERT INTO medication_schedules "
                + "(medication_id, slot_id, scheduled_time, frequency, "
                + "start_date, end_date, is_active) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?)";

        try (Connection con = dataSource.getConnection();
             PreparedStatement stmt = con.prepareStatement(sql)) {

            stmt.setInt(1, schedule.getMedicationId());
            stmt.setInt(2, schedule.getSlotId());
            stmt.setTime(3, schedule.getScheduledTime());
            stmt.setString(4, schedule.getFrequency());
            stmt.setDate(5, schedule.getStartDate());

            if (schedule.getEndDate() == null) {
                stmt.setNull(6, java.sql.Types.DATE);
            } else {
                stmt.setDate(6, schedule.getEndDate());
            }

            stmt.setBoolean(7, schedule.isActive());

            return stmt.executeUpdate() > 0
                    ? "Medication schedule added successfully!"
                    : "Failed to add medication schedule.";

        } catch (SQLException e) {
            if ("23000".equals(e.getSQLState())) {
                throw new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        "Check that medicationId and slotId refer to existing records");
            }

            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Could not add medication schedule");
        }
    }

    // UPDATE an existing medication schedule
    @PutMapping("/api/medication-schedules/{id}")
    public String updateMedicationSchedule(
            @PathVariable int id,
            @RequestBody MedicationSchedule schedule) {

        validateSchedule(schedule);

        String sql = "UPDATE medication_schedules SET "
                + "medication_id = ?, slot_id = ?, scheduled_time = ?, "
                + "frequency = ?, start_date = ?, end_date = ?, "
                + "is_active = ? WHERE schedule_id = ?";

        try (Connection con = dataSource.getConnection();
             PreparedStatement stmt = con.prepareStatement(sql)) {

            stmt.setInt(1, schedule.getMedicationId());
            stmt.setInt(2, schedule.getSlotId());
            stmt.setTime(3, schedule.getScheduledTime());
            stmt.setString(4, schedule.getFrequency());
            stmt.setDate(5, schedule.getStartDate());

            if (schedule.getEndDate() == null) {
                stmt.setNull(6, java.sql.Types.DATE);
            } else {
                stmt.setDate(6, schedule.getEndDate());
            }

            stmt.setBoolean(7, schedule.isActive());
            stmt.setInt(8, id);

           int rowsUpdated = stmt.executeUpdate();

if (rowsUpdated > 0) {
    return "Medication schedule updated successfully!";
} else {
    throw new ResponseStatusException(
            HttpStatus.NOT_FOUND,
            "No medication schedule found with ID: " + id
    );
}

        } catch (SQLException e) {
            if ("23000".equals(e.getSQLState())) {
                throw new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        "Check that medicationId and slotId refer to existing records");
            }

          throw new ResponseStatusException(
    HttpStatus.NOT_FOUND,
    "No medication schedule found with ID: " + id
);
        }
    }

    // DELETE a medication schedule by ID
    @DeleteMapping("/api/medication-schedules/{id}")
    public String deleteMedicationSchedule(@PathVariable int id) {
        String sql = "DELETE FROM medication_schedules "
                + "WHERE schedule_id = ?";

        try (Connection con = dataSource.getConnection();
             PreparedStatement stmt = con.prepareStatement(sql)) {

            stmt.setInt(1, id);

           int rowsDeleted = stmt.executeUpdate();

if (rowsDeleted > 0) {
    return "Medication schedule deleted successfully!";
} else {
    throw new ResponseStatusException(
            HttpStatus.NOT_FOUND,
            "No medication schedule found with ID: " + id
    );
}

        } catch (SQLException e) {
            if ("23000".equals(e.getSQLState())) {
                throw new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        "Medication schedule may be referenced by other records");
            }

            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Could not delete medication schedule");
        }
    }
    
    // ---------------- MEDICATION SLOTS ----------------

    // Convert a medication slot database row into an API response
    private Map<String, Object> medicationSlotResponse(ResultSet rs)
            throws SQLException {

        Map<String, Object> result = new LinkedHashMap<>();

        result.put("slotId", rs.getInt("slot_id"));
        result.put("elderlyId", rs.getInt("elderly_id"));
        result.put("medicationId", rs.getInt("medication_id"));
        result.put("slotNumber", rs.getInt("slot_number"));
        result.put("capacity", rs.getInt("capacity"));
        result.put("currentStock", rs.getInt("current_stock"));

        return result;
    }

    // Validate medication slot details
    private void validateMedicationSlot(MedicationSlot slot) {
        if (slot == null
                || slot.getElderlyId() <= 0
                || slot.getMedicationId() <= 0
                || slot.getSlotNumber() <= 0
                || slot.getCapacity() <= 0
                || slot.getCurrentStock() < 0
                || slot.getCurrentStock() > slot.getCapacity()) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Provide valid elderlyId, medicationId, slotNumber, "
                            + "capacity and currentStock");
        }
    }

    // GET ALL medication slots
    @GetMapping("/api/medication-slots")
    public List<Map<String, Object>> getMedicationSlots()
            throws SQLException {

        List<Map<String, Object>> slots = new ArrayList<>();

        String sql = "SELECT slot_id, elderly_id, medication_id, "
                + "slot_number, capacity, current_stock "
                + "FROM medication_slots";

        try (Connection con = dataSource.getConnection();
             Statement stmt = con.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                slots.add(medicationSlotResponse(rs));
            }
        }

        return slots;
    }

    // GET one medication slot by ID
    @GetMapping("/api/medication-slots/{id}")
    public Map<String, Object> getMedicationSlot(
            @PathVariable int id) throws SQLException {

        String sql = "SELECT slot_id, elderly_id, medication_id, "
                + "slot_number, capacity, current_stock "
                + "FROM medication_slots WHERE slot_id = ?";

        try (Connection con = dataSource.getConnection();
             PreparedStatement stmt = con.prepareStatement(sql)) {

            stmt.setInt(1, id);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return medicationSlotResponse(rs);
                }
            }
        }

        throw new ResponseStatusException(
                HttpStatus.NOT_FOUND, "Medication slot not found");
    }

    // ADD a medication slot
    @PostMapping("/api/medication-slots")
    public String addMedicationSlot(
            @RequestBody MedicationSlot slot) {

        validateMedicationSlot(slot);

        String sql = "INSERT INTO medication_slots "
                + "(elderly_id, medication_id, slot_number, "
                + "capacity, current_stock) VALUES (?, ?, ?, ?, ?)";

        try (Connection con = dataSource.getConnection();
             PreparedStatement stmt = con.prepareStatement(sql)) {

            stmt.setInt(1, slot.getElderlyId());
            stmt.setInt(2, slot.getMedicationId());
            stmt.setInt(3, slot.getSlotNumber());
            stmt.setInt(4, slot.getCapacity());
            stmt.setInt(5, slot.getCurrentStock());

            return stmt.executeUpdate() > 0
                    ? "Medication slot added successfully!"
                    : "Failed to add medication slot.";

        } catch (SQLException e) {
            if ("23000".equals(e.getSQLState())) {
                throw new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        "Check that elderlyId and medicationId "
                                + "refer to existing records");
            }

            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Could not add medication slot");
        }
    }

    // UPDATE a medication slot
    @PutMapping("/api/medication-slots/{id}")
    public String updateMedicationSlot(
            @PathVariable int id,
            @RequestBody MedicationSlot slot) {

        validateMedicationSlot(slot);

        String sql = "UPDATE medication_slots SET elderly_id = ?, "
                + "medication_id = ?, slot_number = ?, capacity = ?, "
                + "current_stock = ? WHERE slot_id = ?";

        try (Connection con = dataSource.getConnection();
             PreparedStatement stmt = con.prepareStatement(sql)) {

            stmt.setInt(1, slot.getElderlyId());
            stmt.setInt(2, slot.getMedicationId());
            stmt.setInt(3, slot.getSlotNumber());
            stmt.setInt(4, slot.getCapacity());
            stmt.setInt(5, slot.getCurrentStock());
            stmt.setInt(6, id);

            return stmt.executeUpdate() > 0
                    ? "Medication slot updated successfully!"
                    : "No medication slot found with ID: " + id;

        } catch (SQLException e) {
            if ("23000".equals(e.getSQLState())) {
                throw new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        "Check that elderlyId and medicationId "
                                + "refer to existing records");
            }

            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Could not update medication slot");
        }
    }

    // DELETE a medication slot
    @DeleteMapping("/api/medication-slots/{id}")
    public String deleteMedicationSlot(@PathVariable int id) {

        String sql = "DELETE FROM medication_slots WHERE slot_id = ?";

        try (Connection con = dataSource.getConnection();
             PreparedStatement stmt = con.prepareStatement(sql)) {

            stmt.setInt(1, id);

            return stmt.executeUpdate() > 0
                    ? "Medication slot deleted successfully!"
                    : "No medication slot found with ID: " + id;

        } catch (SQLException e) {
            if ("23000".equals(e.getSQLState())) {
                throw new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        "Medication slot may be referenced by schedules "
                                + "or dispensing records");
            }

            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Could not delete medication slot");
        }
    }
    
    // ---------------- DISPENSING EVENTS ----------------

    // GET ALL dispensing events
    @GetMapping("/api/dispensing-events")
    public List<Map<String, Object>> getDispensingEvents()
            throws SQLException {

        List<Map<String, Object>> events = new ArrayList<>();

        String sql = "SELECT dispensing_id, dose_id, slot_id, "
                + "dispensed_at, dispense_status, tablets_dispensed, "
                + "device_message FROM dispensing_events "
                + "ORDER BY dispensing_id";

        try (Connection con = dataSource.getConnection();
             Statement stmt = con.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                Map<String, Object> event = new LinkedHashMap<>();

                event.put("dispensingId",
                        rs.getInt("dispensing_id"));
                event.put("doseId",
                        rs.getInt("dose_id"));
                event.put("slotId",
                        rs.getInt("slot_id"));
                event.put("dispensedAt",
                        rs.getTimestamp("dispensed_at"));
                event.put("dispenseStatus",
                        rs.getString("dispense_status"));
                event.put("tabletsDispensed",
                        rs.getInt("tablets_dispensed"));
                event.put("deviceMessage",
                        rs.getString("device_message"));

                events.add(event);
            }
        }

       
        return events;
    }

    
    // ADD a dispensing event and update stock safely
    @PostMapping("/api/dispensing-events")
    public String addDispensingEvent(
            @RequestBody DispensingEvent event) {

        if (event == null
                || event.getDoseId() <= 0
                || event.getSlotId() <= 0
                || event.getTabletsDispensed() < 0) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Provide valid doseId, slotId and tabletsDispensed");
        }

        String status = event.getDispenseStatus() == null
                ? "PENDING"
                : event.getDispenseStatus().trim().toUpperCase();

        if (!status.equals("PENDING")
                && !status.equals("DISPENSED")
                && !status.equals("FAILED")) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Status must be PENDING, DISPENSED or FAILED");
        }

        if (status.equals("DISPENSED")
                && event.getTabletsDispensed() <= 0) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "A successful dispensing event must dispense at least one tablet");
        }

        String doseSql =
                "SELECT schedule_id FROM scheduled_doses "
                + "WHERE dose_id = ? FOR UPDATE";

        String slotCheckSql =
                "SELECT slot_id FROM medication_schedules "
                + "WHERE schedule_id = ?";

        String duplicateSql =
                "SELECT dispensing_id FROM dispensing_events "
                + "WHERE dose_id = ? AND dispense_status = 'DISPENSED' "
                + "LIMIT 1";

        String stockSql =
                "SELECT current_stock FROM medication_slots "
                + "WHERE slot_id = ? FOR UPDATE";

        String updateStockSql =
                "UPDATE medication_slots "
                + "SET current_stock = current_stock - ? "
                + "WHERE slot_id = ?";

        String historySql =
                "INSERT INTO stock_changes "
                + "(slot_id, quantity_changed, change_type, notes) "
                + "VALUES (?, ?, 'REMOVE', ?)";

        String insertSql =
                "INSERT INTO dispensing_events "
                + "(dose_id, slot_id, dispense_status, "
                + "tablets_dispensed, device_message) "
                + "VALUES (?, ?, ?, ?, ?)";
           String updateDoseSql =
        "UPDATE scheduled_doses "
        + "SET dose_status = 'DISPENSED' "
        + "WHERE dose_id = ?";
        try (Connection con = dataSource.getConnection()) {

            con.setAutoCommit(false);

            try {
                int scheduleId;

                // Lock the dose to help prevent duplicate processing
                try (PreparedStatement stmt =
                             con.prepareStatement(doseSql)) {

                    stmt.setInt(1, event.getDoseId());

                    try (ResultSet rs = stmt.executeQuery()) {
                        if (!rs.next()) {
                            throw new ResponseStatusException(
                                    HttpStatus.NOT_FOUND,
                                    "Scheduled dose not found");
                        }

                        scheduleId = rs.getInt("schedule_id");
                    }
                }

                // Ensure the dose belongs to the selected slot
                try (PreparedStatement stmt =
                             con.prepareStatement(slotCheckSql)) {

                    stmt.setInt(1, scheduleId);

                    try (ResultSet rs = stmt.executeQuery()) {
                        if (!rs.next()
                                || rs.getInt("slot_id")
                                != event.getSlotId()) {

                            throw new ResponseStatusException(
                                    HttpStatus.BAD_REQUEST,
                                    "Dose does not belong to the selected slot");
                        }
                    }
                }

                if (status.equals("DISPENSED")) {

                    // Reject another successful event for this dose
                    try (PreparedStatement stmt =
                                 con.prepareStatement(duplicateSql)) {

                        stmt.setInt(1, event.getDoseId());

                        try (ResultSet rs = stmt.executeQuery()) {
                            if (rs.next()) {
                                throw new ResponseStatusException(
                                        HttpStatus.CONFLICT,
                                        "This dose has already been dispensed");
                            }
                        }
                    }

                    int currentStock;

                    try (PreparedStatement stmt =
                                 con.prepareStatement(stockSql)) {

                        stmt.setInt(1, event.getSlotId());

                        try (ResultSet rs = stmt.executeQuery()) {
                            if (!rs.next()) {
                                throw new ResponseStatusException(
                                        HttpStatus.NOT_FOUND,
                                        "Medication slot not found");
                            }

                            currentStock = rs.getInt("current_stock");
                        }
                    }

                    if (currentStock < event.getTabletsDispensed()) {
                        throw new ResponseStatusException(
                                HttpStatus.BAD_REQUEST,
                                "Insufficient medication stock");
                    }

                    try (PreparedStatement stmt =
                                 con.prepareStatement(updateStockSql)) {

                        stmt.setInt(1, event.getTabletsDispensed());
                        stmt.setInt(2, event.getSlotId());
                        stmt.executeUpdate();
                    }

                    try (PreparedStatement stmt =
                                 con.prepareStatement(historySql)) {

                        stmt.setInt(1, event.getSlotId());
                        stmt.setInt(2, -event.getTabletsDispensed());
                        stmt.setString(
                                3,
                                "Dispensed for dose ID "
                                        + event.getDoseId());

                        stmt.executeUpdate();
                    }
                }

                // Save the event in the same transaction
                try (PreparedStatement stmt =
                             con.prepareStatement(insertSql)) {

                    stmt.setInt(1, event.getDoseId());
                    stmt.setInt(2, event.getSlotId());
                    stmt.setString(3, status);
                    stmt.setInt(4, event.getTabletsDispensed());
                    stmt.setString(5, event.getDeviceMessage());

                    stmt.executeUpdate();
                }
            if (status.equals("DISPENSED")) {
    try (PreparedStatement stmt =
                 con.prepareStatement(updateDoseSql)) {

        stmt.setInt(1, event.getDoseId());
        stmt.executeUpdate();
    }
}
                con.commit();
                return "Dispensing event saved successfully!";

            } catch (SQLException | RuntimeException e) {
                try {
                    con.rollback();
                } catch (SQLException rollbackError) {
                    e.addSuppressed(rollbackError);
                }

                throw e;
            }

        } catch (ResponseStatusException e) {
            throw e;

        } catch (SQLException e) {
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Could not save dispensing event");
        }
    }

    // ---------------- ACKNOWLEDGMENT EVENTS ----------------

    // GET ALL acknowledgment events
    @GetMapping("/api/acknowledgment-events")
    public List<Map<String, Object>> getAcknowledgmentEvents()
            throws SQLException {

        List<Map<String, Object>> events = new ArrayList<>();

        String sql = "SELECT acknowledgment_id, dose_id, "
                + "dispensing_id, acknowledged_at, "
                + "acknowledgment_method, acknowledgment_status "
                + "FROM acknowledgment_events "
                + "ORDER BY acknowledgment_id";

        try (Connection con = dataSource.getConnection();
             Statement stmt = con.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                Map<String, Object> event = new LinkedHashMap<>();

                event.put("acknowledgmentId",
                        rs.getInt("acknowledgment_id"));
                event.put("doseId",
                        rs.getInt("dose_id"));
                event.put("dispensingId",
                        rs.getInt("dispensing_id"));
                event.put("acknowledgedAt",
                        rs.getTimestamp("acknowledged_at"));
                event.put("acknowledgmentMethod",
                        rs.getString("acknowledgment_method"));
                event.put("acknowledgmentStatus",
                        rs.getString("acknowledgment_status"));

                events.add(event);
            }
        }

        return events;
    }

    // ADD a new acknowledgment event
    
    // ADD acknowledgment and resolve matching missed-dose alerts
    @PostMapping("/api/acknowledgment-events")
    public String addAcknowledgmentEvent(
            @RequestBody AcknowledgmentEvent event) {

        if (event == null
                || event.getDoseId() <= 0
                || event.getDispensingId() <= 0
                || event.getAcknowledgmentMethod() == null
                || event.getAcknowledgmentMethod().trim().isEmpty()
                || event.getAcknowledgmentStatus() == null
                || event.getAcknowledgmentStatus().trim().isEmpty()) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Provide valid doseId, dispensingId, "
                            + "acknowledgmentMethod and acknowledgmentStatus");
        }

        String insertSql = "INSERT INTO acknowledgment_events "
                + "(dose_id, dispensing_id, acknowledgment_method, "
                + "acknowledgment_status) VALUES (?, ?, ?, ?)";

        String updateSql = "UPDATE alerts "
                + "SET alert_status = 'RESOLVED', "
                + "resolved_at = CURRENT_TIMESTAMP "
                + "WHERE dose_id = ? "
                + "AND alert_type = 'MISSED_DOSE' "
                + "AND alert_status = 'OPEN'";

        try (Connection con = dataSource.getConnection()) {

            con.setAutoCommit(false);

            try {
                try (PreparedStatement stmt =
                             con.prepareStatement(insertSql)) {

                    stmt.setInt(1, event.getDoseId());
                    stmt.setInt(2, event.getDispensingId());
                    stmt.setString(3,
                            event.getAcknowledgmentMethod().trim());
                    stmt.setString(4,
                            event.getAcknowledgmentStatus().trim());

                    stmt.executeUpdate();
                }

                if ("ACKNOWLEDGED".equalsIgnoreCase(
                        event.getAcknowledgmentStatus().trim())) {

                    try (PreparedStatement stmt =
                                 con.prepareStatement(updateSql)) {

                        stmt.setInt(1, event.getDoseId());
                        stmt.executeUpdate();
                    }
                }

                con.commit();
                return "Acknowledgment saved successfully!";

            } catch (SQLException e) {
                con.rollback();
                throw e;
            }

        } catch (SQLException e) {
            if ("23000".equals(e.getSQLState())) {
                throw new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        "Check that doseId and dispensingId "
                                + "refer to existing records");
            }

            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Could not save acknowledgment");
        }
    }


    // ---------------- ALERTS ----------------

    // GET ALL alerts
    @GetMapping("/api/alerts")
    public List<Map<String, Object>> getAlerts()
            throws SQLException {

        List<Map<String, Object>> alerts = new ArrayList<>();

        String sql = "SELECT alert_id, elderly_id, dose_id, slot_id, "
                + "alert_type, alert_message, alert_status, "
                + "created_at, resolved_at "
                + "FROM alerts ORDER BY alert_id";

        try (Connection con = dataSource.getConnection();
             Statement stmt = con.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                Map<String, Object> alert = new LinkedHashMap<>();

                alert.put("alertId", rs.getInt("alert_id"));
                alert.put("elderlyId", rs.getInt("elderly_id"));

                int doseId = rs.getInt("dose_id");
                alert.put("doseId",
                        rs.wasNull() ? null : doseId);

                int slotId = rs.getInt("slot_id");
                alert.put("slotId",
                        rs.wasNull() ? null : slotId);

                alert.put("alertType", rs.getString("alert_type"));
                alert.put("alertMessage", rs.getString("alert_message"));
                alert.put("alertStatus", rs.getString("alert_status"));
                alert.put("createdAt", rs.getTimestamp("created_at"));
                alert.put("resolvedAt", rs.getTimestamp("resolved_at"));

                alerts.add(alert);
            }
        }

        return alerts;
    }

    // ADD a new alert
    @PostMapping("/api/alerts")
    public String addAlert(@RequestBody Alert alert) {

        if (alert == null
                || alert.getElderlyId() <= 0
                || alert.getAlertType() == null
                || alert.getAlertType().trim().isEmpty()
                || alert.getAlertMessage() == null
                || alert.getAlertMessage().trim().isEmpty()
                || alert.getAlertStatus() == null
                || alert.getAlertStatus().trim().isEmpty()) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Provide elderlyId, alertType, alertMessage and alertStatus");
        }

        String sql = "INSERT INTO alerts "
                + "(elderly_id, dose_id, slot_id, alert_type, "
                + "alert_message, alert_status) "
                + "VALUES (?, ?, ?, ?, ?, ?)";

        try (Connection con = dataSource.getConnection();
             PreparedStatement stmt = con.prepareStatement(sql)) {

            stmt.setInt(1, alert.getElderlyId());

            if (alert.getDoseId() <= 0) {
                stmt.setNull(2, java.sql.Types.INTEGER);
            } else {
                stmt.setInt(2, alert.getDoseId());
            }

            if (alert.getSlotId() <= 0) {
                stmt.setNull(3, java.sql.Types.INTEGER);
            } else {
                stmt.setInt(3, alert.getSlotId());
            }

            stmt.setString(4, alert.getAlertType());
            stmt.setString(5, alert.getAlertMessage());
            stmt.setString(6, alert.getAlertStatus());

            return stmt.executeUpdate() > 0
                    ? "Alert added successfully!"
                    : "Failed to add alert.";

        } catch (SQLException e) {
            if ("23000".equals(e.getSQLState())) {
                throw new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        "Check that elderlyId, doseId and slotId "
                                + "refer to existing records");
            }

            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Could not add alert");
        }
    }

    // UPDATE an alert's status
    @PutMapping("/api/alerts/{alertId}/status")
    public String updateAlertStatus(
            @PathVariable int alertId,
            @RequestBody Map<String, String> request) {

        String status = request.get("alertStatus");

        if (alertId <= 0
                || status == null
                || status.trim().isEmpty()) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Provide a valid alertId and alertStatus");
        }

        String sql = "UPDATE alerts SET alert_status = ?, "
                + "resolved_at = CASE "
                + "WHEN ? = 'RESOLVED' THEN CURRENT_TIMESTAMP "
                + "ELSE NULL END "
                + "WHERE alert_id = ?";

        try (Connection con = dataSource.getConnection();
             PreparedStatement stmt = con.prepareStatement(sql)) {

            stmt.setString(1, status.trim().toUpperCase());
            stmt.setString(2, status.trim().toUpperCase());
            stmt.setInt(3, alertId);

            if (stmt.executeUpdate() > 0) {
                return "Alert status updated successfully!";
            }

            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Alert not found");

        } catch (SQLException e) {
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Could not update alert status");
        }
    }

    // ---------------- SCHEDULED DOSES ----------------

    // GET ALL scheduled doses
    @GetMapping("/api/scheduled-doses")
    public List<Map<String, Object>> getScheduledDoses()
            throws SQLException {

        List<Map<String, Object>> doses = new ArrayList<>();

        String sql = "SELECT dose_id, schedule_id, due_time, "
                + "dose_status, created_at "
                + "FROM scheduled_doses ORDER BY dose_id";

        try (Connection con = dataSource.getConnection();
             Statement stmt = con.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                Map<String, Object> dose = new LinkedHashMap<>();

                dose.put("doseId", rs.getInt("dose_id"));
                dose.put("scheduleId", rs.getInt("schedule_id"));
                dose.put("dueTime", rs.getTimestamp("due_time"));
                dose.put("doseStatus", rs.getString("dose_status"));
                dose.put("createdAt", rs.getTimestamp("created_at"));

                doses.add(dose);
            }
        }

        return doses;
    }

    // UPDATE scheduled dose status
    @PutMapping("/api/scheduled-doses/{doseId}/status")
    public String updateScheduledDoseStatus(
            @PathVariable int doseId,
            @RequestBody Map<String, String> request) {

        String status = request.get("doseStatus");

        if (doseId <= 0
                || status == null
                || status.trim().isEmpty()) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Provide a valid doseId and doseStatus");
        }

        String sql = "UPDATE scheduled_doses "
                + "SET dose_status = ? WHERE dose_id = ?";

        try (Connection con = dataSource.getConnection();
             PreparedStatement stmt = con.prepareStatement(sql)) {

            stmt.setString(1, status.trim().toUpperCase());
            stmt.setInt(2, doseId);

            if (stmt.executeUpdate() > 0) {
                return "Scheduled dose status updated successfully!";
            }

            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Scheduled dose not found");

        } catch (SQLException e) {
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Could not update scheduled dose status");
        }
    }
// ---------------- STOCK MANAGEMENT ----------------

// GET ALL medication slot stock
@GetMapping("/api/stock")
public List<Map<String, Object>> getStock()
        throws SQLException {

    List<Map<String, Object>> stocks = new ArrayList<>();

    String sql = "SELECT slot_id, elderly_id, medication_id, "
            + "slot_number, capacity, current_stock "
            + "FROM medication_slots ORDER BY slot_id";

    try (Connection con = dataSource.getConnection();
         Statement stmt = con.createStatement();
         ResultSet rs = stmt.executeQuery(sql)) {

        while (rs.next()) {
            Map<String, Object> stock = new LinkedHashMap<>();

            stock.put("slotId", rs.getInt("slot_id"));
            stock.put("elderlyId", rs.getInt("elderly_id"));
            stock.put("medicationId", rs.getInt("medication_id"));
            stock.put("slotNumber", rs.getInt("slot_number"));
            stock.put("capacity", rs.getInt("capacity"));
            stock.put("currentStock", rs.getInt("current_stock"));

            stocks.add(stock);
        }
    }

    return stocks;
}
// ADD OR REMOVE STOCK
@PostMapping("/api/stock/adjust")
public String adjustStock(
        @RequestBody Map<String, Object> request) {

    Object slotValue = request.get("slotId");
    Object quantityValue = request.get("quantityChanged");
    Object typeValue = request.get("changeType");

    if (slotValue == null
            || quantityValue == null
            || typeValue == null) {
        throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Provide slotId, quantityChanged and changeType");
    }

    int slotId;
    int quantity;
    String type = typeValue.toString().trim().toUpperCase();

    try {
        slotId = Integer.parseInt(slotValue.toString());
        quantity = Integer.parseInt(quantityValue.toString());
    } catch (NumberFormatException e) {
        throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "slotId and quantityChanged must be integers");
    }

    if (slotId <= 0 || quantity <= 0
            || !type.equals("ADD")
            && !type.equals("REMOVE")) {
        throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Use a valid slotId, positive quantity, and ADD or REMOVE");
    }

    String notes = request.get("notes") == null
            ? null : request.get("notes").toString();

    String selectSql =
            "SELECT capacity, current_stock FROM medication_slots "
            + "WHERE slot_id = ? FOR UPDATE";

    String updateSql =
            "UPDATE medication_slots SET current_stock = ? "
            + "WHERE slot_id = ?";

    String insertSql =
            "INSERT INTO stock_changes "
            + "(slot_id, quantity_changed, change_type, notes) "
            + "VALUES (?, ?, ?, ?)";

    try (Connection con = dataSource.getConnection()) {
        con.setAutoCommit(false);

        try {
            int capacity;
            int currentStock;

            try (PreparedStatement stmt =
                         con.prepareStatement(selectSql)) {
                stmt.setInt(1, slotId);

                try (ResultSet rs = stmt.executeQuery()) {
                    if (!rs.next()) {
                        throw new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "Medication slot not found");
                    }

                    capacity = rs.getInt("capacity");
                    currentStock = rs.getInt("current_stock");
                }
            }

            int newStock = type.equals("ADD")
                    ? currentStock + quantity
                    : currentStock - quantity;

            if (newStock < 0 || newStock > capacity) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Stock must remain between 0 and capacity ("
                                + capacity + ")");
            }

            try (PreparedStatement stmt =
                         con.prepareStatement(updateSql)) {
                stmt.setInt(1, newStock);
                stmt.setInt(2, slotId);
                stmt.executeUpdate();
            }

            try (PreparedStatement stmt =
                         con.prepareStatement(insertSql)) {
                stmt.setInt(1, slotId);
                stmt.setInt(2, type.equals("ADD")
                        ? quantity : -quantity);
                stmt.setString(3, type);
                stmt.setString(4, notes);
                stmt.executeUpdate();
            }

            con.commit();
            return "Stock adjusted successfully! New stock: " + newStock;

        } catch (SQLException | RuntimeException e) {
            con.rollback();
            throw e;
        }

    } catch (SQLException e) {
        throw new ResponseStatusException(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Could not adjust stock");
    }
}

@GetMapping("/api/devices")
public ResponseEntity<?> getDevices() {
    String sql = "SELECT * FROM devices";

    try (Connection con = dataSource.getConnection();
         PreparedStatement stmt = con.prepareStatement(sql);
         ResultSet rs = stmt.executeQuery()) {

        List<Device> devices = new ArrayList<>();

        while (rs.next()) {
            Device device = new Device();

            device.setDeviceId(rs.getInt("device_id"));
            device.setElderlyId(rs.getInt("elderly_id"));
            device.setDeviceName(rs.getString("device_name"));
            device.setDeviceCode(rs.getString("device_code"));
            device.setDeviceStatus(rs.getString("device_status"));

            Timestamp lastSeen = rs.getTimestamp("last_seen");
            device.setLastSeen(lastSeen);

            Timestamp registeredAt = rs.getTimestamp("registered_at");
            device.setRegisteredAt(registeredAt);

            devices.add(device);
        }

        return ResponseEntity.ok(devices);

    } catch (SQLException e) {
        return ResponseEntity.status(500)
                .body("Failed to retrieve devices: " + e.getMessage());
    }
}

@PostMapping("/api/devices")
public ResponseEntity<?> addDevice(@RequestBody Device device) {

    String sql = "INSERT INTO devices "
            + "(elderly_id, device_name, device_code, device_status) "
            + "VALUES (?, ?, ?, ?)";

    if (device.getDeviceName() == null
            || device.getDeviceName().trim().isEmpty()
            || device.getDeviceCode() == null
            || device.getDeviceCode().trim().isEmpty()) {
        return ResponseEntity.badRequest()
                .body("Device name and device code are required.");
    }

    try (Connection con = dataSource.getConnection();
         PreparedStatement stmt = con.prepareStatement(
                 sql, java.sql.Statement.RETURN_GENERATED_KEYS)) {

        stmt.setInt(1, device.getElderlyId());
        stmt.setString(2, device.getDeviceName().trim());
        stmt.setString(3, device.getDeviceCode().trim());
        stmt.setString(4, "OFFLINE");

        stmt.executeUpdate();

        try (ResultSet keys = stmt.getGeneratedKeys()) {
            if (keys.next()) {
                return ResponseEntity.status(201)
                        .body("Device registered successfully! Device ID: "
                                + keys.getInt(1));
            }
        }

        return ResponseEntity.status(500)
                .body("Device was inserted, but its ID could not be retrieved.");

    } catch (SQLException e) {
        if ("23000".equals(e.getSQLState())) {
            return ResponseEntity.status(409)
                    .body("Device code already exists or elderly ID is invalid.");
        }

        return ResponseEntity.status(500)
                .body("Failed to register device: " + e.getMessage());
    }
}

@PutMapping("/api/devices/{id}/status")
public ResponseEntity<?> updateDeviceStatus(
        @PathVariable int id,
        @RequestBody Map<String, String> body) {

    String status = body.get("deviceStatus");

    if (status == null
            || !(status.equals("ONLINE") || status.equals("OFFLINE"))) {
        return ResponseEntity.badRequest()
                .body("deviceStatus must be ONLINE or OFFLINE.");
    }

    String sql = "UPDATE devices "
            + "SET device_status = ?, last_seen = NOW() "
            + "WHERE device_id = ?";

    try (Connection con = dataSource.getConnection();
         PreparedStatement stmt = con.prepareStatement(sql)) {

        stmt.setString(1, status);
        stmt.setInt(2, id);

        int rows = stmt.executeUpdate();

        if (rows == 0) {
            return ResponseEntity.status(404)
                    .body("Device not found.");
        }

        return ResponseEntity.ok("Device status updated to " + status);

    } catch (SQLException e) {
        return ResponseEntity.status(500)
                .body("Failed to update device: " + e.getMessage());
    }
}

@GetMapping("/api/devices/{id}")
public ResponseEntity<?> getDeviceById(@PathVariable int id) {

    String sql = "SELECT * FROM devices WHERE device_id = ?";

    try (Connection con = dataSource.getConnection();
         PreparedStatement stmt = con.prepareStatement(sql)) {

        stmt.setInt(1, id);

        try (ResultSet rs = stmt.executeQuery()) {
            if (rs.next()) {
                Device device = new Device();

                device.setDeviceId(rs.getInt("device_id"));
                device.setElderlyId(rs.getInt("elderly_id"));
                device.setDeviceName(rs.getString("device_name"));
                device.setDeviceCode(rs.getString("device_code"));
                device.setDeviceStatus(rs.getString("device_status"));
                device.setLastSeen(rs.getTimestamp("last_seen"));
                device.setRegisteredAt(rs.getTimestamp("registered_at"));

                return ResponseEntity.ok(device);
            }

            return ResponseEntity.status(404)
                    .body("Device not found.");
        }

    } catch (SQLException e) {
        return ResponseEntity.status(500)
                .body("Failed to retrieve device: " + e.getMessage());
    }
}

@PutMapping("/api/devices/{id}")
public ResponseEntity<?> updateDevice(
        @PathVariable int id,
        @RequestBody Device device) {

    if (device.getDeviceName() == null
            || device.getDeviceName().trim().isEmpty()
            || device.getDeviceCode() == null
            || device.getDeviceCode().trim().isEmpty()
            || device.getElderlyId() <= 0) {
        return ResponseEntity.badRequest()
                .body("Valid elderly ID, device name, and device code are required.");
    }

    String sql = "UPDATE devices "
            + "SET elderly_id = ?, device_name = ?, device_code = ? "
            + "WHERE device_id = ?";

    try (Connection con = dataSource.getConnection();
         PreparedStatement stmt = con.prepareStatement(sql)) {

        stmt.setInt(1, device.getElderlyId());
        stmt.setString(2, device.getDeviceName().trim());
        stmt.setString(3, device.getDeviceCode().trim());
        stmt.setInt(4, id);

        int rows = stmt.executeUpdate();

        if (rows == 0) {
            return ResponseEntity.status(404)
                    .body("Device not found.");
        }

        return ResponseEntity.ok("Device details updated successfully!");

    } catch (SQLException e) {
        if ("23000".equals(e.getSQLState())) {
            return ResponseEntity.status(409)
                    .body("Device code already exists or elderly ID is invalid.");
        }

        return ResponseEntity.status(500)
                .body("Failed to update device: " + e.getMessage());
    }
}

@DeleteMapping("/api/devices/{id}")
public ResponseEntity<?> deleteDevice(@PathVariable int id) {

    String sql = "DELETE FROM devices WHERE device_id = ?";

    try (Connection con = dataSource.getConnection();
         PreparedStatement stmt = con.prepareStatement(sql)) {

        stmt.setInt(1, id);

        int rows = stmt.executeUpdate();

        if (rows == 0) {
            return ResponseEntity.status(404)
                    .body("Device not found.");
        }

        return ResponseEntity.ok("Device deleted successfully!");

    } catch (SQLException e) {
        if ("23000".equals(e.getSQLState())) {
            return ResponseEntity.status(409)
                    .body("Device cannot be deleted because related records exist.");
        }

        return ResponseEntity.status(500)
                .body("Failed to delete device: " + e.getMessage());
    }
}

}

