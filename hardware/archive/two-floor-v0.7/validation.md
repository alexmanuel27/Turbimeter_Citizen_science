# Revision 0.7 placement and connectivity checks

Design: **Ing. Alex M. Rivera**. Checked 11 October 2026 with KiCad 9.0.7.

| Project | ERC violations | Placement DRC violations | PCB/schematic differences | Intentional unrouted items |
| --- | ---: | ---: | ---: | ---: |
| Sensor / base | 0 | 0 | 0 | 40 |
| Controller + ADC | 0 | 0 | 0 | 21 |

These are **unrouted boards**, not a manufacturing release. Manual routing
must resolve the 61 unconnected items. No rule exclusions were introduced.
Local footprint variants adjust silkscreen without changing copper geometry.

The then-current `check_stack.py`, with `--unrouted`, checked the following.
That script revision is not included in this archived folder; this is a
historical result, not a runnable current validation:

- All 18 original electronic components occur once across the two floors.
- Their connected pins retain the reviewed monolithic circuit's nets.
- The base's Edge.Cuts primitives exactly match the archived proven contour.
- Upper geometry differs only at the filled lower-left cable relief;
  both cuvette openings remain clear at the reference point.
- Electronics are on F; only upper J1/J4 male headers are on B.
- Mating contact axes and net identities match pin for pin on J1 and J4.
- USB mappings match at base J2/J4 and upper J4/J5; XIAO edge 5V stays NC.
- Nominal XIAO and provisional ADS component-envelope corners fit the
  44 mm cylinder; this is a 2D estimate, not a measured module fit.
- Both PCB title blocks credit **Ing. Alex M. Rivera**; tracks/zones are absent.

Schematics, board previews and the [assembly drawing](stack-assembly.svg)
were rendered and inspected. The external J5 pigtail connection to XIAO's
USB socket is an assembly instruction; schematic parity does not verify it.
Actual cable/CC continuity, both phone plug orientations, USB signal integrity,
connector reach, module dimensions, optical bodies, upright ADC/J4 tail
clearance and mechanical support require a physical dry fit and bench test.
The preferred 35 mm height is not demonstrated; the nominal estimate is
35.6 mm, provisionally allow 36–38 mm before lid and cable-bend allowances.

Analog gain, feedback compensation, LED current, supply stability, noise,
phone independence and quantitative turbidity still need bench tests and
calibration. Generate manufacturing outputs only after routing and fit checks.
