# Earlier geometry exports — not the current fabrication set

Design: **Ing. Alex M. Rivera**.

Files in this directory predate the two-floor revision 0.9. They do not
manufacture the active sensor/base and combined controller/ADC assembly. Keep them as references
for the existing enclosure geometry. Generate new fabrication files from each
active KiCad project only after routing, DRC and a physical fit check.

The active revision uses single-face copper: export **B.Cu only**, plus the
required mask, solder-side legend, outline and drill files. Omit F.Cu. KiCad
retains two logical layers and conventional PTH pad stacks; that does not
make F.Cu part of the manufacturing set. All joints are soldered from BOTTOM.
