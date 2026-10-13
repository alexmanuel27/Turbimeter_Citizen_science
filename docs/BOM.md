# Prototype purchase list

Design: **Ing. Alex M. Rivera**.

The current purchasing file for **one Android USB-C turbidimeter** is
[`shopping-list-prototype-usb-c.xlsx`](shopping-list-prototype-usb-c.xlsx).
It has one row per product, supplier references, quantities, full product URLs
as plain text, and prices **including VAT** (checked 29 September 2026).

The priced subtotal is **€76.52 including VAT**. This excludes delivery and
the Hanna HI98703-11 turbidity standards, for which a VAT-inclusive quote is
still needed. Check ULB stock before ordering; the spreadsheet starts with
zero stock as an editable placeholder.

The list covers the IR LED, two BPW34 photodiodes, MCP6002, ADS1115, passives,
2 m USB-C data cable, one M12 gland, and a metric O-ring
assortment. The cable's manufacturer specifies a 5.5 ± 0.15 mm outer
diameter, within the M12 gland's 3–6.5 mm clamping range. Check the final lid
groove: the selected O-ring kit reaches only 28 mm inner diameter.

The workbook still lists a USB-C Nano. That board has been superseded by the
**XIAO SAMD21 already in hand**; do not order the Nano for this firmware.

The cuvettes are on hand. The active design is now a stack of two
boards: a user-routed base and an aligned, unrouted upper XIAO + upright ADS1115 PCB. See the
[hardware assembly notes](../hardware/README.md). The enclosure-derived outline
is preserved on the base; only its cable relief is filled on the upper PCB.
The old PCB cannot replace these two carrier boards. No battery, display, separate turbidimeter, or additional gland sizes
are needed.

The updated enclosure files are [cuerpo.stl](../3D/cuerpo.stl) and
[tapa.stl](../3D/tapa.stl). Check the actual lid groove and gland seat on
these parts before selecting seals; the updated meshes do not validate the
workbook's O-ring assortment or cable diameter.

## Changes not yet reflected in the workbook — upper revision 0.10

Do not use the spreadsheet subtotal as the cost of the new stack. Confirm stock
and select the connector mating heights before ordering these replacements or
additional items. Prices and supplier SKUs for this revision are not assigned.

| Quantity per instrument | Item | Note |
| --- | --- | --- |
| 2 | 1206 1 MΩ resistors, 1% | R1/R2; feedback values may change after bench tests |
| 2 | 1206 10 kΩ resistors, 1% | R3/R4; replaces the old 1 MΩ divider |
| 2 | 1206 220 Ω resistors | R5/R6; series LED current limiting |
| 2 | 1206 10 pF capacitors, C0G/NP0 | C1/C2, provisional feedback compensation |
| 3 | 1206 100 nF capacitors, X7R | C3/C4/C5; ≥10 V |
| 1 | 10 µF, 16 V radial capacitor | C6: 5 mm diameter, 2 mm lead pitch; check real height |
| 1 pair | 1×6, **2.00 mm** SMD female socket + long-reach THT male header | J1; male THT on base B (UP), female SMD on upper F (BELOW); solder from F |
| 1 pair | 2×2, **1.27 mm** SMD female socket + long-reach THT male header | J4 USB link; male THT on base TOP, female SMD beneath upper PCB; match actual footprint and mating reach |
| 1 | 1×10, 2.54 mm right-angle male header | Upright ADS1115 mounting; verify delivered module pin geometry and orientation |
| 2 | 1×7, 2.54 mm low-profile XIAO header strips | 15.24 mm row spacing; select actual module mounting height |
| 2 | Carrier PCBs | Upper routing/solder side F.Cu; retain base routing as supplied; fabricate after checks; cable relief on base only |
| 1 | Compact legacy-compatible USB2 male pigtail, 4 wires | Solder to THT J5 and plug into XIAO; verify local Rp and available plug-tail clearance |
| As needed | Enclosure guides/spacers and module support | Approximately 14 mm PCB top-to-top spacing; support upright ADS1115 ≥2 mm above upper PCB and trim THT tails beneath the base |

The footprint dimensions specify solder geometry, not guaranteed connector
height. Aim for the preferred 35 mm total if a verified assembly allows it;
the present upright-module budget is approximately 36–38 mm. Do not buy
ordinary short headers without checking that they reach their mating sockets.
Do not duplicate components already in stock; through-hole passives in stock
will need replacement if the 1206 bottom-side layout is retained.

The USB-C pigtail is **not yet in the shopping list**. J2/J5 each have four
1 mm wire holes in a 2×2 group at 2 mm pitch; they are not purchased headers.
Identify VBUS, GND, D− and D+ by continuity. There is no carrier CC conductor:
verify a phone-facing OTG/Rd plug or adapter, and a legacy-compatible
micro-facing USB-C pigtail. The actual cable termination is still unconfirmed.
A cut ordinary charging cable must not be assumed to make the phone a host.
Support both cables mechanically and verify both phone plug orientations.
The revised LEFT USB exit needs a compact bare/right-angle plug; only about
5.5 mm rectangular tail space is provisionally reserved inside the cylinder.

Cable diameter source, Goobay 66508 manufacturer datasheet:
https://www.wentronic.com/media/perfion/perfion-product/66508_Produkt-Datenblatt%23Product-Data-Sheet.pdf
