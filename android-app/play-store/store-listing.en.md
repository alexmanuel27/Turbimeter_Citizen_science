# Turbimeter — Google Play listing draft

Design: **Ing. Alex M. Rivera**.

## App name

WaterLab Turbidimeter

## Short description

View and save readings from a compatible USB-C water turbidity sensor

## Full description

WaterLab Turbidimeter turns an Android phone into the display and field log for the Cuban Water Lab USB-C turbidity sensor. A compatible external sensor is required; the app cannot measure water on its own.

Connect the sensor, request a reading, and see its status on screen. Real readings are saved on the phone with a UTC timestamp and can be exported as CSV. The app distinguishes an uncalibrated optical ratio from a calibrated turbidity result. A ratio must not be interpreted as an FNU or NTU value.

Use Calibration to pair sensor readings with values measured by a reference turbidimeter, then save a curve for that sensor. Values outside the measured reference range are extrapolated and clearly labelled. Keep the original optical ratios and calibration identifiers in your history, and export calibration files for use with the same sensor on another phone. Calibration accuracy must be checked using independent reference samples.

The current version works offline. It does not request location or upload readings. It has no account, advertising, or analytics.

This app is for the compatible citizen-science instrument. Measurement reliability depends on the sensor hardware and its calibration; do not use uncalibrated readings to make water safety decisions.

## Screenshots

Genuine app screens are in `assets/screenshots/`: Home, History, and Privacy. These older screenshots predate the Calibration page and must be refreshed before publishing version 0.5. They were captured on the Android API 35 emulator at 1080 × 1920 without a sensor or illustrative readings. Replace them with phone screenshots from the field instrument when those are available.

## Graphic alt text

- Icon: ARES Cuban Water Lab logo with a water wave and optical sensor symbol.
- Feature graphic: Blue water layers crossed by a warm light beam and scattering rings.
