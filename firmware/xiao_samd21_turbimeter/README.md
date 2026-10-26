# XIAO SAMD21 optical firmware

Design: **Ing. Alex M. Rivera**.

Upload [`xiao_samd21_turbimeter.ino`](xiao_samd21_turbimeter.ino) to the
**original Seeed Studio XIAO SAMD21** (Arduino board: **Seeeduino XIAO**).
This is the current USB-C bench firmware. The older
[`firmware.ino`](../firmware.ino) targets a Nano/TL072/indicator-LED circuit;
do not upload it for this build.

## Active wiring and bench checks

| XIAO / sensor | Connection |
| --- | --- |
| USB-C | Power and USB CDC data from the Android phone or test computer |
| 3V3, GND | Common 3.3 V supply and ground for ADS1115 and MCP6002 |
| D4 (SDA), D5 (SCL) | ADS1115 I²C; ADDR to GND gives address `0x48` |
| D10 (A1 pin 11 in KiCad) | Control of the current-limited 850 nm LED circuit |
| ADS1115 AIN0 | MCP6002 output from the 90° scatter photodiode |
| ADS1115 AIN1 | MCP6002 output from the LED reference photodiode |

**Do not reuse the old 5 V Nano wiring unchanged.** XIAO GPIO and I²C are
3.3 V. Power the ADS1115/MCP6002 analog circuit at 3.3 V for this test, and
confirm the LED resistor/driver current and both amplifier output ranges before
connecting them. The [two-board hardware design](../../hardware/README.md)
links the independent sensor and controller projects. A prototype has been
assembled and tested; replication and fabrication still need design checks.
See the [XIAO pin map](https://wiki.seeedstudio.com/Seeeduino-XIAO/),
[ADS1115 datasheet](https://www.ti.com/lit/ds/symlink/ads1115.pdf), and
[MCP6002 specifications](https://www.microchip.com/en-us/product/MCP6002).

### Testing the controller without the sensor/base board

The saved controller PCB has two separate ground islands. XIAO A1 pin 13
and ADS1115 U2 pin 2 are joined through the base's J1 contacts 1 and 6 when
the complete stack is connected. For a **controller-only bench test**, with
USB disconnected, connect a short jumper **directly between the marked GND
pins of the XIAO and ADS1115**. Verify ADS VDD is supplied from XIAO **3V3**.
Keep this common-ground connection during standalone tests.

Use `PING` and `RAW` to check communication and acquisition. Without the
analog sensor/base, floating AIN0/AIN1 readings do not represent an optical
signal or turbidity. `MEASURE`/`DIAG` optical errors are not a validation of
this incomplete setup.

**Pin revision, 25 October 2026:** LED control moved from D7 (KiCad A1 pin 8)
to **D10 (KiCad A1 pin 11)**. The Arduino constant is `LED_PIN = 10`,
not `11`. Earlier bench results below used the previous D7 connection.
Load the revised firmware after moving the LED connection.

## Upload and first tests

1. In Arduino IDE, add Seeed's boards URL
   `https://files.seeedstudio.com/arduino/package_seeeduino_boards_index.json`,
   install **Seeed SAMD Boards**, select **Seeeduino XIAO** and its USB port,
   then open this `.ino` and click **Upload**. The sketch needs only `Wire.h`.
2. With the XIAO connected to a computer, open Serial Monitor at **115200 baud**
   with **Newline** line ending. Send `PING`; expect `TURB1,PONG`. Send
   `MEASURE` before wiring the ADC; expect `TURB1,ERROR,ADC_MISSING`.
3. After wiring, send `DIAG`. The reply is
   `TURB1,DIAG,darkScatter,litScatter,darkReference,litReference,ratio`.
   The four signals are averaged ADS1115 counts (0.125 mV/count at the chosen
   gain). Check that the LED changes both channels, the reference difference
   is clearly above the noise, and neither input approaches 26000 counts.
4. Send `MEASURE`; expect `TURB1,RATIO,<number>`. Then connect the XIAO to an
   Android USB-C phone, tap **Connect sensor** and **Measure now** in the app.

If `DIAG` reports an optical error such as `SATURATED`, send `RAW` to inspect
`TURB1,RAW,darkScatter,litScatter,darkReference,litReference`. These four
averaged ADC counts remain available even when optical validation fails.
`RAW` uses the same LED-off/on acquisition and I²C error checks, but produces
**no ratio or turbidity result**. `MEASURE` and `DIAG` retain their validation.

The sketch takes four LED-off/on samples per request, subtracts the dark
signals, and divides corrected scatter by corrected reference. Adjust the two
`*_POLARITY` constants if an amplifier output falls rather than rises when
illuminated; use `DIAG` to decide. `MIN_REFERENCE_COUNTS`,
`SATURATION_COUNTS`, and `LED_SETTLE_MS` are bench settings to revisit after
measuring the actual circuit. I²C failures, weak reference, negative scatter,
and near-rail signals return explicit `TURB1,ERROR,...` messages to the app.

**Bench result, 2 October 2026:** this sketch uploaded and verified on the
connected XIAO. `PING` returned `TURB1,PONG`; `MEASURE` and `DIAG` both
returned `TURB1,ERROR,ADC_MISSING`, as expected with only the XIAO plugged in.
The ADS1115 and optical circuit were not connected, so no optical reading has
been verified at that date.

**Bench result, 24 October 2026:** after the builder repaired power/SDA wiring,
the ADS responded at `0x48`, its configuration register read `0x8583`, and
USB commands remained responsive. With the enclosure reported dark and an
alcohol-filled cuvette installed, five `RAW` acquisitions gave these ranges:

| Signal | ADC counts | Approximate voltage |
| --- | --- | --- |
| Scatter, LED off | 26364–26365 | 3.296 V |
| Scatter, LED on | 26369–26371 | 3.296 V |
| Reference, LED off | 26362–26364 | 3.295 V |
| Reference, LED on | 26370–26371 | 3.296 V |

At that stage, `MEASURE` continued returning `TURB1,ERROR,SATURATED`, as
required; no valid optical ratio or turbidity had been established. The builder reports **100 kΩ**
fitted for both R1/R2, rather than the schematic's 1 MΩ. Keep the fitted values
for diagnosis until the MCP6002 supply and nominal **1.65 V VREF** are checked.
The alcohol sample was not a calibration standard. The measurement firmware,
including `RAW`, is loaded; the earlier temporary bus-only diagnostic is not.

**Recovery in the same session:** the builder initially measured VREF at 3.3 V
and subsequently repaired it to approximately **1.6 V**, while keeping R1/R2
at 100 kΩ. Both ADC channels then left saturation and responded to LED off/on.
A first 20-reading run was made with the enclosure open, as confirmed by the
builder, and is not used to assess closed-enclosure repeatability.

After the builder closed the enclosure and installed the alcohol-filled cuvette,
20 consecutive `MEASURE` requests returned real optical ratios without errors:

| Closed-enclosure check | Result |
| --- | --- |
| Mean optical ratio | 0.646405 |
| Minimum / maximum | 0.6453 / 0.6479 |
| Sample standard deviation | 0.00054336 |
| Coefficient of variation | 0.0841% |
| Median / maximum host response time | 203 / 213 ms |
| Final scatter, LED off / on | 13260 / 14166 counts |
| Final reference, LED off / on | 13307 / 14709 counts |

This is short-run repeatability of an **uncalibrated optical ratio**, not
turbidity accuracy or validation across samples. See the
[recorded frames](bench/2026-10-24-closed.jsonl) and
[summary](bench/2026-10-24-closed-summary.json). Android operation with this
assembled sensor remains to be checked.

**The ratio is not turbidity in FNU/NTU.** The firmware deliberately sends no
`TURB1,RESULT` until standards, a calibration curve, valid range, and
independent-sample checks have been completed. The XIAO USB link has been
checked, and ADC communication, LED response and uncalibrated optical readout
are verified on the Mac. Calibration and independent-sample validation remain
pending.

## Controller-only recovery — 26 October 2026

Design: **Ing. Alex M. Rivera**.

The builder replaced the XIAO and confirmed the ADS supply at 3.3 V after
previously reporting 5 V. SDA/SCL wiring faults were repaired. With only the
controller and ADS connected, normal acquisition still blocked until a direct
GND jumper was fitted between the modules. The production D10 firmware then
completed **20 / 20 `RAW` acquisitions without errors**, with **203 ms median
and 206 ms maximum** host response time. The temporary diagnostic firmware
was removed; the normal measurement firmware remains installed.

This check validates communication/acquisition in that bench setup only;
the sensor/base was absent. It does not validate analog accuracy, optical
repeatability or turbidity calibration. See the [frames](bench/2026-10-26-controller-only.jsonl)
and [summary](bench/2026-10-26-controller-only-summary.json).

## Closed enclosure / empty cuvette — 26 October 2026

Design: **Ing. Alex M. Rivera**.

After the standalone controller recovery, the builder connected the analog
base, closed the enclosure and confirmed that the cuvette was **empty**.
The normal firmware with LED D10 completed 20 consecutive `MEASURE` requests:

| Empty-cuvette check | Result |
| --- | --- |
| Valid readings / errors | 20 / 0 |
| Mean optical ratio | 6.902305 |
| Minimum / maximum | 6.8945 / 6.9115 |
| Sample standard deviation | 0.00524881 |
| Coefficient of variation | 0.0760% |
| Median / maximum host response time | 203 / 203 ms |
| Final scatter, LED off / on | 13263 / 19649 counts |
| Final reference, LED off / on | 13225 / 14151 counts |

Both optical channels responded to LED switching, and no saturation error
occurred. This is **short-run optical-ratio repeatability for an empty
cuvette**. It is not a zero-turbidity blank, FNU/NTU calibration, turbidity
accuracy or an established measurement range. A filled cuvette and independent
reference measurements remain necessary. The earlier alcohol-filled-cuvette
session used different conditions and is not a direct accuracy comparison.
See the [frames](bench/2026-10-26-closed-empty.jsonl) and
[summary](bench/2026-10-26-closed-empty-summary.json).

Water-filled-cuvette testing was deferred after this session. No further
physical measurement or calibration was performed during repository organization.
