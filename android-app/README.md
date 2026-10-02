# Android app prototype

The Android phone is the turbidimeter's screen and controller. This first
version connects to a USB-CDC XIAO SAMD21, requests a reading, identifies demo
or uncalibrated data, and can save/share real readings locally with their
quality status. It never uploads automatically. The citizen-science API is not defined yet, so
the upload option will be added when its URL, authentication, and data format
are available.

## Try it now

1. Flash [`usb_cdc_smoke_test.ino`](../firmware/usb_cdc_smoke_test/usb_cdc_smoke_test.ino)
   to the XIAO SAMD21 using the [Seeed Arduino setup](https://wiki.seeedstudio.com/Seeeduino-XIAO/).
2. Open this `android-app` folder in Android Studio, install Android SDK 35 if
   prompted, and run the app on an Android phone with USB host support.
3. Connect the XIAO with a **data-capable USB-C cable**. Tap **Connect sensor**,
   grant USB permission, then tap **Measure**. The board sends a changing demo
   ratio; the app labels it as test data and does not allow saving it.

For the final sealed instrument, the cable cut and passed through the gland
still needs a sound electrical termination at the board's USB-C connection.
Test the complete cable and the intended Android phone before sealing.

## Serial contract, version 1

The app sends one ASCII line: `MEASURE\n`. The device sends one ASCII line in
response. USB packets may split a line; the app assembles lines before parsing.

| Response | Meaning |
| --- | --- |
| `TURB1,DEMO,0.2500` | Synthetic value for testing USB and the screen |
| `TURB1,RATIO,0.2500` | Real optical ratio, still uncalibrated |
| `TURB1,RESULT,12.30,FNU,CAL-001,OK` | Future calibrated result with unit and calibration ID |

The app rejects incomplete/nonfinite values and will enable **Save** for
`RATIO` and `RESULT`, recording their quality status in the CSV. The measurement firmware must not emit `RESULT` until its
calibration and valid measurement range have been verified. For this 850 nm
near-infrared design, decide the reportable unit and range during validation.

Saved readings remain private on the phone and can be shared manually as CSV.
No network permission or endpoint is configured yet.

## Check the parser without Android

With a JDK installed, run from `android-app`:

```sh
mkdir -p /tmp/turbimeter-parser
javac -d /tmp/turbimeter-parser app/src/main/java/org/citizenscience/turbimeter/{MeasurementFrame,LineFramer}.java test/MeasurementFrameCheck.java
java -ea -cp /tmp/turbimeter-parser org.citizenscience.turbimeter.MeasurementFrameCheck
```
