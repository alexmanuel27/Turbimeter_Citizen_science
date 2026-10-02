# Android app

The Android phone is the turbidimeter's screen and controller. This first
version connects to a USB-CDC XIAO SAMD21 and requests a reading. The side
menu has Home, History, Signature, and Privacy. Each real reading is saved automatically
in local History with its UTC timestamp and calibration status; History can be
shared as CSV. Demo readings are never saved. Signature stores
a self-declared contributor name and optional institution on the phone.
Privacy explains local data handling and lets the user delete saved data.

The **Upload reading** button appears on Home after a real reading. Uncalibrated
ratios cannot be uploaded. The citizen-science API is not defined yet, so the
app does not send data or request location permission. When the API is added,
each upload must use a fresh upload timestamp and phone location, with explicit
location permission, plus the saved contributor identity. Upload stays an
optional user action.

## Try it now

1. Flash [`usb_cdc_smoke_test.ino`](../firmware/usb_cdc_smoke_test/usb_cdc_smoke_test.ino)
   to the XIAO SAMD21 using the [Seeed Arduino setup](https://wiki.seeedstudio.com/Seeeduino-XIAO/).
2. Open this `android-app` folder in Android Studio, install Android SDK 36 if
   prompted, and run the app on an Android phone with USB host support.
3. Connect the XIAO with a **data-capable USB-C cable**. Tap **Connect sensor**,
   grant USB permission, then tap **Measure now**. The board sends a changing demo
   ratio; the app labels it as test data and does not allow saving it.

For the real optical bench, upload
[`xiao_samd21_turbimeter.ino`](../firmware/xiao_samd21_turbimeter/xiao_samd21_turbimeter.ino)
instead. It reads the ADS1115 and reports an uncalibrated ratio or a specific
sensor error. Its [wiring and test steps](../firmware/xiao_samd21_turbimeter/README.md)
must be checked before connecting the analog circuit.

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
| `TURB1,ERROR,ADC_MISSING` | Example hardware error shown by the app |

The app rejects incomplete/nonfinite values and saves real `RATIO` and `RESULT`
frames automatically, recording their quality status in the CSV. The measurement firmware must not emit `RESULT` until its
calibration and valid measurement range have been verified. For this 850 nm
near-infrared design, decide the reportable unit and range during validation.

Saved readings remain private on the phone and can be shared manually as CSV.
The release app has no network permission or endpoint configured yet.

## Google Play preparation

See [`play-store/release-checklist.md`](play-store/release-checklist.md) for the
store assets, unsigned AAB build, privacy-policy draft, and remaining account,
signing, screenshots, and closed-testing steps.

## Check the parser without Android

With a JDK installed, run from `android-app`:

```sh
mkdir -p /tmp/turbimeter-parser
javac -d /tmp/turbimeter-parser app/src/main/java/org/citizenscience/turbimeter/{MeasurementFrame,LineFramer}.java test/MeasurementFrameCheck.java
java -ea -cp /tmp/turbimeter-parser org.citizenscience.turbimeter.MeasurementFrameCheck
```
