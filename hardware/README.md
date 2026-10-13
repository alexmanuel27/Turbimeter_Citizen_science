# Two-floor electronics — upper revision 0.10

Design: **Ing. Alex M. Rivera**. Updated **13 October 2026**.

The upper PCB is aligned to the user's routed base. **The base PCB, schematic,
project settings and fabrication exports were not edited during this alignment.**
The upper board has no tracks; its existing F.Cu ground zone was retained and
refilled after moving components.

| Floor | KiCad 9 project | State |
| --- | --- | --- |
| Base — optics and cable entry | [esque.kicad_pro](esque.kicad_pro) | User routing retained as supplied |
| Upper — controller and ADC | [controller/controller.kicad_pro](controller/controller.kicad_pro) | Revision 0.10; placement checked; route manually |

![Current placement and renders](pcb-top-preview.svg)

[Connected-board drawing](stack-assembly.svg) · [Validation](validation.md)

## Mounting faces

The revised base uses **B for through-hole components** and **F for SMD**.
The upper board follows that physical orientation:

| Physical face in the assembled instrument | Native KiCad face | Upper components |
| --- | --- | --- |
| UP, away from the board below | B | XIAO A1, upright ADS1115 U2, J5 wire heads |
| BELOW, solder side | F | Female SMD sockets J1/J4, C5, through-hole solder joints |

Route the **upper PCB on F.Cu only**. Its soldering remains on one physical
face. KiCad retains conventional plated through-hole annuli on both logical
copper layers. Export the selected manufacturing layer after routing checks.

The base currently contains **86 F.Cu segments, 7 B.Cu segments and one F.Cu
zone**, as supplied. This alignment does not change or approve that routing.
Its existing TOP/BOTTOM legends and revision text were also preserved; use
the actual F/B component layers when interpreting assembly faces.

## Mechanical matching

The original base contour remains intact. The upper outline matches it except
for filling the lower-left USB cable relief; both retain the cuvette opening.
The nominal cuvette exterior is **12.5 × 12.5 mm**. A 44 mm Dwgs.User circle
marks the available enclosure diameter and is not a milling line.

All coordinates below are the **shared native KiCad coordinates**. Viewing
from the physical B side mirrors the drawing.

| Upper part | Position, mm | Native orientation | Purpose |
| --- | --- | --- | --- |
| J1 | 21.17175, 29.020 | 0°, F | Six mating contact axes match base J1 |
| J4 | 32.20675, 47.185 | 0°, F | Four mating contact axes match base J4 |
| A1 | 31.500, 37.500 | 90°, B | XIAO, USB toward the left in shared coordinates |
| U2 | 43.500, 44.660 | 90°, B | Upright ADS1115 on the right wing |
| J5 | 38.200, 47.600 | 180°, B | Accessible wire holes below the XIAO body |
| C5 | 40.000, 32.000 | 180°, F | 100 nF decoupling under the module |

Keep **J1/J4 mating axes fixed** while routing. SMD solder-pad centres are
offset from the socket contact axes. All ten contacts were checked by pin
number and net against the base.

J4 uses a local copy of the standard KiCad 2×2 P1.27 SMD socket footprint.
Three silk segments were moved to Fab to clear nearby XIAO pads; contact
geometry and 3D model are unchanged. The schematic references that local copy.

### Stack height

The provisional spacing is **14 mm between corresponding PCB faces**, or a
12.4 mm clear gap for 1.6 mm boards. With roughly 19 mm of upright ADS1115
above the upper board and 1 mm beneath the base, the nominal total is
**35.6 mm**. Allow approximately **36–38 mm before lid and cable clearance**.
The preferred 35 mm total has not been verified.

Select actual long-reach male headers, compatible SMD sockets and module
supports. Trim and insulate tails as required. Verify real optical bodies,
connector insertion, USB plug and cable bends with the printed enclosure.

## Electrical connections

| Interboard connector | Pin order, identical across both boards |
| --- | --- |
| J1 — 1×6, 2.00 mm | 1 GND; 2 3V3; 3 LED_DRIVE; 4 SCATTER_OUT; 5 REFERENCE_OUT; 6 GND |
| J4 — 2×2, 1.27 mm | 1 USB_VBUS; 2 GND; 3 USB_DM; 4 USB_DP |

Base J1 pin 1 is at **21.17175, 24.020 mm**; subsequent contacts advance +Y
by 2 mm. Base J4 pin 1 is at **32.84175, 46.550 mm**; pin 2 is 1.27 mm to
the left, and the second row advances +Y by 1.27 mm.

**The cable wire-hole numbering differs between boards after the user's
base revision. Follow net names, not matching hole numbers:**

| Wire hole | Base J2 | Upper J5 |
| --- | --- | --- |
| 1 | USB_DM (D−) | USB_VBUS |
| 2 | USB_DP (D+) | GND |
| 3 | USB_VBUS | USB_DM (D−) |
| 4 | GND | USB_DP (D+) |

J2/J5 are bare 1 mm through-holes in a 2×2 group at 2 mm pitch. Insert wires
from the component side and solder on the opposite side. Secure both cables
mechanically so that their joints carry no cable load.

The USB path is **phone → cable/OTG termination → base J2 → J4 → upper J5
→ compact USB-C male pigtail → XIAO socket**. The XIAO edge pins do not expose
D+/D−; edge pin 14 remains unconnected because it is after the protection diode.

No carrier CC net exists. A phone-facing captive/legacy plug needs suitable
local Rd termination or a proper OTG adapter; a normal C-to-C cable cannot
lose its CC path and still be assumed to work. The micro-facing plug must be
legacy USB2-compatible. Verify actual plug construction, continuity and both
phone orientations. See [Chromium's cable guide](https://chromium.googlesource.com/playground/chromium-org-site/+/refs/heads/main/chromium-os/cable-and-adapter-tips-and-tricks/index.md).

The left-facing XIAO socket has a provisional compact plug-tail allowance,
not a verified moulded-plug fit. Keep D+/D− short and together, with a sound
return path and away from high-impedance analog wiring.

J1 feeds U2 AIN0 (scatter) and AIN1 (reference). XIAO D4/D5 provide SDA/SCL;
ADDR is grounded for address `0x48`. The 3.3 V rail supplies the analog circuit.
Gain, LED current, noise, saturation and phone independence require bench tests.
The supplied ADS model prints a different analog-pin order; verify the actual
module. No pin mapping was changed to match a generic 3D asset.

## Checks and renders

Check the upper board against the current base:

```sh
/Applications/KiCad/KiCad.app/Contents/Frameworks/Python.framework/Versions/3.9/bin/python3 hardware/check_stack.py --match-base
```

This mode checks upper ERC, DRC, schematic parity, the module envelope and
all mating contacts. It permits unfinished upper routing and does **not**
approve base DRC findings. The older all-board check describes the earlier
unrouted, B.Cu fabrication stage and is not the current alignment check.

[3D model notes](3dmodels/README.md) describe the supplied assets and their
limits. [Current enclosure meshes](../3D/README.md) are `cuerpo.stl` and
`tapa.stl`. The renders show individual boards and the connected stack, with
the B component faces physically upwards. They do not include the enclosure
or prove sealing, module dimensions or a complete cable fit.

Regenerate the views with KiCad Python:

```sh
/Applications/KiCad/KiCad.app/Contents/Frameworks/Python.framework/Versions/3.9/bin/python3 hardware/render_assembly.py
```

The supplied source models remain unchanged. [BOM notes](../docs/BOM.md)
list unpriced connectors and replacements. Generate final fabrication outputs
only after routing and fit checks; calibrate and validate before reporting
quantitative turbidity.
