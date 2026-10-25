#include <Wire.h>
#include <RTClib.h>
#include <ESP32Servo.h>

RTC_DS1307 rtc;

Servo servoSlot1;
Servo servoSlot2;

const int servoPin1 = 18;
const int servoPin2 = 23;
const int buttonPin = 19;

const int MAX_EVENTS = 10;
const unsigned long DROP_TIMEOUT = 60000UL;       // 1 minute
const unsigned long COLLECTION_TIMEOUT = 1800000UL; // 30 minutes

// Temporary schedules: 24-hour format
struct DoseSchedule {
  int hour;
  int minute;
  bool slot1;
  bool slot2;
};

DoseSchedule schedules[] = {
  {12, 41, true, true},    // Morning: both slots
  {12, 43, true, false},  // Afternoon: Slot 1
  {20, 0, true, true}    // Evening: both slots
};

const int scheduleCount =
    sizeof(schedules) / sizeof(schedules[0]);

// Per-event, per-slot status
bool dispensed[MAX_EVENTS][2] = {};
bool acknowledged[MAX_EVENTS][2] = {};
bool dropDetected[MAX_EVENTS][2] = {};
bool dropFailureAlerted[MAX_EVENTS][2] = {};
bool collectionOverdueAlerted[MAX_EVENTS][2] = {};
bool missedDoseAlerted[MAX_EVENTS][2]={};

unsigned long dispenseTime[MAX_EVENTS][2] = {};
unsigned long dropTime[MAX_EVENTS][2] = {};

// Simulated stock values; change these using Serial commands.
int stock[2] = {10, 10};

// Shared collection cup simulation
bool cupOccupied = false;
int tabletsInCup = 0;
bool collectionAlertSent = false;
unsigned long firstDropTime = 0;

int lastDispenseDate = -1;
bool buttonWasPressed = false;

// --------------------------------------------------
// Servo functions
// --------------------------------------------------

void openSlot(int slot) {
  if (slot == 0) {
    Serial.println("Opening Slot 1...");
    servoSlot1.write(90);
  } else {
    Serial.println("Opening Slot 2...");
    servoSlot2.write(90);
  }
}

void closeSlot(int slot) {
  if (slot == 0) {
    servoSlot1.write(0);
  } else {
    servoSlot2.write(0);
  }
}

// --------------------------------------------------
// Dispensing
// --------------------------------------------------

void dispenseSlot(int eventIndex, int slot) {
  if (stock[slot] <= 0) {
    Serial.print("WARNING: Slot ");
    Serial.print(slot + 1);
    Serial.println(" has no stock. Dispensing skipped.");
    return;
  }

  openSlot(slot);
  delay(500);
  closeSlot(slot);

  dispensed[eventIndex][slot] = true;
  acknowledged[eventIndex][slot] = false;
  dropDetected[eventIndex][slot] = false;
  dropFailureAlerted[eventIndex][slot] = false;
  collectionOverdueAlerted[eventIndex][slot] = false;

  dispenseTime[eventIndex][slot] = millis();

  Serial.print("Slot ");
  Serial.print(slot + 1);
  Serial.println(" gate activated.");

  Serial.println(
      "Simulate the tablet reaching the cup with DROP1 or DROP2.");
}

// --------------------------------------------------
// Acknowledgment
// --------------------------------------------------

void acknowledgeSlot(int slot) {
  for (int i = 0; i < scheduleCount; i++) {
    if (dispensed[i][slot] && !acknowledged[i][slot]) {
      acknowledged[i][slot] = true;

      Serial.print("Acknowledged: Slot ");
      Serial.println(slot + 1);
      return;
    }
  }

  Serial.print("No pending acknowledgment for Slot ");
  Serial.println(slot + 1);
}

void acknowledgeNextDose() {
  for (int i = 0; i < scheduleCount; i++) {
    for (int slot = 0; slot < 2; slot++) {
      if (dispensed[i][slot] && !acknowledged[i][slot]) {
        acknowledgeSlot(slot);
        return;
      }
    }
  }

  Serial.println("No pending dose to acknowledge.");
}

// --------------------------------------------------
// Simulated sensor events
// --------------------------------------------------

