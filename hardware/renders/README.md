# Native KiCad assembly renders

Design: **Ing. Alex M. Rivera**. Upper revision 0.10, 13 October 2026.

- `base-top.png` and `upper-top.png`: individual boards, through-hole parts above.
- `base-bottom.png` and `upper-bottom.png`: individual boards, SMD and solder side below.
- `assembly.png`: both boards connected, provisional 14 mm top-to-top spacing.
- `assembly-front.png`: side view showing the mating connectors and both floors.

Models are visual assembly aids. Verify actual components, pin order, header
reach, trimmed tails, cable bends and enclosure fit before fabrication.
No upper tracks were added. Route the upper board on F.Cu; base routing remains as supplied.

Current views use physical UP = native B and solder side = native F. The base
routing is shown as supplied; only the upper placement was adjusted. The two
wire-hole connectors J2/J5 have different pin orders; follow the assembly notes.
