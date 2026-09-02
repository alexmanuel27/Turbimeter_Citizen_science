/*
  ============================================================
   DFROBOT SEN0189 TURBIDITY SENSOR + ARDUINO NANO
   VERSION 1: NTU CALCULATION + INTERACTIVE SERIAL CALIBRATION
  ============================================================
  Description:
   Reads the turbidity sensor, calculates an approximate NTU
   value using a polynomial formula, and turns on an LED based
   on the detected level. Includes a manual calibration wizard
   controlled by typing letters into the Serial Monitor.

  *** WARNING ***
   The NTU formula used here is a widely-used approximation from
   the Arduino community, NOT an official equation published by
   DFRobot (they only publish a reference graph). That's why this
   code calibrates the LED THRESHOLDS using your own real water
   samples instead of blindly trusting the formula.

  Sensor wiring:
   - RED wire   -> 5V
   - BLACK wire -> GND
   - BLUE wire  -> Analog pin A1 (signal)

  REQUIRED LIBRARIES:
   - No external library needed for the sensor (just analogRead).
   - <EEPROM.h> is used to permanently store the calibration. It's
     a NATIVE Arduino library, already bundled with the IDE, no
     download needed.

  ============================================================
                 CALIBRATION GUIDE (Serial Monitor)
  ============================================================
   1. Open the Serial Monitor (Ctrl+Shift+M) at 9600 baud.
   2. Type the letter  C  and press Enter -> starts calibration.
   3. The program will ask you to dip the sensor in CLEAR water
      (pure water). Wait for the reading to stabilize and type
      F  + Enter to fix that point.
   4. Then it will ask for WATER WITH YELLOW DYE (medium turbidity).
      Dip the sensor, wait for it to stabilize, and type
      F  + Enter to fix that point.
   5. Then it will ask for WATER WITH DISSOLVED COFFEE OR DARK INK
      (high turbidity). Dip the sensor, wait for it to stabilize,
      and type  F  + Enter to fix that point.
   6. Finally type  X  + Enter to FINISH the calibration. The
      values are calculated and automatically saved to EEPROM.

   Suggestion for the "high turbidity" sample: dissolve a bit of
   instant coffee in water, or use diluted india ink. Avoid toxic
   or corrosive substances, since the sensor will stay submerged
   for several seconds.
  ============================================================
*/

#include <EEPROM.h> // Native Arduino library used to store data permanently

// ------------------- PIN CONFIGURATION -------------------

const int TURBIDITY_SENSOR_PIN = A1; // Sensor's blue (signal) wire

// >>> SET THE DIGITAL PINS YOU'RE ACTUALLY USING HERE <<<
const int RED_LED_PIN    = 8;   // <-- CHANGE: RED LED digital pin
const int YELLOW_LED_PIN = 9;   // <-- CHANGE: YELLOW LED digital pin
const int GREEN_LED_PIN  = 10;  // <-- CHANGE: GREEN LED digital pin

const int NUMBER_OF_SAMPLES = 800; // Readings to average to reduce noise

// ------------------- NTU FORMULA CONSTANTS -------------------
const float ARDUINO_REFERENCE_VOLTAGE = 5.00; // <-- ADJUST if your real 5V differs
float COEF_A = -1120.4; // <-- ADJUST only if you run your own regression with real NTU
float COEF_B = 5742.3;  // <-- ADJUST only if you run your own regression with real NTU
float COEF_C = -4352.9; // <-- ADJUST only if you run your own regression with real NTU

// ------------------- EEPROM MEMORY ADDRESSES -------------------
// Each float takes 4 bytes, so addresses are spaced 4 apart
const int CLEAR_NTU_ADDRESS      = 0;
const int MEDIUM_NTU_ADDRESS     = 4;
const int TURBID_NTU_ADDRESS     = 8;
const int CALIBRATED_FLAG_ADDRESS = 12; // Flag: 1 = already calibrated before

// ------------------- CALIBRATION VARIABLES (IN NTU) -------------------
// These values are calculated during calibration, or loaded from EEPROM
// if it was already calibrated before. They act as thresholds for the LEDs.
float clearWaterNtu  = 200.0;  // Default value until calibrated
float mediumWaterNtu = 1000.0; // Default value until calibrated
float turbidWaterNtu = 2500.0; // Default value until calibrated

float HIGH_TURBIDITY_THRESHOLD;   // Automatically calculated between medium and turbid
float MEDIUM_TURBIDITY_THRESHOLD; // Automatically calculated between clear and medium