void simulateDrop(int slot) {
  int eventIndex = -1;

  // Find the earliest dose for this slot that has not
  // yet registered a tablet drop.
  for (int i = 0; i < scheduleCount; i++) {
    if (dispensed[i][slot] && !dropDetected[i][slot]) {
      eventIndex = i;
      break;
    }
  }

  if (eventIndex == -1) {
    Serial.print("No pending tablet drop for Slot ");
    Serial.println(slot + 1);
    return;
  }

  dropDetected[eventIndex][slot] = true;
  dropTime[eventIndex][slot] = millis();

  if (stock[slot] > 0) {
    stock[slot]--;
  }

  if (!cupOccupied) {
    firstDropTime = millis();
    collectionAlertSent = false;
  }

  cupOccupied = true;
  tabletsInCup++;

  Serial.print("SIMULATION: Tablet from Slot ");
  Serial.print(slot + 1);
  Serial.println(" detected in shared cup.");

  Serial.print("Simulated stock in Slot ");
  Serial.print(slot + 1);
  Serial.print(": ");
  Serial.println(stock[slot]);

  if (stock[slot] <= 2) {
    Serial.print("LOW STOCK WARNING: Slot ");
    Serial.println(slot + 1);
  }
}

void simulateTaken() {
  if (!cupOccupied) {
    Serial.println("Cup is already empty.");
    return;
  }

  Serial.println(
      "SIMULATION: Shared cup emptied by patient.");

  Serial.println(
      "Collection event recorded for all tablets currently in cup.");

  cupOccupied = false;
  tabletsInCup = 0;
  collectionAlertSent = false;
}

// --------------------------------------------------
// Daily reset
// --------------------------------------------------

void resetDailyStatus() {
  for (int i = 0; i < scheduleCount; i++) {
    for (int slot = 0; slot < 2; slot++) {
      dispensed[i][slot] = false;
      acknowledged[i][slot] = false;
      dropDetected[i][slot] = false;
      dropFailureAlerted[i][slot] = false;
      collectionOverdueAlerted[i][slot] = false;
      missedDoseAlerted[i][slot] = false;

      dispenseTime[i][slot] = 0;
      dropTime[i][slot] = 0;
    }
  }

  cupOccupied = false;
  tabletsInCup = 0;
  collectionAlertSent = false;

  Serial.println("New day: daily dose statuses reset.");
}

// --------------------------------------------------
// Status report
// --------------------------------------------------

void printStatus() {
  Serial.println("\n--- DISPENSER STATUS ---");

  Serial.print("Slot 1 simulated stock: ");
  Serial.println(stock[0]);

  Serial.print("Slot 2 simulated stock: ");
  Serial.println(stock[1]);

  Serial.print("Shared cup: ");
  Serial.println(cupOccupied ? "OCCUPIED" : "EMPTY");

  Serial.print("Simulated tablets in cup: ");
  Serial.println(tabletsInCup);

  for (int i = 0; i < scheduleCount; i++) {
    for (int slot = 0; slot < 2; slot++) {
      if (dispensed[i][slot]) {
        Serial.print("Event ");
        Serial.print(i + 1);
        Serial.print(", Slot ");
        Serial.print(slot + 1);
        Serial.print(": ");

        Serial.print(acknowledged[i][slot]
                         ? "ACKNOWLEDGED"
                         : "NOT ACKNOWLEDGED");

        Serial.print(", Drop: ");

        Serial.println(dropDetected[i][slot]
                           ? "DETECTED"
                           : "NOT DETECTED");
      }
    }
  }

  Serial.println("------------------------\n");
}

// --------------------------------------------------
// Serial command processing
// --------------------------------------------------

void processCommand(String command) {
  command.trim();
  command.toUpperCase();

  if (command == "DROP1") {
    simulateDrop(0);
  }
  else if (command == "DROP2") {
    simulateDrop(1);
  }
  else if (command == "TAKEN") {
    simulateTaken();
  }
  else if (command == "EMPTY1") {
    stock[0] = 0;
    Serial.println("SIMULATION: Slot 1 is empty.");
  }
  else if (command == "EMPTY2") {
    stock[1] = 0;
    Serial.println("SIMULATION: Slot 2 is empty.");
  }
  else if (command.startsWith("STOCK1 ")) {
    int value = command.substring(7).toInt();
stock[0] = (value < 0) ? 0 : value;
    Serial.print("Simulated Slot 1 stock set to ");
    Serial.println(stock[0]);
  }
  else if (command.startsWith("STOCK2 ")) {
    int value = command.substring(7).toInt();
stock[1] = (value < 0) ? 0 : value;
    Serial.print("Simulated Slot 2 stock set to ");
    Serial.println(stock[1]);
  }
  else if (command == "ACK1") {
    acknowledgeSlot(0);
  }
  else if (command == "ACK2") {
    acknowledgeSlot(1);
  }
  else if (command == "ACK") {
    acknowledgeNextDose();
  }
  else if (command == "STATUS") {
    printStatus();
  }
  else if (command == "HELP") {
    Serial.println("Available commands:");
    Serial.println("DROP1  - Simulate Slot 1 tablet drop");
    Serial.println("DROP2  - Simulate Slot 2 tablet drop");
    Serial.println("TAKEN  - Simulate emptying the shared cup");
    Serial.println("EMPTY1 - Simulate empty Slot 1");
    Serial.println("EMPTY2 - Simulate empty Slot 2");
    Serial.println("STOCK1 n - Set simulated Slot 1 stock");
    Serial.println("STOCK2 n - Set simulated Slot 2 stock");
    Serial.println("ACK1   - Acknowledge Slot 1");
    Serial.println("ACK2   - Acknowledge Slot 2");
    Serial.println("ACK    - Acknowledge next pending slot");
    Serial.println("STATUS - Display status");
  }
  else {
    Serial.println("Unknown command. Type HELP.");
  }
}

