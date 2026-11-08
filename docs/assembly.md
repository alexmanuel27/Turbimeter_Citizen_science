# Assembly and first tests — citizen-science turbidimeter

Design: **Ing. Alex M. Rivera**. Draft for the Havana team, 8 November 2026.

For daily operation and a detailed calibration walkthrough, see the
[app user guide](app-user-guide.md).

This guide covers the XIAO SAMD21 / ADS1115 two-board prototype. One assembled
instrument has been sent to Havana together with parts for a second. Use the
assembled unit as a **physical reference**, and document any differences in
the second build. Steps marked **confirm** require checking the actual parts:
the fit of the latest Fusion exports and the complete as-built parts list need confirmation.

## 1. Gather the parts and tools

- Sensor/base PCB and controller PCB; open their respective KiCad 9 projects:
  [base](../hardware/esque.kicad_pro) and
  [controller](../hardware-controller/controller.kicad_pro). The saved PCB
  and schematic are the reference for each board. Existing
  [base](../hardware/gerber/README.md) and
  [controller](../hardware-controller/gerber/README.md) Gerbers are snapshots:
  verify against the saved design before ordering additional boards.
- XIAO SAMD21, ADS1115 module, MCP6002-E/P, two BPW34 photodiodes and one
  850 nm IR LED. Confirm the supplied module's pin labels and LED polarity.
- Resistors, capacitors and the matching 1×6, 2.54 mm connectors listed below;
  wires/headers, suitable USB data connection, cable gland, seal and fasteners.
- Spectrophotometer cuvette, 10 mm optical path; 12.5 × 12.5 mm outside is
  a provisional fit assumption. Printed enclosure and optical support parts.
- Soldering iron and solder, multimeter, computer with Arduino IDE, compatible
  Android USB-host phone, data cable and clean water. A reference turbidimeter
  and samples with different turbidity are needed for calibration.

The old [purchase workbook](shopping-list-prototype-usb-c.xlsx) is a stock
reference, not a verified as-built bill of materials. Reconcile quantities and
values with the parts supplied and the assembled unit before soldering.

## 2. Prepare the printed parts

1. Open [body_tur.stl](../3D/fusion-export/body_tur.stl) and
   [Tapa_tur.stl](../3D/fusion-export/Tapa_tur.stl): one body and one lid are
   the starting assumption. Compare them with the assembled instrument. The
   [OBJ and material](../3D/fusion-export/) show two named groups; they have
   different numerical scaling from the STL files, so check import units.
2. Confirm intended units and dimensions with the physical instrument or
   Fusion before printing. If STL coordinates are in mm, the body bounds are
   approximately 62 × 62 × 93 mm and the lid 62 × 62 × 22.5 mm. Place each
   part on the slicer's bed and select orientation and supports after inspection.
   Remove supports without damaging optical alignment surfaces.
3. Dry-fit the cuvette, boards, lid, gasket and gland. Verify the real cable
   diameter against the gland, the seal groove against the O-ring, and the
   available connector, screw and cable clearances. Do not force the cuvette.
   The meshes alone do not establish assembled height, orientation or sealing.

## 3. Populate the sensor/base board

With power disconnected, use the saved PCB, schematic and component markings
to locate each reference. Orientation in the KiCad editor depends on the face
being viewed; follow the physical board and the assembled unit. Confirm that
SMD contacts can be soldered on the intended copper face and through-hole
parts face into the enclosure. Do not rely on older render positions.

| References | Value / part | Notes |
| --- | --- | --- |
| R1, R2 | **100 kΩ fitted in the tested prototype** | Saved PCB labels say 1 MΩ. Confirm actual parts and record fitted values; both channels need the intended matching gain. |
| R3, R4 | 10 kΩ each | VREF divider. |
| R5, R6 | 220 Ω each | LED current limiting, in series. |
| C1, C2 | 10 pF each | Feedback capacitors. |
| C3, C4 | 100 nF each | VREF and 3.3 V decoupling. |
| C6 | 10 µF, 16 V | Polarized: positive to 3V3, negative to GND; verify actual marking. |
| U1 | MCP6002-E/P | Align the notch/pin 1 with the PCB and datasheet. Pin 8 = 3V3, pin 4 = GND. |
| D1, D2 | BPW34 | D1 scatter, D2 reference. Pin 1 is the summing input (cathode in the design), pin 2 GND; verify the supplied part's pinout. |
| D3 | 850 nm LED | PCB pad 1 is GND, pad 2 LED_ANODE; verify actual cathode/anode before soldering. |
| J1 | 1×6 female socket, 2.54 mm pitch | Match the male connector on the controller and the actual assembly height. The saved base value text still says `2mm`; the saved footprint is 2.54 mm. |

1. Fit and solder the small resistors and capacitors. Check values before
   installation and inspect adjacent pads for bridges.
2. Fit U1 and C6 with correct orientation. Fit J1 on the physical face that
   mates with the controller; verify its pin 1 and the real mating reach.
3. Dry-fit the LED and photodiodes with the cuvette and optical support in
   place **before** final soldering. The LED illuminates the cuvette; D1 views
   the sample at 90° and D2 views a fixed fraction of LED light through a
   shielded reference path. Keep the optical faces in their designed positions
   and avoid stressing their leads. Confirm the new printed parts' geometry.
4. Check polarity, joints, bridges and component clearances with a multimeter.
   Record any deviation from the table and photograph both board faces.

## 4. Populate the controller board

1. Fit C5 (100 nF), the matching 1×6 male J1 and the XIAO/ADS headers according
   to the saved controller PCB. Confirm the real connector orientation and pin
   1 before soldering; allow room for the XIAO USB plug and the ADS support.
