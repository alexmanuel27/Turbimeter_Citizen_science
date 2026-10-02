#include <Wire.h>

// XIAO SAMD21: D4=SDA, D5=SCL, D7=LED control. All logic is 3.3 V.
constexpr uint8_t LED_PIN = 7;
constexpr uint8_t ADS_ADDRESS = 0x48; // ADS1115 ADDR tied to GND
constexpr uint8_t SAMPLES = 4;
constexpr uint8_t LED_SETTLE_MS = 5;
constexpr int8_t SCATTER_POLARITY = 1; // Use -1 if this amplifier output falls with light
constexpr int8_t REFERENCE_POLARITY = 1;
constexpr int16_t MIN_REFERENCE_COUNTS = 8; // Tune after measuring dark noise
constexpr int16_t SATURATION_COUNTS = 26000; // ~3.25 V at +/-4.096 V PGA

struct OpticalReading {
  int32_t darkScatter;
  int32_t litScatter;
  int32_t darkReference;
  int32_t litReference;
  float ratio;
};

char command[24];
uint8_t commandLength = 0;
bool commandOverflow = false;
const char *adcError = "ADC_IO";

bool readRegister(uint8_t reg, uint16_t &value) {
  Wire.beginTransmission(ADS_ADDRESS);
  Wire.write(reg);
  if (Wire.endTransmission(false) != 0) {
    adcError = "ADC_READ";
    return false;
  }
  if (Wire.requestFrom(static_cast<int>(ADS_ADDRESS), 2) != 2) {
    adcError = "ADC_READ";
    return false;
  }
  value = static_cast<uint16_t>(Wire.read()) << 8;
  value |= Wire.read();
  return true;
}

bool readChannel(uint8_t channel, int16_t &value) {
  // Single-ended AIN0/AIN1, +/-4.096 V, single shot at 128 samples/s.
  const uint16_t config = 0x8000 | (static_cast<uint16_t>(4 + channel) << 12)
      | 0x0200 | 0x0100 | 0x0080 | 0x0003;
  Wire.beginTransmission(ADS_ADDRESS);
  Wire.write(0x01);
  Wire.write(config >> 8);
  Wire.write(config & 0xff);
  const uint8_t transmission = Wire.endTransmission();
  if (transmission != 0) {
    adcError = transmission == 2 ? "ADC_MISSING" : "ADC_CONFIG";
    return false;
  }

  uint16_t state = 0;
  bool ready = false;
  for (uint8_t elapsed = 0; elapsed < 20; ++elapsed) {
    delay(1);
    if (!readRegister(0x01, state)) return false;
    if (state & 0x8000) {
      ready = true;
      break;
    }
  }
  if (!ready) {
    adcError = "ADC_TIMEOUT";
    return false;
  }
  uint16_t raw = 0;
  if (!readRegister(0x00, raw)) return false;
  value = static_cast<int16_t>(raw);
  return true;
}

bool readPair(int16_t &scatter, int16_t &reference) {
  return readChannel(0, scatter) && readChannel(1, reference);
}

bool measure(OpticalReading &reading, const char *&error) {
  uint16_t adcConfig = 0;
  if (!readRegister(0x01, adcConfig)) {
    error = "ADC_MISSING";
    return false;
  }

  int32_t darkScatter = 0, litScatter = 0, darkReference = 0, litReference = 0;
  error = nullptr;
  digitalWrite(LED_PIN, LOW);
  delay(LED_SETTLE_MS);
  for (uint8_t i = 0; i < SAMPLES; ++i) {
    int16_t scatter = 0, reference = 0;
    if (!readPair(scatter, reference)) {
      error = adcError;
      break;
    }
    if (scatter >= SATURATION_COUNTS || reference >= SATURATION_COUNTS) {
      error = "SATURATED";
      break;
    }
    darkScatter += scatter;
    darkReference += reference;

    digitalWrite(LED_PIN, HIGH);
    delay(LED_SETTLE_MS);
    if (!readPair(scatter, reference)) {
      error = adcError;
      break;
    }
    if (scatter >= SATURATION_COUNTS || reference >= SATURATION_COUNTS) {
      error = "SATURATED";
      break;
    }
    litScatter += scatter;
    litReference += reference;
    digitalWrite(LED_PIN, LOW);
    delay(LED_SETTLE_MS);
  }
  digitalWrite(LED_PIN, LOW);
  if (error) return false;

  reading.darkScatter = darkScatter / SAMPLES;
  reading.litScatter = litScatter / SAMPLES;
  reading.darkReference = darkReference / SAMPLES;
  reading.litReference = litReference / SAMPLES;
  const int32_t scatterSignal = SCATTER_POLARITY * (reading.litScatter - reading.darkScatter);
  const int32_t referenceSignal = REFERENCE_POLARITY * (reading.litReference - reading.darkReference);
  if (referenceSignal < MIN_REFERENCE_COUNTS) {
    error = "REFERENCE_LOW";
    return false;
  }
  if (scatterSignal < 0) {
    error = "SCATTER_NEGATIVE";
    return false;
  }
  reading.ratio = static_cast<float>(scatterSignal) / referenceSignal;
  if (reading.ratio > 10000.0f) {
    error = "RATIO_RANGE";
    return false;
  }
  return true;
}

void respond(const char *input) {
  if (strcmp(input, "PING") == 0) {
    Serial.println("TURB1,PONG");
    return;
  }
  if (strcmp(input, "MEASURE") != 0 && strcmp(input, "DIAG") != 0) {
    Serial.println("TURB1,ERROR,BAD_COMMAND");
    return;
  }

  OpticalReading reading;
  const char *error = nullptr;
  if (!measure(reading, error)) {
    Serial.print("TURB1,ERROR,");
    Serial.println(error);
    return;
  }
  if (strcmp(input, "MEASURE") == 0) {
    Serial.print("TURB1,RATIO,");
    Serial.println(reading.ratio, 4);
  } else {
    Serial.print("TURB1,DIAG,");
    Serial.print(reading.darkScatter);
    Serial.print(',');
    Serial.print(reading.litScatter);
    Serial.print(',');
    Serial.print(reading.darkReference);
    Serial.print(',');
    Serial.print(reading.litReference);
    Serial.print(',');
    Serial.println(reading.ratio, 4);
  }
}

void setup() {
  pinMode(LED_PIN, OUTPUT);
  digitalWrite(LED_PIN, LOW);
  Wire.begin();
  Serial.begin(115200); // USB CDC; no wait for the host
}

void loop() {
  while (Serial.available()) {
    const char ch = Serial.read();
    if (ch == '\r') continue;
    if (ch == '\n') {
      if (!commandOverflow && commandLength > 0) {
        command[commandLength] = '\0';
        respond(command);
      } else if (commandOverflow) {
        Serial.println("TURB1,ERROR,BAD_COMMAND");
      }
      commandLength = 0;
      commandOverflow = false;
    } else if (ch < 32 || ch > 126 || commandLength >= sizeof(command) - 1) {
      commandOverflow = true;
    } else if (!commandOverflow) {
      command[commandLength++] = ch;
    }
  }
}
