# Revision 0.6 placement and connectivity checks

Design: **Ing. Alex M. Rivera**. Checked 11 October 2026 with KiCad 9.0.7.

| Project | ERC violations | Placement DRC violations | PCB/schematic differences | Intentional unrouted items |
| --- | ---: | ---: | ---: | ---: |
| Sensor | 0 | 0 | 0 | 32 |
| Controller | 0 | 0 | 0 | 12 |
| ADC | 0 | 0 | 0 | 11 |

These are **unrouted boards**, not a manufacturing release or a completed DRC
pass for routed copper. KiCad reports 55 unconnected items across the stack,
which the manual routing must resolve. No rules or clearance violations were
excluded to obtain the placement results. Local connector footprint variants
retain standard copper pad geometry and adjust silkscreen near the preserved
outline and neighbouring through-hole pads.

The runnable [check_stack.py](check_stack.py) additionally confirms:

- All 18 original electronic components occur once across the three floors.
- Every connected electronic pin retains its net from the reviewed monolithic
  schematic; the six mating connector instances add no circuit changes.
- All Edge.Cuts primitives exactly match the archived proven contour.
- Every electronic component is on F; only upper male interboard connectors
  are on B. Socket mating contact axes and net identities match pin for pin.
- All three PCB title blocks credit **Ing. Alex M. Rivera**.
- No tracks or copper zones are present with `--unrouted`.

The schematics and top-view board previews were rendered and inspected.
The [vertical assembly drawing](stack-assembly.svg) is an estimate, not a 3D
interference check. Module dimensions, socket/header mating heights, XIAO
mounting clearance, C6 height, sideways optical mounting, cable bend, mechanical
support and enclosure clearance remain physical dry-fit checks. The preferred
35 mm height is not demonstrated; allow approximately 45–48 mm for this
provisional assembly before enclosure allowances.

Analog gain, feedback compensation, LED current, supply stability, noise,
phone independence and quantitative turbidity still require bench tests and
calibration. This validation checks schematic connectivity and PCB placement.
