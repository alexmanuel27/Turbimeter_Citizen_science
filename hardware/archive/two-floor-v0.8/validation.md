# Revision 0.8 placement and connectivity checks

Design: **Ing. Alex M. Rivera**. Checked 12 October 2026 with KiCad 9.0.7.

| Project | ERC violations | Placement DRC violations | PCB/schematic differences | Intentional unrouted items |
| --- | ---: | ---: | ---: | ---: |
| Sensor / base | 0 | 0 | 0 | 40 |
| Controller + ADC | 0 | 0 | 0 | 21 |

These are **unrouted boards**, not a manufacturing release. Manual routing
must resolve the 61 unconnected items. No rule exclusions were introduced.
Local footprint variants provide mounting marks and models. Conventional PTH
annuli are retained in KiCad; export B.Cu only for single-face manufacture.

The then-current `check_stack.py`, with `--unrouted`, checked the following.
That script revision is not included in this archived folder; this is a
historical result, not a runnable current validation:

- All 18 original electronic components occur once across the two floors.
- Their connected pins retain the reviewed monolithic circuit's nets.
- The base's Edge.Cuts primitives exactly match the archived proven contour.
- Upper geometry differs only at the filled lower-left cable relief;
  both cuvette openings remain clear at the reference point.
- THT parts and male headers are on TOP; all SMD, upper female sockets
  and cable solder pads are on BOTTOM. B.Cu is the sole routing layer; no vias.
- Every populated component has an existing 3D model; bare cable solder
  lands intentionally have none. The restored LED points toward the cuvette.
- Mating contact axes and net identities match pin for pin on J1 and J4.
- USB mappings match at base J2/J4 and upper J4/J5; XIAO edge 5V stays NC.
- Nominal XIAO and supplied ADS board-body corners fit the
  44 mm cylinder; this is a 2D estimate, not a measured module fit.
- Both PCB title blocks credit **Ing. Alex M. Rivera**; tracks/zones are absent.

Schematics, board previews and the [assembly drawing](stack-assembly.svg)
were rendered and inspected. The external J5 pigtail connection to XIAO's
USB socket is an assembly instruction; schematic parity does not verify it.
Actual cable/CC continuity, both phone plug orientations, USB signal integrity,
connector reach, module dimensions, optical bodies, upright ADC and base header tail
clearance and mechanical support require a physical dry fit and bench test.
The preferred 35 mm height is not demonstrated; the nominal estimate is
35.6 mm, provisionally allow 36–38 mm before lid and cable-bend allowances.

Analog gain, feedback compensation, LED current, supply stability, noise,
phone independence and quantitative turbidity still need bench tests and
calibration. Generate manufacturing outputs only after routing and fit checks.

The supplied ADS model prints A3/A2/A1/A0, while the schematic uses
A0/A1/A2/A3. Its geometry is visual guidance only; confirm the real module
before fabrication. No electrical pin mapping was changed based on the STEP.