// ------------------- CALIBRATION STATE VARIABLES -------------------
bool calibrating = false;   // true while the wizard is active
int  calibrationStep = 0;   // 0=inactive, 1=clear water, 2=medium water, 3=turbid water


void setup() {
  Serial.begin(9600);

  pinMode(RED_LED_PIN, OUTPUT);
  pinMode(YELLOW_LED_PIN, OUTPUT);
  pinMode(GREEN_LED_PIN, OUTPUT);
  turnOffAllLeds();

  loadCalibrationFromEEPROM();
  recalculateThresholds();

  Serial.println("============================================");
  Serial.println(" DFRobot turbidity sensor - NTU mode");
  Serial.println(" Type 'C' + Enter to start calibration");
  Serial.println("============================================");
}


void loop() {

  // ---------- Check if the user typed something in the Serial Monitor ----------
  if (Serial.available() > 0) {
    char receivedKey = Serial.read();
    processKey(receivedKey);
  }

  // ---------- STEP 1: READ THE SENSOR MULTIPLE TIMES ----------
  long readingSum = 0;
  for (int i = 0; i < NUMBER_OF_SAMPLES; i++) {
    readingSum += analogRead(TURBIDITY_SENSOR_PIN);
    delayMicroseconds(500);
  }
  float averageReading = readingSum / (float)NUMBER_OF_SAMPLES;

  // ---------- STEP 2: CONVERT TO VOLTAGE ----------
  float voltage = averageReading * (ARDUINO_REFERENCE_VOLTAGE / 1023.0);

  // ---------- STEP 3: CONVERT TO NTU ----------
  float ntu = calculateNTU(voltage);

  // ---------- STEP 4: SHOW THE READING ON THE SERIAL MONITOR ----------
  Serial.print("Voltage: ");
  Serial.print(voltage, 3);
  Serial.print(" V  |  Estimated NTU: ");
  Serial.print(ntu, 1);

  if (calibrating) {
    // While calibrating, LEDs are not evaluated, only the reading is shown
    Serial.print("   [CALIBRATING - Step ");
    Serial.print(calibrationStep);
    Serial.println(" of 3]");
  } else {
    // ---------- STEP 5: EVALUATE THE 3 LED CONDITIONS ----------
    Serial.print("  |  Status: ");
    evaluateLeds(ntu);
  }

  delay(500); // Update every half second
}


// ============================================================
//                   CALIBRATION FUNCTIONS
// ============================================================

void processKey(char key) {

  if (key == 'C' || key == 'c') {
    // ----- Start calibration -----
    calibrating = true;
    calibrationStep = 1;
    Serial.println();
    Serial.println("### CALIBRATION STARTED ###");
    Serial.println("Step 1/3: Dip the sensor in CLEAR (pure) water.");
    Serial.println("Once the reading stabilizes, type 'F' + Enter.");
  }
  else if ((key == 'F' || key == 'f') && calibrating) {
    // ----- Fix the current value for the current step -----
    float currentVoltage = readAverageVoltage();
    float currentNtu = calculateNTU(currentVoltage);

    if (calibrationStep == 1) {
      clearWaterNtu = currentNtu;
      Serial.print("Value fixed for CLEAR WATER: ");
      Serial.print(currentNtu, 1);
      Serial.println(" NTU");
      Serial.println("Step 2/3: Dip the sensor in WATER WITH YELLOW DYE.");
      Serial.println("Once the reading stabilizes, type 'F' + Enter.");
      calibrationStep = 2;
    }
    else if (calibrationStep == 2) {
      mediumWaterNtu = currentNtu;
      Serial.print("Value fixed for MEDIUM (yellow) WATER: ");
      Serial.print(currentNtu, 1);
      Serial.println(" NTU");
      Serial.println("Step 3/3: Dip the sensor in WATER WITH COFFEE/DARK INK.");
      Serial.println("Once the reading stabilizes, type 'F' + Enter.");
      calibrationStep = 3;
    }
    else if (calibrationStep == 3) {
      turbidWaterNtu = currentNtu;
      Serial.print("Value fixed for TURBID (dark) WATER: ");
      Serial.print(currentNtu, 1);
      Serial.println(" NTU");
      Serial.println("All 3 points have been fixed.");
      Serial.println("Type 'X' + Enter to FINISH and save the calibration.");
      calibrationStep = 4; // Waiting to finish
    }
    else {
      Serial.println("You already fixed the 3 points. Type 'X' to finish.");
    }
  }
  else if ((key == 'X' || key == 'x') && calibrating) {
    // ----- Finish calibration -----
    if (calibrationStep < 4) {
      Serial.println("You haven't fixed the 3 points with 'F' yet. Can't finish yet.");
    } else {
      recalculateThresholds();
      saveCalibrationToEEPROM();
      calibrating = false;
      calibrationStep = 0;
      Serial.println("### CALIBRATION FINISHED AND SAVED TO MEMORY ###");
      Serial.println("Returning to normal operation...");
      Serial.println();
    }
  }
}

