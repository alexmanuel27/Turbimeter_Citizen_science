/*
  ============================================================
   DISCRETE 90 DEGREE NEPHELOMETRIC TURBIDITY SENSOR
   ARDUINO NANO + IR LED + 2x BPW34 PHOTODIODE + TL072 + ADS1115
  ============================================================
  Description:
   Replaces the DFRobot SEN0189 module with a custom optical
   bench: an IR LED shines into the sample, a photodiode placed
   at 90 DEGREES from the LED picks up SCATTERED light
   (nephelometry, same principle as ISO 7027 turbidimeters),
   and a second "reference" photodiode near the LED tracks the
   LED's own brightness so drift (temperature, aging) cancels
   out. Both photodiode signals are amplified by a transimpedance
   op-amp (TL072) and read through an external 16-bit ADC
   (ADS1115) instead of the Nano's own 10-bit ADC.

   Full schematic, optical geometry, part numbers and purchase
   links: docs/optical-design.md and docs/diagrams/*.svg

  *** WHY THIS INSTEAD OF THE OLD SEN0189 FORMULA ***
   The old firmware (see firmware/legacy_sen0189/) computed a
   fake "NTU" from a straight-through (0 degree) analog reading
   using a generic community formula with no physical basis, and
   read it through the Nano's noisy 10-bit ADC. This version
   reports an honest, uncalibrated TURBIDITY INDEX instead of a
   fabricated NTU number - it is NOT yet an absolute NTU value.
   Turning it into real NTU requires calibrating against a
   reference instrument or formazin standards (see
   docs/optical-design.md, section "Getting to absolute NTU").

  Wiring summary (see docs/diagrams/circuit-schematic.svg):
   - IR LED (850nm)  -> D7 through a 220ohm resistor -> GND
   - Photodiode A (measurement, at 90 degrees) -> TL072 channel A
     (transimpedance amp) -> ADS1115 AIN0
   - Photodiode B (reference, near the LED)    -> TL072 channel B
     (transimpedance amp) -> ADS1115 AIN1
   - ADS1115: VDD->5V, GND->GND, ADDR->GND (I2C address 0x48),
     SDA->Nano A4, SCL->Nano A5

  REQUIRED LIBRARIES:
   - None beyond what ships with the Arduino IDE. <Wire.h> is
     used for I2C, talking to the ADS1115 directly via its
     register interface (no Adafruit_ADS1X15 dependency), the
     same "no external library" philosophy as the original
     firmware. <EEPROM.h> is used to store the calibration.

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
  ============================================================
*/

#include <Wire.h>
#include <EEPROM.h>

// ------------------- PIN CONFIGURATION -------------------

const int LED_DRIVE_PIN = 7; // Drives the IR LED (through a 220ohm resistor)

// >>> SET THE DIGITAL PINS YOU'RE ACTUALLY USING HERE <<<
const int RED_LED_PIN    = 8;   // <-- CHANGE: status RED LED digital pin
const int YELLOW_LED_PIN = 9;   // <-- CHANGE: status YELLOW LED digital pin
const int GREEN_LED_PIN  = 10;  // <-- CHANGE: status GREEN LED digital pin

const int SAMPLES_PER_READING = 8; // ADS1115 reads to average per state (dark/lit)
const int LED_SETTLE_MS = 5;       // Wait after switching the LED before reading

// ------------------- ADS1115 (RAW I2C, NO LIBRARY) -------------------
const uint8_t ADS1115_ADDRESS = 0x48;      // ADDR pin tied to GND
const uint8_t ADS1115_REG_CONVERSION = 0x00;
const uint8_t ADS1115_REG_CONFIG = 0x01;
const int MEASUREMENT_CHANNEL = 0; // AIN0 <- photodiode A (90 degrees)
const int REFERENCE_CHANNEL   = 1; // AIN1 <- photodiode B (near the LED)
const float ADS1115_LSB_VOLTS = 4.096 / 32768.0; // Gain = +/-4.096V (config below)

// ------------------- EEPROM MEMORY ADDRESSES -------------------
const int CLEAR_INDEX_ADDRESS     = 0;
const int MEDIUM_INDEX_ADDRESS    = 4;
const int TURBID_INDEX_ADDRESS    = 8;
const int CALIBRATED_FLAG_ADDRESS = 12; // Flag: 1 = already calibrated before

// ------------------- CALIBRATION VARIABLES (TURBIDITY INDEX, NOT NTU) -------------------
float clearWaterIndex  = 0.05;  // Default value until calibrated
float mediumWaterIndex = 0.25;  // Default value until calibrated
float turbidWaterIndex = 0.60;  // Default value until calibrated

float HIGH_TURBIDITY_THRESHOLD;
float MEDIUM_TURBIDITY_THRESHOLD;

