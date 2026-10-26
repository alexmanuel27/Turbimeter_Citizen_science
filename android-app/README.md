# Android app

Design: **Ing. Alex M. Rivera**. Current release: **0.5 / code 5**.

The Android phone is the turbidimeter's screen and controller. This version
connects to a USB-CDC XIAO SAMD21 and requests real readings. The side menu has
Home, Calibration, History, and Privacy. Readings stay on the phone and can be
shared as CSV. There is no simulated-reading mode, network upload, contributor
form, or location permission.

## Try it now

1. Flash [`xiao_samd21_turbimeter.ino`](../firmware/xiao_samd21_turbimeter/xiao_samd21_turbimeter.ino)
   to the XIAO SAMD21 using the [Seeed Arduino setup](https://wiki.seeedstudio.com/Seeeduino-XIAO/).
2. Open this `android-app` folder in Android Studio, install Android SDK 36 if
   prompted, and run the app on an Android phone with USB host support.
3. Connect the assembled sensor with a **data-capable USB-C cable**. Tap
   **Connect sensor**, grant USB permission, then tap **Measure now**. Without
   an active calibration, the app shows and saves an **optical ratio**.
   Follow the [wiring and test steps](../firmware/xiao_samd21_turbimeter/README.md).

The phone connects through the XIAO USB socket using a suitable data cable
and USB host/OTG termination. Earlier base J2 / interboard J4 / upper J5 cable
links describe a retired carrier layout. Use the actual cable assembly and
the saved boards as the reference. USB-C host/device detection is not
established by counting four conductors. Follow the
[hardware cable and assembly notes](../hardware/README.md).

## Calibrate against a reference instrument

1. Connect the sensor, then open **Calibration** in the side menu.
2. Identify the reference turbidimeter (model/serial/calibration details) and
   select the unit it reports: **FNU** or **NTU**. No unit conversion is applied.
3. Measure a sample with the reference instrument. Put the same sample in our
   sensor, close the enclosure and tap **Measure sample (5 readings)**.
4. Enter the reference value, then tap **Add calibration point**. The app keeps
   all five raw ratios, their mean, sample standard deviation, and capture time.
5. Repeat for **3 to 12 different reference values**, including a low-turbidity
   sample and covering the intended working range. Keep the sample mixed and
   free of bubbles. Higher reference values must produce higher sensor ratios;
   overlapping five-reading ranges or a nonmonotonic response prevent saving.
6. Tap **Save and use calibration**. Check additional samples that were not used
   to build the curve before relying on quantitative results.

Conversion uses **straight lines between the paired points**. Outside the
measured range, the first or last segment is extended: the app still shows the
estimated value but marks it **EXTRAPOLATED** on Home, in History, and in CSV.
Extrapolation is an estimate whose accuracy has not been established outside
that range. A negative extrapolation is shown explicitly; it is not physical
turbidity and is not silently clamped to zero. Hardware errors such as saturation
still stop a measurement; a curve cannot recover a rejected optical reading.

Each curve belongs to the connected sensor's USB vendor/product/serial identity,
so it can be transferred between compatible phones without being applied to a
different sensor. If a serial number is unavailable, raw readings work but
calibration is disabled. New curves have new IDs and never recalculate history.
Completed draft points survive screen recreation; reconnect to continue and
repeat any unfinished capture. Save the curve before closing the app.
Changes to the optics, electronics or cuvette require a new calibration. A
reference comparison does not establish ISO/EPA compliance or universal accuracy
across different particle types.

**Export this calibration (JSON)** saves an individual curve through Android's
file picker. On another phone, connect the same sensor and use **Import
calibration (JSON)**, then confirm activation. Imports validate the format,
sensor identity, points and immutable ID. **Use uncalibrated ratios** deactivates
the curve while retaining its record. History's **Export calibration records
(JSON)** saves an archive of all curves for traceability; this multi-curve archive
is not an individual calibration import file.

## History and privacy

The CSV columns are:

```text
timestamp_utc,value,unit,quality,calibration_id,optical_ratio,sensor_id
```

Raw ratios are retained alongside converted results. Quality is `UNCALIBRATED`,
`OK`, or `EXTRAPOLATED`; a nonfinite conversion saves the raw ratio with
`CONVERSION_ERROR`. Existing five-column history is preserved and upgraded with
empty extra columns. Calibration captures do not become ordinary history rows.
Only requested replies are accepted; capture cancellation or timeout disconnects
the sensor to discard late replies. Reconnect to continue.

Saved readings and calibration records remain private on the phone. Privacy
explains storage and **Delete all local data** removes both, including any legacy
contributor details. Exported copies must be managed separately by the user.

## Serial contract, version 1

The app sends `MEASURE\n`; the device returns one ASCII line. USB packet
fragments are assembled before parsing. Current firmware returns raw ratios;
calibration conversion happens on the phone.

| Response | Meaning |
| --- | --- |
| `TURB1,RATIO,0.2500` | Real optical ratio, converted if this sensor has an active curve |
| `TURB1,RESULT,12.30,FNU,CAL-001,OK` | Supported firmware-calibrated result; not converted a second time |
| `TURB1,ERROR,ADC_MISSING` | Example hardware error shown by the app |

Legacy `TURB1,DEMO,...` responses are rejected as unsupported firmware. The app
also rejects malformed/nonfinite wire values. Sensor firmware must only emit
`RESULT` when its own calibration has been established.

## Build and checks

With a JDK installed, run from `android-app`:

```sh
./gradlew :app:assembleDebug :app:assembleDebugAndroidTest
mkdir -p /tmp/turbimeter-calibration-check
javac -d /tmp/turbimeter-calibration-check app/src/main/java/org/citizenscience/turbimeter/{CalibrationCurve,ReadingLog,MeasurementFrame,LineFramer}.java test/{CalibrationCheck,MeasurementFrameCheck}.java
java -ea -cp /tmp/turbimeter-calibration-check org.citizenscience.turbimeter.CalibrationCheck
java -ea -cp /tmp/turbimeter-calibration-check org.citizenscience.turbimeter.MeasurementFrameCheck
```

The native Android integration check exercises paired capture, persistence,
sensor isolation, JSON, CSV migration, extrapolation, timeout, cancellation and
synthetic rejection. **Run only in a disposable emulator**: it clears that test
app's local history and calibration storage. Install both debug APKs, then run:

```sh
adb -s EMULATOR_SERIAL shell am instrument -w com.Turbidimeter.main.test/org.citizenscience.turbimeter.CalibrationIntegrationCheck
```

These software checks do not validate physical turbidity accuracy. See
[`play-store/release-checklist.md`](play-store/release-checklist.md) for the
remaining phone, reference-instrument and publication checks.