// Reads the sensor multiple times and returns the average voltage (used during calibration)
float readAverageVoltage() {
  long readingSum = 0;
  for (int i = 0; i < NUMBER_OF_SAMPLES; i++) {
    readingSum += analogRead(TURBIDITY_SENSOR_PIN);
    delayMicroseconds(500);
  }
  float averageReading = readingSum / (float)NUMBER_OF_SAMPLES;
  return averageReading * (ARDUINO_REFERENCE_VOLTAGE / 1023.0);
}

// Converts a voltage to NTU using the polynomial formula
float calculateNTU(float voltage) {
  float ntu;
  if (voltage < 2.5) {
    ntu = 3000;
  } else {
    ntu = (COEF_A * voltage * voltage) + (COEF_B * voltage) + COEF_C;
  }
  if (ntu < 0) ntu = 0;
  return ntu;
}

// Calculates the LED thresholds as midpoints between the 3 calibrated samples
void recalculateThresholds() {
  MEDIUM_TURBIDITY_THRESHOLD = (clearWaterNtu + mediumWaterNtu) / 2.0;
  HIGH_TURBIDITY_THRESHOLD   = (mediumWaterNtu + turbidWaterNtu) / 2.0;
}

// Permanently saves the 3 calibrated values to EEPROM
void saveCalibrationToEEPROM() {
  EEPROM.put(CLEAR_NTU_ADDRESS, clearWaterNtu);
  EEPROM.put(MEDIUM_NTU_ADDRESS, mediumWaterNtu);
  EEPROM.put(TURBID_NTU_ADDRESS, turbidWaterNtu);
  EEPROM.put(CALIBRATED_FLAG_ADDRESS, (byte)1); // Marks that a calibration is already saved
}

// Loads a previously saved calibration (if any) when the Arduino powers on
void loadCalibrationFromEEPROM() {
  byte calibratedFlag;
  EEPROM.get(CALIBRATED_FLAG_ADDRESS, calibratedFlag);

  if (calibratedFlag == 1) {
    EEPROM.get(CLEAR_NTU_ADDRESS, clearWaterNtu);
    EEPROM.get(MEDIUM_NTU_ADDRESS, mediumWaterNtu);
    EEPROM.get(TURBID_NTU_ADDRESS, turbidWaterNtu);
    Serial.println("A previously saved calibration was loaded.");
  } else {
    Serial.println("No saved calibration found. Using default values.");
  }
}

// ============================================================
//                     LED FUNCTIONS
// ============================================================

void turnOffAllLeds() {
  digitalWrite(RED_LED_PIN, LOW);
  digitalWrite(YELLOW_LED_PIN, LOW);
  digitalWrite(GREEN_LED_PIN, LOW);
}

// Evaluates the 3 conditions and turns on the corresponding LED
void evaluateLeds(float ntu) {
  if (ntu > HIGH_TURBIDITY_THRESHOLD) {
    digitalWrite(RED_LED_PIN, HIGH);
    digitalWrite(YELLOW_LED_PIN, LOW);
    digitalWrite(GREEN_LED_PIN, LOW);
    Serial.println("HIGH TURBIDITY - RED LED");
  }
  else if (ntu > MEDIUM_TURBIDITY_THRESHOLD) {
    digitalWrite(RED_LED_PIN, LOW);
    digitalWrite(YELLOW_LED_PIN, HIGH);
    digitalWrite(GREEN_LED_PIN, LOW);
    Serial.println("MEDIUM TURBIDITY - YELLOW LED");
  }
  else {
    digitalWrite(RED_LED_PIN, LOW);
    digitalWrite(YELLOW_LED_PIN, LOW);
    digitalWrite(GREEN_LED_PIN, HIGH);
    Serial.println("LOW TURBIDITY - GREEN LED");
  }
}
