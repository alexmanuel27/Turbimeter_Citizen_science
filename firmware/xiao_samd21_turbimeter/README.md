# XIAO SAMD21 optical firmware

Upload [`xiao_samd21_turbimeter.ino`](xiao_samd21_turbimeter.ino) to the
**original Seeed Studio XIAO SAMD21** (Arduino board: **Seeeduino XIAO**).
This is the current USB-C bench firmware. The older
[`firmware.ino`](../firmware.ino) targets a Nano/TL072/indicator-LED circuit;
do not upload it for this build.

## Planned wiring to verify with the electronics technician

| XIAO / sensor | Connection |
| --- | --- |
| USB-C | Power and USB CDC data from the Android phone or test computer |
| 3V3, GND | Common 3.3 V supply and ground for ADS1115 and MCP6002 |
| D4 (SDA), D5 (SCL) | ADS1115 I²C; ADDR to GND gives address `0x48` |
| D7 | Control of the current-limited 850 nm LED circuit |
| ADS1115 AIN0 | MCP6002 output from the 90° scatter photodiode |
| ADS1115 AIN1 | MCP6002 output from the LED reference photodiode |

**Do not reuse the old 5 V Nano wiring unchanged.** XIAO GPIO and I²C are
3.3 V. Power the ADS1115/MCP6002 analog circuit at 3.3 V for this test, and
confirm the LED resistor/driver current and both amplifier output ranges before
connecting them. The old PCB and drawing are not the final XIAO circuit.
See the [XIAO pin map](https://wiki.seeedstudio.com/Seeeduino-XIAO/),
[ADS1115 datasheet](https://www.ti.com/lit/ds/symlink/ads1115.pdf), and
[MCP6002 specifications](https://www.microchip.com/en-us/product/MCP6002).

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
been verified yet.

**The ratio is not turbidity in FNU/NTU.** The firmware deliberately sends no
`TURB1,RESULT` until standards, a calibration curve, valid range, and
independent-sample checks have been completed. The XIAO USB link has been
checked; ADC and optical readings still need physical verification.
