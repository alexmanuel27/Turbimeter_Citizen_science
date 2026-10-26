# Prototype purchasing and as-built notes

Design: **Ing. Alex M. Rivera**. Updated **26 October 2026**.

## Purchase workbook

The current file for one prototype is
[`shopping-list-prototype-usb-c.xlsx`](shopping-list-prototype-usb-c.xlsx),
the latest user-edited workbook formerly at the repository root. Its contents
were moved unchanged. The earlier 29 September workbook is retained in
[`archive/`](archive/). Do not combine their totals or treat either list as a
new supplier quote: stock, prices, VAT and delivery have not been rechecked
for this repository update.

The old €76.52 subtotal referred to the September list and is not a verified
cost of the assembled instrument. Match the latest workbook to the actual
build and existing stock before ordering. The cuvettes and XIAO are already
available; the current build has no battery or display.

## Current build and differences to check

| Item | Current design / bench note |
| --- | --- |
| Microcontroller | **XIAO SAMD21**, replacing the earlier Nano; LED drive is D10, KiCad A1 pin 11 |
| ADC | ADS1115 at **3.3 V**, on the controller board with the XIAO |
| Optical chain | 850 nm LED, two BPW34 photodiodes, MCP6002; see the saved base schematic |
| R1/R2 feedback resistors | Builder reports **100 kΩ** fitted; original schematic starting value was 1 MΩ. Replication and calibration must document the actual gain |
| Reference divider | Nominal VREF 1.65 V; approximately 1.6 V reported after repair |
| J1 interboard connector | **1×6, 2.54 mm**; base THT female socket, controller SMD male header. Select mating height/reach from the actual assembly |
| Carrier PCBs | Two independent KiCad projects: base and controller; use saved files, not older renders or Gerbers, as the design source |
| USB termination | Four USB2 power/data conductors, with USB-C host/device detection handled correctly by plugs/OTG. Earlier carrier J2/J4/J5 footprints have been removed from the saved boards |
| Enclosure | Current [body/lid](../3D/README.md); verify gland clamping, actual cable diameter and O-ring fit |
| Stack supports | Verify connector engagement, module support, screw fit, insulation, cable bends and lid clearance physically |

The old **2.00 mm J1** purchasing suggestion is superseded by 2.54 mm.
Earlier J4 USB-link connector suggestions describe a retired layout; order
only what the saved design and actual cabling require. The nominal cuvette
exterior remains 12.5 × 12.5 mm and the cylinder allowance is 44 mm. The
preferred 35 mm stack height was a design target, not a verified dimension.

For exact passive package sizes, values and quantities use the saved
[base](../hardware/esque.kicad_sch) and
[controller](../hardware-controller/controller.kicad_sch) schematics, then
reconcile them with the assembled prototype. A stable optical ratio does not
remove the need to record as-built differences and recalibrate after changes.

[Hardware notes](../hardware/README.md) ·
[Controller notes](../hardware-controller/README.md) ·
[Bench records](../firmware/xiao_samd21_turbimeter/bench/README.md)