2. Mount the XIAO SAMD21 and ADS1115. Match **printed module pin names**, not
   an assumed physical order or an old 3D model. The ADS is upright in the
   earlier design; confirm the actual mounting against the assembled unit.
3. With USB disconnected, verify:

| XIAO / base signal | ADS1115 connection |
| --- | --- |
| XIAO 3V3 | VDD |
| XIAO GND | GND and ADDR |
| XIAO D4 | SDA |
| XIAO D5 | SCL |
| Base scatter output / J1 pin 4 | AIN0 |
| Base reference output / J1 pin 5 | AIN1 |

J1 is **1 GND, 2 3V3, 3 LED_DRIVE, 4 SCATTER_OUT, 5 REFERENCE_OUT,
6 GND**. XIAO **D10** drives the LED through the base resistors; on the KiCad
XIAO symbol it is A1 pin 11. It is not Arduino pin 11. The controller's saved
ground fill has two islands; when testing it **without the base**, fit a
short direct jumper from XIAO GND to ADS GND with USB disconnected. Check
common ground in the final assembly too; never use the mounting screw as the
only ground connection. See [controller notes](../hardware-controller/README.md).

## 5. Check and program before final assembly

1. With USB disconnected, check continuity for SDA, SCL, common GND, ADS ADDR
   to GND and J1 signals; inspect for unintended shorts between 3V3, GND, SDA
   and SCL. Confirm the ADS VDD is connected to XIAO **3V3**, not USB 5V.
2. Install the Seeed SAMD board package in Arduino IDE and select **Seeeduino
   XIAO**, then open and upload the [active firmware](../firmware/xiao_samd21_turbimeter/xiao_samd21_turbimeter.ino).
   The [firmware instructions](../firmware/xiao_samd21_turbimeter/README.md)
   include the board-package URL and troubleshooting.
3. Open the serial monitor at 115200 baud, newline ending. `PING` should return
   `TURB1,PONG`. For a controller-only test, keep the direct ground jumper
   connected. Without the sensor/base, ADC inputs are floating and their
   values do not represent a sample.
4. With the complete boards connected and powered, measure ADS VDD to GND and
   MCP6002 pin 8 to pin 4: both should be about **3.3 V**. Measure VREF to GND:
   approximately **1.65 V** (about 1.6 V on the tested unit). If incorrect,
   disconnect USB and inspect the divider, supply and connections before
   proceeding. Avoid bridging adjacent pins with meter probes.

## 6. Assemble the enclosure and connect the phone

1. Disconnect USB. Fit the sensor PCB and optical parts into the enclosure;
   check the LED, 90° sensor, reference sensor and cuvette alignment. Fit the
   controller board and join J1 pin-for-pin. Confirm no exposed lead touches
   another pad or the enclosure fasteners.
2. Route the actual phone cable through the gland and connect it to the XIAO
   USB socket using the verified cable/plug arrangement of the assembled
   instrument. Provide strain relief and verify seal seating. The current
   saved boards do not include the earlier J2/J4/J5 USB carrier connectors.
   USB-C host detection must work in the chosen cable/adapter: four conductors
   alone do not establish it. Confirm both phone plug orientations.
3. Insert a clean cuvette filled with clean water, free of bubbles; wipe its
   optical faces and close the light-tight enclosure. The final fit and orientation of the new printed parts remain **to be
   confirmed** against the assembled unit and Fusion design. Do not infer waterproofing from a dry fit.

## 7. Test the assembled instrument

1. On the computer, send `PING`, then `DIAG` in the serial monitor. A successful
   response is `TURB1,DIAG,darkScatter,litScatter,darkReference,litReference,ratio`.
   Both channels should change with the LED; values approaching 26000 counts
   are treated as saturated. `RAW` shows the four readings if an optical
   validation error prevents `DIAG`; see the [firmware guide](../firmware/xiao_samd21_turbimeter/README.md).
2. Send `MEASURE` and record several `TURB1,RATIO,...` replies, sample,
   cuvette, enclosure, date, component values and firmware version. A ratio
   is **not** a turbidity value in NTU/FNU. Compare repeated readings under
   unchanged conditions; a different unit may give a different ratio.
3. Disconnect from the computer and connect an Android phone with USB host
   support. Install the app provided by the project team, open it, tap
   **Connect sensor**, grant USB access, then **Measure now**. Confirm that
   real readings appear and are saved in **History**. Without calibration
   the app reports an optical ratio. Test the actual supplied cable and phone.
4. If `ADC_MISSING` or communication hangs, disconnect power and inspect the
   ADS 3.3 V supply, common ground, D4/SDA, D5/SCL and ADDR/GND. If
   `SATURATED` or `REFERENCE_LOW` appears, inspect VREF, optical alignment and
   light shielding. Do not treat a returned error as a measurement.

## 8. Calibrate and document each instrument

Once a reference turbidimeter is available, use the app's **Calibration**
page to pair five sensor readings with each reference result. Use 3–12 samples
spanning the intended range, enter the same unit reported by the reference
instrument, and record reference instrument, dates and sample preparation.
Follow the [app calibration instructions](../android-app/README.md). Test
additional independent samples and document uncertainty and usable range.
Results outside the calibrated range are marked **EXTRAPOLATED** and require
separate validation. A new XIAO has a different sensor identity; calibrate
each unit and repeat calibration when the optics or electronics change.

The app is currently offline and can export history as CSV; it cannot yet
upload data to the website. The 26 October empty-cuvette result in the
[bench log](../firmware/xiao_samd21_turbimeter/bench/README.md) is a short-run
repeatability check, not an acceptance limit for a new instrument. Complete
water-filled tests and independent reference validation before reporting
quantitative turbidity.