// --------------------------------------------------
// Setup
// --------------------------------------------------

void setup() {
  Serial.begin(115200);

  pinMode(buttonPin, INPUT_PULLUP);

  servoSlot1.attach(servoPin1);
  servoSlot2.attach(servoPin2);

  servoSlot1.write(0);
  servoSlot2.write(0);

  Wire.begin(21, 22);

  if (!rtc.begin()) {
    Serial.println("RTC not found! Check wiring.");

    while (true) {
      delay(1000);
    }
  }

  if (!rtc.isrunning()) {
    Serial.println("WARNING: RTC is not running.");
    Serial.println("Set the RTC time before testing.");
  }

  if (scheduleCount > MAX_EVENTS) {
    Serial.println("ERROR: Too many schedule events.");

    while (true) {
      delay(1000);
    }
  }

  Serial.println("\nSmart Medication Dispenser Ready");
  Serial.println("Two servos and shared cup simulation enabled.");
  Serial.println("Type HELP in the Serial Monitor for commands.");
}

// --------------------------------------------------
// Main loop
// --------------------------------------------------

void loop() {
  DateTime now = rtc.now();

  int today = now.year() * 10000
              + now.month() * 100
              + now.day();

  if (lastDispenseDate != today) {
    resetDailyStatus();
    lastDispenseDate = today;
  }

  // Read commands entered in the Serial Monitor.
  if (Serial.available()) {
    String command = Serial.readStringUntil('\n');
    processCommand(command);
  }

  // Check each scheduled event.
  for (int i = 0; i < scheduleCount; i++) {
    if (now.hour() == schedules[i].hour &&
        now.minute() == schedules[i].minute) {

      if (schedules[i].slot1 && !dispensed[i][0]) {
        Serial.println("Scheduled dose for Slot 1.");
        dispenseSlot(i, 0);
      }

      if (schedules[i].slot2 && !dispensed[i][1]) {
        Serial.println("Scheduled dose for Slot 2.");
        dispenseSlot(i, 1);
      }
    }
  }

  // Button: acknowledge one pending slot-dose per press.
  bool buttonIsPressed = (digitalRead(buttonPin) == LOW);

  if (buttonIsPressed && !buttonWasPressed) {
    acknowledgeNextDose();
  }

  buttonWasPressed = buttonIsPressed;

  // Alert if no simulated tablet drop occurs within one minute.
  for (int i = 0; i < scheduleCount; i++) {
    for (int slot = 0; slot < 2; slot++) {
      if (dispensed[i][slot] &&
          !dropDetected[i][slot] &&
          !dropFailureAlerted[i][slot] &&
          millis() - dispenseTime[i][slot] >= DROP_TIMEOUT) {

        dropFailureAlerted[i][slot] = true;

        Serial.print("ALERT: No simulated tablet drop from Slot ");
        Serial.println(slot + 1);
      }
    }
  }

  // Alert if the shared cup has not been emptied for 30 minutes.
  if (cupOccupied &&
      !collectionAlertSent &&
      millis() - firstDropTime >= COLLECTION_TIMEOUT) {

    collectionAlertSent = true;

    Serial.println(
        "ALERT: Shared cup has not been emptied for 30 minutes.");

    Serial.println("Caregiver notification required.");
  }
  // Alert if a dose is not acknowledged within 30 minutes.
 for (int i = 0; i < scheduleCount; i++) {
  for (int slot = 0; slot < 2; slot++) {
    if (dispensed[i][slot] &&
        !acknowledged[i][slot] &&
        !missedDoseAlerted[i][slot] &&
        millis() - dispenseTime[i][slot] >= COLLECTION_TIMEOUT) {

      missedDoseAlerted[i][slot] = true;

      Serial.print("MISSED DOSE ALERT: Slot ");
      Serial.print(slot + 1);
      Serial.println(" was not acknowledged within 30 minutes.");

      Serial.println("Caregiver notification required.");
    }
  }
}
  delay(50);
}