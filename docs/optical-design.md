# Optical design and validation plan

## Target and status

Build one closed turbidimeter that an Android USB-C phone powers and reads.
The app will show a quantitative turbidity value and optionally upload it to the
citizen-science website. The instrument has no battery, screen, or status
LEDs. **The optical bench, firmware, app, and NTU calibration are not yet
validated.** They must be ready before 18 October 2026; community deployment
comes afterward.

The DFRobot SEN0189 sensor and its firmware are retained only as an earlier
prototype. The current KiCad PCB and 3D files also predate this design. The
[one-unit purchase list](shopping-list-prototype-usb-c.xlsx) is the source for
current parts, supplier URLs, and VAT-inclusive prices.

## Optical and electrical concept

<img src="diagrams/optical-geometry.svg" width="700" alt="90-degree optical geometry">

An 850 nm IR LED illuminates the sample. One BPW34 photodiode measures light
scattered at 90°; a second, shielded from the sample, monitors the LED. Read
both photodiodes with the LED off and on, subtract the dark readings, and form
a scattered/reference ratio. This ratio compensates for some changes in LED
output, but **it is not an NTU value** by itself.

The intended signal chain is:

| Stage | Planned part / connection |
| --- | --- |
| Illumination | 850 nm IR LED controlled by XIAO SAMD21 D7 through a verified current-limited circuit |
| Detection | Two BPW34 photodiodes: 90° scatter and shielded reference |
| Amplification | MCP6002 dual amplifier at 3.3 V; start with 1 MΩ feedback resistors and tune using measured signal levels |
| Conversion | ADS1115 at 3.3 V, with the two amplifier outputs on AIN0 and AIN1; I²C to XIAO D4/D5 |
| Phone interface | USB-C XIAO SAMD21, data-capable 2 m USB-C cable, Android USB host app |

Prototype the analog chain and verify input range, saturation, ambient-light
rejection, and repeatability before revising the PCB. The old
[`legacy-circuit-schematic.svg`](diagrams/legacy-circuit-schematic.svg) uses a
TL072 and status LEDs; it is **not** a schematic for this build. Likewise,
[`firmware/firmware.ino`](../firmware/firmware.ino) is the older sketch that
outputs a relative index and controls LEDs. The current USB bench sketch is
[`xiao_samd21_turbimeter.ino`](../firmware/xiao_samd21_turbimeter/xiao_samd21_turbimeter.ino).

## Enclosure and USB cable

Use the Goobay 66508 USB-C-to-USB-C data cable (2 m). Its specified outer
diameter is **5.5 ± 0.15 mm**; the selected TinyTronics M12 gland clamps
**3–6.5 mm** cable. Cut the device-side plug, pass the cable through the
gland, and terminate USB power, USB 2.0 data, and the required USB-C CC
connection to the board. Confirm continuity, Android USB host operation,
strain relief, and sealing before closing the enclosure. Verify final lid
dimensions before selecting an O-ring from the assortment (maximum 28 mm
inner diameter).

## From optical ratio to reliable NTU

1. Fix the final optical geometry and record LED-off/LED-on readings from both
   channels, including the clear-water background.
2. Measure known turbidity standards spanning the intended range with the
   same sample vessels and handling procedure. The Hanna HI98703-11 set is a
   purchase candidate; check ULB stock, vessel compatibility, and quote.
3. Fit a calibration curve only after inspecting the measured response.
   Store its coefficients and calibration identity with the device/app data.
4. Check independent samples and repeat measurements. Record the range,
   error, repeatability, drift, and any region where the sensor saturates.
5. Show/upload NTU only when the calibration is valid for that range; otherwise
   label the reading as an uncalibrated optical ratio or out of range.

The Android app prototype is present, but the citizen-science API has not yet
been specified. Upload must remain optional. A published measurement record
should include the reading, unit, calibration identity, timestamp, and quality
status.
