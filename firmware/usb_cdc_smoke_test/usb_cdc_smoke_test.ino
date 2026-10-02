/* USB-C communication test for the XIAO SAMD21. No sensor is measured. */

void setup() {
  Serial.begin(115200);
}

void loop() {
  if (!Serial || !Serial.available()) return;
  String command = Serial.readStringUntil('\n');
  command.trim();
  if (command == "MEASURE") {
    static unsigned int count = 0;
    Serial.print("TURB1,DEMO,");
    Serial.println(0.20f + 0.01f * (count++ % 5), 4);
  }
}