// ------------------- CALIBRATION STATE VARIABLES -------------------
bool calibrating = false;
int  calibrationStep = 0; // 0=inactive, 1=clear water, 2=medium water, 3=turbid water


void setup() {
  Serial.begin(9600);
  Wire.begin();

  pinMode(LED_DRIVE_PIN, OUTPUT);
  digitalWrite(LED_DRIVE_PIN, LOW);

  pinMode(RED_LED_PIN, OUTPUT);
  pinMode(YELLOW_LED_PIN, OUTPUT);
  pinMode(GREEN_LED_PIN, OUTPUT);
  turnOffAllLeds();

  loadCalibrationFromEEPROM();
  recalculateThresholds();

  Serial.println("============================================");
  Serial.println(" Discrete 90-degree turbidity sensor");
  Serial.println(" Reporting a relative TURBIDITY INDEX (not NTU)");
  Serial.println(" Type 'C' + Enter to start calibration");
  Serial.println("============================================");
}


void loop() {

  if (Serial.available() > 0) {
    char receivedKey = Serial.read();
    processKey(receivedKey);
  }

  float measurementSignal, referenceSignal;
  float index = readTurbidityIndex(measurementSignal, referenceSignal);

  Serial.print("Scatter: ");
  Serial.print(measurementSignal, 4);
  Serial.print(" V | Reference: ");
  Serial.print(referenceSignal, 4);
  Serial.print(" V | Index: ");
  Serial.print(index, 4);

  if (calibrating) {
    Serial.print("   [CALIBRATING - Step ");
    Serial.print(calibrationStep);
    Serial.println(" of 3]");
  } else {
    Serial.print("  |  Status: ");
    evaluateLeds(index);
  }

  delay(300);
}


// ============================================================
//              SENSOR READING (LED ON/OFF DIFFERENCING)
// ============================================================

// Reads both photodiode channels with the LED off, then with the LED on,
// and returns the ambient-corrected ratio (scattered / reference).
// Also returns the two corrected signals (in volts) via the reference args,
// mainly for debugging/logging over Serial.
float readTurbidityIndex(float &measurementSignalOut, float &referenceSignalOut) {
  digitalWrite(LED_DRIVE_PIN, LOW);
  delay(LED_SETTLE_MS);
  float darkMeasurement = readAveragedChannel(MEASUREMENT_CHANNEL);
  float darkReference   = readAveragedChannel(REFERENCE_CHANNEL);

  digitalWrite(LED_DRIVE_PIN, HIGH);
  delay(LED_SETTLE_MS);
  float litMeasurement = readAveragedChannel(MEASUREMENT_CHANNEL);
  float litReference   = readAveragedChannel(REFERENCE_CHANNEL);

  digitalWrite(LED_DRIVE_PIN, LOW); // off between cycles

  float measurementSignal = litMeasurement - darkMeasurement;
  float referenceSignal   = litReference - darkReference;

  measurementSignalOut = measurementSignal;
  referenceSignalOut = referenceSignal;

  // Guard against divide-by-zero / a disconnected or dead reference channel
  if (referenceSignal < 0.001) {
    return 0.0;
  }
  return measurementSignal / referenceSignal;
}

// Reads one ADS1115 channel multiple times and returns the average in volts
float readAveragedChannel(int channel) {
  long sum = 0;
  for (int i = 0; i < SAMPLES_PER_READING; i++) {
    sum += readADS1115Raw(channel);
  }
  float averageRaw = sum / (float)SAMPLES_PER_READING;
  return averageRaw * ADS1115_LSB_VOLTS;
}

// Triggers a single-shot conversion on the given single-ended channel (0-3)
// and returns the raw 16-bit signed result. Talks to the ADS1115 directly
// over I2C, no external library needed.
int16_t readADS1115Raw(int channel) {
  // Config register bits:
  //   OS=1 (start), MUX=100+channel (single-ended vs GND),
  //   PGA=001 (+/-4.096V), MODE=1 (single-shot),
  //   DR=100 (128SPS), COMP_QUE=11 (disable comparator)
  uint16_t config = 0x8000;               // OS = 1 (start single conversion)
  config |= (uint16_t)(0x04 + channel) << 12; // MUX
  config |= 0x1 << 9;                     // PGA = +/-4.096V
  config |= 0x1 << 8;                     // MODE = single-shot
  config |= 0x4 << 5;                     // DR = 128SPS
  config |= 0x3;                          // COMP_QUE = disabled

  writeADS1115Register(ADS1115_REG_CONFIG, config);
  delay(9); // ~1 conversion period at 128SPS, plus margin

  return (int16_t)readADS1115Register(ADS1115_REG_CONVERSION);
}

