# Standalone controller / ADC project

Design: **Ing. Alex M. Rivera**. Separated **19 October 2026**.

**Current source, 26 October:** the saved PCB includes subsequent builder
routing and placement changes. The dated sections below record earlier work;
their counts, positions and checks must not be read as a validation of later
edits. Existing [Gerbers](gerber/README.md) and assembly previews are snapshots.
The repository organization preserves this project and its routing unchanged.

Open **[controller.kicad_pro](controller.kicad_pro)** directly in KiCad 9.
The schematic and PCB have the same basename so KiCad opens the correct pair.
This folder is independent of the sensor/base project: its custom symbols,
footprints, required 3D models and Gerber output directory are local.
It uses the standard libraries and 3D models supplied with KiCad 9.

| Location | Contents |
| --- | --- |
| `controller.kicad_sch` | Existing controller schematic |
| `controller.kicad_pcb` | User's current PCB, including tracks and zone fills |
| `controller.kicad_pro` | Project settings and design rules |
| `Turbimeter.kicad_sym`, `Turbimeter.pretty/` | Project-local custom libraries |
| `3dmodels/` | Required supplied/derived module models |
| `gerber/` | Existing controller exports, moved as supplied; destination for new plots |

The separation preserves schematic contents, board geometry, component
positions, nets, tracks, zones and design rules. Only library/model lookup
paths and the plot-output directory were changed. The sensor/base project
was left untouched. The folder can be moved with its contents without
requiring the sensor project's libraries.

The existing design is still work in progress. Before moving, its DRC report
contained 2 findings, 2 unconnected items and 2 schematic-parity findings.
Separation does not repair or approve those existing findings. The older
assembly previews and alignment report describe a previous placement revision;
use this PCB as the current editable source and regenerate fabrication outputs
after completing your checks.

## J1 alignment — 20 October 2026, revision 0.11

Design: **Ing. Alex M. Rivera**.

J1 is now a **male SMD 1×6 header, 2.54 mm pitch**, on **F.Cu** (the
controller's underside), to mate with the female connector on the base.
Its footprint is
`Connector_PinHeader_2.54mm:PinHeader_1x06_P2.54mm_Vertical_SMD_Pin1Right`.
The schematic uses the same footprint. Both boards share the same XY frame.

| Contact | X (mm) | Y (mm) | Signal |
| --- | --- | --- | --- |
| 1 | 20.375 | 23.545 | GND |
| 2 | 20.375 | 26.085 | 3V3 |
| 3 | 20.375 | 28.625 | LED_DRIVE |
| 4 | 20.375 | 31.165 | SCATTER_OUT |
| 5 | 20.375 | 33.705 | REFERENCE_OUT |
| 6 | 20.375 | 36.245 | GND |

The footprint origin is **X 20.375, Y 29.895 mm, rotation 0°**. These are
the mating contact axes; the SMD solder pads alternate to either side.
All six contact axes and schematic net assignments were checked against
the saved base. The base file, other controller placements and all 49 existing
track/via items were preserved. Zone fills were cleared for the changed pads.

**Routing and fabrication checks remain pending.** Old J1 traces now cross
new pads on other nets and must be rerouted before powering the board.
Pad 2 has only **0.0607 mm copper-to-edge clearance**, below the project's
0.5 mm rule; resolve this with a mechanically verified connector land pattern
or a local upper-board outline change before fabrication. The outline has
been preserved. Ensure the base socket faces the controller header during
assembly: its current F-side model conflicts with the earlier B-side-up base
mounting convention. Through-hole placement must follow the actual assembly.

The existing Gerbers and assembly previews predate this change. Refill zones,
run DRC and regenerate fabrication outputs after routing.

## M3 mounting hole — 25 October 2026, revision 0.12

Design: **Ing. Alex M. Rivera**.

H1 is aligned with the large via on the base at **X 47.680, Y 36.930 mm**.
Its **3.000 mm non-plated hole** matches the base drill exactly; the controller
reserves a **5.000 mm diameter free of copper**. It is a mechanical mounting
hole and has no electrical net. Its local footprint is
`Turbimeter:MountingHole_3mm_M3`. Confirm that the actual M3 screw passes
through the nominal 3 mm hole before assembly.

Existing component placements, tracks, vias and outline were preserved. The
ground zone was refilled to clear H1. DRC introduced no new findings; the
existing design findings remain to be resolved. The base was not modified.
Regenerate manufacturing files after completing the design.

## Standalone controller test — 26 October 2026

Design: **Ing. Alex M. Rivera**.

The current F.Cu ground fill has two islands: XIAO A1 pin 13 / C5 / J1 pin 1,
and ADS1115 U2 pins 2 and 5 / J1 pin 6. The base's ground plane normally joins
J1 pins 1 and 6. When testing this board **without the base**, fit a short
direct **XIAO GND to ADS GND jumper** with USB disconnected, and confirm
ADS VDD is 3.3 V before reconnecting. Do not rely on the mechanical screw as
an electrical ground return.

Adding that jumper resolved the observed I2C/acquisition blocking on the
assembled standalone controller: 20 consecutive `RAW` requests succeeded
using the normal D10 firmware. See the [bench record](../firmware/xiao_samd21_turbimeter/bench/2026-10-26-controller-only-summary.json).
Analog/optical validation requires the sensor/base. PCB routing was not
changed in this diagnostic session.
