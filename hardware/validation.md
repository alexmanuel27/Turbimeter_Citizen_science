# Upper PCB alignment — revision 0.10

Design: **Ing. Alex M. Rivera**. Checked **13 October 2026**.

> **Historical report, 13 October 2026.** Later user routing, the independent
> controller project, J1 pitch change and mounting hole are not validated by
> this report. See the [current hardware notes](README.md). The historical
> `check_stack.py` assertions are not a current manufacturing approval.

## Scope

The user's routed base is the mechanical and electrical reference. SHA-256
checks confirm that its PCB, schematic and project files were not changed by
this alignment. Existing Gerbers were also left untouched.

The upper PCB has no tracks. Its existing F.Cu ground zone was retained and
refilled for the new placement. Through-hole components mount on B, matching
the base; SMD sockets and C5 mount on F. Upper manual routing should use F.Cu.

## Results

- Upper schematic ERC: **0** findings.
- Upper placement DRC: **0** findings, including library-footprint matching.
- Upper schematic/PCB parity: **0** findings.
- Upper routing: **12** unconnected items remain; no tracks were added.
- All **10** interboard contacts match the base by contact axis and net.
- Upper outline matches the base except for filling its USB cable relief.
- Nominal XIAO and ADS body corners fit within the 44 mm reference cylinder.
- J2's revised base numbering and J5's existing upper numbering are documented
  separately in the [assembly notes](README.md).

Run the reproducible upper alignment check with KiCad Python:

```sh
/Applications/KiCad/KiCad.app/Contents/Frameworks/Python.framework/Versions/3.9/bin/python3 hardware/check_stack.py --match-base
```

## Existing base findings — not modified

The read-only base check found **0 unconnected items**, **0 schematic parity
findings** and **11 DRC findings**: 7 silk-over-copper, 2 silk overlaps,
1 silk-edge clearance and 1 copper-edge clearance. The copper-edge finding
is D3 pad 1, approximately **0.2202 mm** from the edge against the configured
0.5 mm requirement. The base has 86 F.Cu and 7 B.Cu segments plus one F.Cu
zone. These are recorded as received, not accepted as manufacturing-ready.

## Physical checks still required

Select and measure actual interboard connectors and their reach. The base
long-pin extension models are hidden in the user-supplied PCB; that setting
was preserved. Renders therefore show standard short pins and do not establish
contact insertion at the provisional 14 mm spacing. Check trimmed
solder tails, optical bodies, upright ADC support, compact USB plug and cable
bends in the updated enclosure. The nominal stack budget is 35.6 mm; allow
36–38 mm before lid and cable clearance. Printed-part dimensions, sealing and
complete PCB-to-enclosure collision checks have not been verified.

The ADS model's printed A3/A2/A1/A0 order differs from the schematic's
A0/A1/A2/A3. Confirm the real module before fabrication. No electrical mapping
was changed based on the 3D model. Analog performance, calibration and
quantitative turbidity still require the assembled bench tests.