void writeADS1115Register(uint8_t reg, uint16_t value) {
  Wire.beginTransmission(ADS1115_ADDRESS);
  Wire.write(reg);
  Wire.write((uint8_t)(value >> 8));   // MSB
  Wire.write((uint8_t)(value & 0xFF)); // LSB
  Wire.endTransmission();
}

uint16_t readADS1115Register(uint8_t reg) {
  Wire.beginTransmission(ADS1115_ADDRESS);
  Wire.write(reg);
  Wire.endTransmission();

  Wire.requestFrom((int)ADS1115_ADDRESS, 2);
  uint16_t value = 0;
  if (Wire.available() >= 2) {
    value = (uint16_t)Wire.read() << 8; // MSB
    value |= Wire.read();               // LSB
  }
  return value;
}

// ============================================================
//                   CALIBRATION FUNCTIONS
// ============================================================

void processKey(char key) {

  if (key == 'C' || key == 'c') {
    calibrating = true;
    calibrationStep = 1;
    Serial.println();
    Serial.println("### CALIBRATION STARTED ###");
    Serial.println("Step 1/3: Dip the sensor in CLEAR (pure) water.");
    Serial.println("Once the reading stabilizes, type 'F' + Enter.");
  }
  else if ((key == 'F' || key == 'f') && calibrating) {
    float measurementSignal, referenceSignal;
    float currentIndex = readTurbidityIndex(measurementSignal, referenceSignal);

    if (calibrationStep == 1) {
      clearWaterIndex = currentIndex;
      Serial.print("Value fixed for CLEAR WATER: ");
      Serial.println(currentIndex, 4);
      Serial.println("Step 2/3: Dip the sensor in WATER WITH YELLOW DYE.");
      Serial.println("Once the reading stabilizes, type 'F' + Enter.");
      calibrationStep = 2;
    }
    else if (calibrationStep == 2) {
      mediumWaterIndex = currentIndex;
      Serial.print("Value fixed for MEDIUM (yellow) WATER: ");
      Serial.println(currentIndex, 4);
      Serial.println("Step 3/3: Dip the sensor in WATER WITH COFFEE/DARK INK.");
      Serial.println("Once the reading stabilizes, type 'F' + Enter.");
      calibrationStep = 3;
    }
    else if (calibrationStep == 3) {
      turbidWaterIndex = currentIndex;
      Serial.print("Value fixed for TURBID (dark) WATER: ");
      Serial.println(currentIndex, 4);
      Serial.println("All 3 points have been fixed.");
      Serial.println("Type 'X' + Enter to FINISH and save the calibration.");
      calibrationStep = 4;
    }
    else {
      Serial.println("You already fixed the 3 points. Type 'X' to finish.");
    }
  }
  else if ((key == 'X' || key == 'x') && calibrating) {
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

// Calculates the LED thresholds as midpoints between the 3 calibrated samples
void recalculateThresholds() {
  MEDIUM_TURBIDITY_THRESHOLD = (clearWaterIndex + mediumWaterIndex) / 2.0;
  HIGH_TURBIDITY_THRESHOLD   = (mediumWaterIndex + turbidWaterIndex) / 2.0;
}

void saveCalibrationToEEPROM() {
  EEPROM.put(CLEAR_INDEX_ADDRESS, clearWaterIndex);
  EEPROM.put(MEDIUM_INDEX_ADDRESS, mediumWaterIndex);
  EEPROM.put(TURBID_INDEX_ADDRESS, turbidWaterIndex);
  EEPROM.put(CALIBRATED_FLAG_ADDRESS, (byte)1);
}

void loadCalibrationFromEEPROM() {
  byte calibratedFlag;
  EEPROM.get(CALIBRATED_FLAG_ADDRESS, calibratedFlag);

  if (calibratedFlag == 1) {
    EEPROM.get(CLEAR_INDEX_ADDRESS, clearWaterIndex);
    EEPROM.get(MEDIUM_INDEX_ADDRESS, mediumWaterIndex);
    EEPROM.get(TURBID_INDEX_ADDRESS, turbidWaterIndex);
    Serial.println("A previously saved calibration was loaded.");
  } else {
    Serial.println("No saved calibration found. Using default values.");
  }
}

// ============================================================
//                     STATUS LED FUNCTIONS
// ============================================================

void turnOffAllLeds() {
  digitalWrite(RED_LED_PIN, LOW);
  digitalWrite(YELLOW_LED_PIN, LOW);
  digitalWrite(GREEN_LED_PIN, LOW);
}

void evaluateLeds(float index) {
  if (index > HIGH_TURBIDITY_THRESHOLD) {
    digitalWrite(RED_LED_PIN, HIGH);
    digitalWrite(YELLOW_LED_PIN, LOW);
    digitalWrite(GREEN_LED_PIN, LOW);
    Serial.println("HIGH TURBIDITY - RED LED");
  }
  else if (index > MEDIUM_TURBIDITY_THRESHOLD) {
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
