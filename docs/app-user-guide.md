# Using and calibrating the WaterLab Turbidimeter app

Design: **Ing. Alex M. Rivera**. Guide for app version **0.5**, 8 November 2026.

The Android phone powers the instrument and displays its measurements. The
app saves readings on the phone and can share them as a CSV file. It does not
upload them to CubanWaterLab yet.

## Before you start

- A fully assembled instrument with the XIAO SAMD21 measurement firmware.
- An Android phone that supports USB host/OTG, with a compatible data cable.
- The version 0.5 APK supplied by the project team. Install it on the phone;
  Android may ask you to allow installation from the app used to open the APK.
- A clean cuvette containing your sample and the light-tight enclosure.
- For calibration: a reference turbidimeter and **at least three different
  samples** measured with it, covering the range you intend to use.

If the app is already installed, open **WaterLab Turbidimeter**. It can be
used without an internet connection. Phone compatibility and a complete
calibration with the assembled instrument are still to be verified.

## Take a reading

1. Mix the sample as appropriate, avoiding bubbles. Fill the cuvette to the
   level required by its holder; wipe the outside and optical faces clean.
2. Place the cuvette in the holder in the same orientation used for the
   instrument's calibration. Close the enclosure to exclude external light.
3. Connect the turbidimeter to the phone, open the app and tap **Connect
   sensor**. Approve Android's USB permission prompt when it appears.
4. Keep the instrument still and tap **Measure now**. The result appears on
   **Home** and a successful reading is saved in **History** on the phone.
5. Open the side menu and tap **History** to review readings. The displayed
   time is UTC. The screen shows the 50 most recent readings; **Share readings
   (CSV)** shares the complete saved record through an app you choose.

Before calibration, the value is an **optical ratio**, without a turbidity
unit. It cannot be interpreted as NTU or FNU. After a calibration is active
for that sensor, the app converts the ratio to the selected reference unit.
The app also keeps the original ratio and calibration identifier in history.

## Calibrate against a reference instrument

Calibration pairs each result from the reference instrument with the optical
response of **the same sample** measured in our instrument. Use a reference
instrument with known calibration status. Its reading is a comparison value;
an arbitrary sample is not automatically a certified standard.

### Prepare samples

1. Prepare at least **three different samples** covering the expected range;
   the app accepts up to 12 points. Include a low-turbidity sample and
   intermediate and higher values relevant to your work. Keep extra samples
   aside for checking the finished calibration.
2. Follow the reference instrument's instructions for its cuvette and
   measurement. Measure each sample with both instruments using the same
   sample, as close together in time as practical. Keep it representative
   and mixed, avoid bubbles, and clean the cuvette's optical faces each time.
3. Record the reference instrument model, serial number and calibration date
   if available. Record sample identification and reference readings with units.
   Do not assume clean water is exactly 0 NTU or 0 FNU: use its measured value.

For example, if the reference instrument reports **12.3 NTU**, enter 12.3 and
select NTU for that sample. This number is only an example, not a required
calibration value.

### Enter the points

1. Connect the sensor on **Home**, open the side menu and select **Calibration**.
2. Under **NEW REFERENCE COMPARISON**, enter the model and identifier in
   **Reference instrument and ID**. Under **Reference unit**, select the unit
   shown by the reference instrument: **FNU** or **NTU**. The app does not
   convert between them.
3. For the first sample, note its measured reference value. Place the same
   sample in our cuvette, wipe its faces, insert it and close the enclosure.
4. Tap **Measure sample (5 readings)**. Keep the instrument still until the
   capture finishes; the app displays the mean optical ratio and its standard
   deviation. If capture fails, resolve the connection or sensor error and
   repeat it; a failed capture is not a calibration point.
5. Enter the reference result in **Reference reading**, then tap **Add
   calibration point**. Check the displayed reference value and ratio.
6. Repeat steps 3–5 for each remaining sample, from low to high where
   practical. Use a new measured reference value for each point. The app
   requires higher reference values to correspond to higher ratios and rejects
   overlapping measurement ranges or a nonmonotonic curve. If a point is
   rejected, check the sample and repeat the measurements; do not invent a
   value to make the curve pass.
7. After at least three valid points, tap **Save and use calibration**. The
   calibration is linked to the connected sensor's USB identity. If the
   sensor has no readable USB serial number, the app can show raw ratios but
   cannot safely link a calibration.
8. Check one or more **independent samples not used to fit the curve** against
   the reference instrument. Record the differences, repeatability and tested
   range before using the results quantitatively.

The app draws straight segments between points. Outside the measured range it
extends the nearest end segment and labels the result **EXTRAPOLATED**. Such
results have not been validated merely by saving the curve; a negative
extrapolated result is displayed but is not physical turbidity.

## Keep and transfer the calibration

In **Calibration**, use **Export this calibration (JSON)** to save the active
curve with Android's file picker. To use it on another phone, connect the
**same sensor**, choose **Import calibration (JSON)**, select that file and
confirm activation. A replacement XIAO has a different identity and requires
its own calibration. Changes to the electronics, optics or cuvette require a
new calibration and validation.

**Use uncalibrated ratios** deactivates the active curve for future readings;
existing history and calibration records are kept. In **History**, **Export
calibration records (JSON)** exports an archive of all curves for traceability;
that archive is not the individual curve file used for import.

## If something goes wrong

- **Phone does not connect:** verify USB host/OTG support, the data cable,
  Android's permission prompt and that the device is powered. Reconnect and
  tap **Connect sensor** again.
- **ADC_MISSING or connection stops:** inspect ADS 3.3 V, common ground and
  I²C connections with power disconnected as described in the
  [assembly guide](assembly.md).
- **SATURATED, REFERENCE_LOW or another sensor error:** no valid measurement
  was produced. Check the cuvette, light shielding, supply, VREF and optical
  alignment using the [firmware diagnostic instructions](../firmware/xiao_samd21_turbimeter/README.md).
- **Capture cancelled or timed out:** reconnect the sensor and repeat the
  unfinished five-reading capture. Save the finished calibration before
  closing the app.

The app's **Privacy** page can delete local readings and calibration data.
Copies previously shared as CSV or JSON are managed separately by whoever
received or stored them.
