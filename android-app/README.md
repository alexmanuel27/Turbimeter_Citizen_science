# Android app

Design: **Ing. Alex M. Rivera**. Current release: **0.4 / code 4**.

The Android phone is the turbidimeter's screen and controller. This version
connects to a USB-CDC XIAO SAMD21 and requests a reading. The side menu has
Home, History, and Privacy. Each real reading is saved locally with its UTC
timestamp and calibration status; History can be shared as CSV. Demo frames
from the old smoke-test firmware are labelled as synthetic and never saved.
Privacy explains local data handling and lets the user delete saved data.
The app does not upload readings or request location permission.

## Try it now

1. Flash [`xiao_samd21_turbimeter.ino`](../firmware/xiao_samd21_turbimeter/xiao_samd21_turbimeter.ino)
   to the XIAO SAMD21 using the [Seeed Arduino setup](https://wiki.seeedstudio.com/Seeeduino-XIAO/).
2. Open this `android-app` folder in Android Studio, install Android SDK 36 if
   prompted, and run the app on an Android phone with USB host support.
3. Connect the XIAO with a **data-capable USB-C cable**. Tap **Connect sensor**,
   grant USB permission, then tap **Measure now**. The board reads the ADS1115
   and reports an uncalibrated optical ratio or a specific sensor error. Follow
   its [wiring and test steps](../firmware/xiao_samd21_turbimeter/README.md)
   before connecting the analog circuit.

For the sealed instrument, the four-wire cable runs through base J2,
interboard J4 and upper J5 to a compact USB-C pigtail in the XIAO socket.
USB-C host/device detection must be handled by suitable plugs or an OTG
adapter; it is not established by counting the four wires. Follow the
[hardware cable and assembly notes](../hardware/README.md), and test the
complete sensor with compatible Android phones before sealing.

## Serial contract, version 1

The app sends one ASCII line: `MEASURE\n`. The device sends one ASCII line in
response. USB packets may split a line; the app assembles lines before parsing.

| Response | Meaning |
| --- | --- |
| `TURB1,DEMO,0.2500` | Synthetic value for testing USB and the screen |
| `TURB1,RATIO,0.2500` | Real optical ratio, still uncalibrated |
| `TURB1,RESULT,12.30,FNU,CAL-001,OK` | Future calibrated result with unit and calibration ID |
| `TURB1,ERROR,ADC_MISSING` | Example hardware error shown by the app |

The app rejects incomplete/nonfinite values and saves real `RATIO` and `RESULT`
frames automatically, recording their quality status in the CSV. The measurement firmware must not emit `RESULT` until its
calibration and valid measurement range have been verified. For this 850 nm
near-infrared design, decide the reportable unit and range during validation.

Saved readings remain private on the phone and can be shared manually as CSV.
The release app has no network permission or endpoint configured yet.

## Google Play preparation

See [`play-store/release-checklist.md`](play-store/release-checklist.md) for the
store assets, release bundle, privacy policy, and closed-testing steps.

## Check the parser without Android

With a JDK installed, run from `android-app`:

```sh
mkdir -p /tmp/turbimeter-parser
javac -d /tmp/turbimeter-parser app/src/main/java/org/citizenscience/turbimeter/{MeasurementFrame,LineFramer}.java test/MeasurementFrameCheck.java
java -ea -cp /tmp/turbimeter-parser org.citizenscience.turbimeter.MeasurementFrameCheck
```
