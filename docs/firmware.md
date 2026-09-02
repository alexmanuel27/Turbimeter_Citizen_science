# Firmware explanation

Analysis of [`firmware/firmware.ino`](../firmware/firmware.ino): DFRobot
SEN0189 turbidity sensor + Arduino Nano.

## Hardware wiring

| Sensor wire | Destination |
|-------------------|---------|
| Red (VCC) | 5V |
| Black (GND) | GND |
| Blue (analog signal) | Pin A1 |

<img src="https://raw.githubusercontent.com/DFRobot/DFRobotMediaWikiImage/master/Image/SEN0189_Probe_Connection.jpg" width="500">

*Sensor wiring diagram. Source: DFRobot Wiki (official).*

There are also 3 indicator LEDs (green/yellow/red) on configurable digital
pins (10, 9, and 8 by default) to visually show the turbidity level without
needing to watch the Serial Monitor.

## General logic (`loop`)

1. **Reading the sensor:** instead of taking a single analog reading (which
   would be noisy), it averages **800** consecutive `analogRead(A1)` samples
   with a small pause between each one (`delayMicroseconds(500)`). This
   stabilizes the value before it's used.
2. **Converting to voltage:** the average (0–1023, 10-bit ADC resolution) is
   scaled to a real voltage using the Arduino's 5V reference:
   `voltage = average * (5.0 / 1023.0)`.
3. **Converting to NTU:** a second-degree polynomial formula
   (`ntu = A*v² + B*v + C`) is applied, with coefficients taken from the
   Arduino community (not an official DFRobot value — the manufacturer only
   publishes a reference graph, not an equation). Below 2.5V, maximum
   turbidity (3000 NTU) is assumed, since the sensor's curve is no longer
   reliable in that range.
4. **Output:** the voltage and NTU value are printed to the Serial Monitor,
   and if not currently calibrating, the corresponding "level" is evaluated
   to turn on the right LED.

## Why there's a calibration wizard

Since the NTU formula is just a generic approximation, the code doesn't
trust it to decide when to turn on each LED. Instead, it lets you calibrate
the **thresholds** using real water samples from the actual sensor:

1. Type `C` in the Serial Monitor to start.
2. You're asked to dip the sensor in **clear water**, fixed with `F`.
3. You're asked for **medium turbidity water** (suggests yellow dye), fixed
   with `F`.
4. You're asked for **high turbidity water** (suggests coffee or dark ink),
   fixed with `F`.
5. You finish with `X`, which:
   - Calculates the thresholds as the midpoint between each pair of samples
     (`recalculateThresholds()`).
   - Saves the 3 reference NTU values to the Arduino's **EEPROM**
     (`saveCalibrationToEEPROM()`), so the calibration isn't lost when the
     device is powered off.

On power-up, `loadCalibrationFromEEPROM()` checks whether a calibration was
already saved (using a "flag" at EEPROM address 12) and, if so, loads it
instead of using the default values.

## LED logic (`evaluateLeds`)

With the 3 calibrated points (clear / medium / turbid), two intermediate
thresholds are calculated:

- `MEDIUM_TURBIDITY_THRESHOLD` = midpoint between "clear" and "medium"
- `HIGH_TURBIDITY_THRESHOLD` = midpoint between "medium" and "turbid"

Then:

- NTU > `HIGH_TURBIDITY_THRESHOLD` → **red LED** (high turbidity)
- NTU > `MEDIUM_TURBIDITY_THRESHOLD` → **yellow LED** (medium turbidity)
- otherwise → **green LED** (low turbidity)

## Notes for anyone modifying the code

- The LED pins (`RED_LED_PIN`/`YELLOW_LED_PIN`/`GREEN_LED_PIN`) are marked
  in the code as `<-- CHANGE` because they depend on how each board is
  wired.
- `NUMBER_OF_SAMPLES = 800` is a trade-off between reading stability and
  refresh speed (more samples = more precise but slower refresh, currently
  about every 0.5s plus sampling time).
- The `COEF_A/B/C` coefficients should only be touched if you run your own
  regression with real NTU values (e.g. with a reference turbidimeter) —
  something still pending for this project. Today, reliability comes from
  the manually calibrated thresholds, not from the formula.
