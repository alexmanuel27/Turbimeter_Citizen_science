# Bill of materials (BOM)

> ⚠️ **The table below reflects the legacy SEN0189-based schematic.** The
> sensor is being redesigned around discrete optics — see the
> [new design components](#new-design-components-discrete-optics) section
> further down, and [docs/optical-design.md](optical-design.md) for the
> full writeup, until the schematic itself is updated to match.

Components pulled directly from the real schematic (`hardware/esque.kicad_sch`,
exported with `kicad-cli sch export bom`), with a photo and a reference
purchase link for each one. The links are examples of well-known stores
(official or reputable) — price and stock may vary, and any equivalent
store works just as well.

| Photo | Component | Schematic refs | Qty | Notes | Where to buy |
|------|------------|:---:|:---:|-------|----------------|
| <img src="https://store.arduino.cc/cdn/shop/files/A000005_03.front_934x700.jpg?v=1777476619" width="90"> | **Arduino Nano** | A1 | 1 | Main microcontroller (ATmega328) | [Arduino Store](https://store.arduino.cc/products/arduino-nano) |
| <img src="https://dfimg.dfrobot.com/enshop/image/data/SEN0189/SEN0189-sp-01_0x0.png.webp" width="90"> | **DFRobot SEN0189 turbidity sensor** | J2 (connector) | 1 | Analog turbidity sensor, ~0–4.5V output, red wire=5V / black=GND / blue=signal→A1 | [DFRobot (official)](https://www.dfrobot.com/product-1394.html) |
| <img src="https://www.sparkfun.com/media/catalog/product/cache/f3020b7489dcfc4d1d147cf4dad07b7f/1/2/12062-01.jpg" width="90"> | **5mm LED** (red, yellow, green) | D1, D2, D3 | 3 | Turbidity level indicators (low/medium/high) | [SparkFun - assorted pack](https://www.sparkfun.com/led-assorted-20-pack.html) |
| <img src="https://www.sparkfun.com/media/catalog/product/1/4/14490-03.jpg" width="90"> | **330 Ω resistor, 1/4W** | R1, R2, R3, R5, R6 | 5 | Current limiters for the LEDs. *The schematic doesn't fix an explicit value* — 220–330 Ω is typical for LEDs at 5V; adjust if a different value is used | [SparkFun - pack of 20](https://www.sparkfun.com/resistor-330-ohm-1-4-watt-pth-20-pack-thick-leads.html) |
| <img src="https://www.sparkfun.com/media/catalog/product/cache/f3020b7489dcfc4d1d147cf4dad07b7f/0/0/00116-02-L.jpg" width="90"> | **Male pin header, 2.54mm pitch** | J2, J3 | 1 strip (cut to 3 and 4 pins) | 3-pin connector for the sensor and 4-pin connector for the extra header (see schematic) | [SparkFun - 40-pin strip](https://www.sparkfun.com/break-away-headers-straight.html) |
| <img src="https://www.sparkfun.com/media/catalog/product/cache/f3020b7489dcfc4d1d147cf4dad07b7f/J/u/JumperWire-Female-01-L.jpg" width="90"> | **Female-to-female jumper wires** | — | ~4 | To connect the sensor's cable to the PCB header | [SparkFun - pack of 10](https://www.sparkfun.com/jumper-wires-premium-6-f-f-pack-of-10.html) |

## New design components (discrete optics)

Replacing the SEN0189 line above. Sourced from **amazon.it** (prices in
EUR as seen when checked — Amazon prices vary by region/time, treat these
as reference, not quotes). Doesn't include the Arduino Nano or resistors,
which the project already has on hand. Full explanation of the circuit
these go into: [docs/optical-design.md](optical-design.md).

| Photo | Component | Needed/unit | Comes in packs of | Buy |
|------|------------|:---:|:---:|:---:|
| <img src="https://m.media-amazon.com/images/I/71QNRaEE6jS._AC_UL320_.jpg" width="80"> | IR LED, 850nm, 5mm | 1 | 100 — €14.99 | [Amazon.it](https://www.amazon.it/dp/B01BVGIZIU) |
| <img src="https://m.media-amazon.com/images/I/51-pCxQf9DL._AC_UL320_.jpg" width="80"> | BPW34 silicon PIN photodiode | 2 | 5 — €8.99 | [Amazon.it](https://www.amazon.it/dp/B07HBQNMYW) |
| <img src="https://m.media-amazon.com/images/I/71hlMEnRORL._AC_UL320_.jpg" width="80"> | TL072 dual JFET op-amp (DIP-8) | 1 | 12 — €14.99 | [Amazon.it](https://www.amazon.it/dp/B0CD76F382) |
| <img src="https://m.media-amazon.com/images/I/71UhzEvjZCL._AC_UL320_.jpg" width="80"> | ADS1115 16-bit I2C ADC module | 1 | 3 — €12.89 | [Amazon.it](https://www.amazon.it/dp/B0G7CGFY8G) |
| <img src="https://m.media-amazon.com/images/I/71k+M8nGnDL._AC_UL320_.jpg" width="80"> | 5mm status LED, assorted (red/yellow/green used) | 3 | 600 (5 colors) — €11.99 | [Amazon.it](https://www.amazon.it/dp/B08FJ6VC8M) |
| <img src="https://m.media-amazon.com/images/I/71vSu1fW9+L._AC_UL320_.jpg" width="80"> | 2.54mm pin header (male/female mix) | ~7 pins | 50 pieces — €10.99 | [Amazon.it](https://www.amazon.it/dp/B0BZH89PSS) |
| <img src="https://m.media-amazon.com/images/I/81yjq1pkiGL._AC_UL320_.jpg" width="80"> | Dupont jumper wires (M-M/M-F/F-F mix) | ~5 (F-F) | 120 (40 of each type) — €12.99 | [Amazon.it](https://www.amazon.it/dp/B01N40EK6M) |

### Shopping list for 8 units

How many packs to buy, and the actual total, for a batch of **8**
turbidimeters (excluding Nano + resistors, already on hand):

| Component | Needed (×8) | Pack size | Packs to buy | Price/pack | Subtotal |
|---|:---:|:---:|:---:|---:|---:|
| IR LED 850nm | 8 | 100 | 1 | €14.99 | €14.99 |
| BPW34 photodiode | 16 | 5 | 4 | €8.99 | €35.96 |
| TL072 op-amp | 8 | 12 | 1 | €14.99 | €14.99 |
| ADS1115 module | 8 | 3 | 3 | €12.89 | €38.67 |
| Status LED (R/Y/G) | 24 | 600 | 1 | €11.99 | €11.99 |
| Pin header 2.54mm | ~56 pins | 50 pcs | 1 | €10.99 | €10.99 |
| Jumper wires F-F | ~40 | 120 (40 F-F) | 1 | €12.99 | €12.99 |
| **Total** | | | | | **€140.58** |

**~€17.60 per unit** in new components for a batch of 8. Most line items
come with meaningful leftover stock (e.g. 100 IR LEDs and 600 status LEDs
for 8 units), which covers mistakes, breakage, and future batches.

## Reference diagrams

**Arduino Nano pinout** (official, Arduino):

<img src="https://content.arduino.cc/assets/Pinout-NANO_latest.png" width="500">

**DFRobot SEN0189 sensor wiring** (official, DFRobot Wiki):

<img src="https://raw.githubusercontent.com/DFRobot/DFRobotMediaWikiImage/master/Image/SEN0189_Probe_Connection.jpg" width="500">

**Resistor color code** (SparkFun Learn):

<img src="https://cdn.sparkfun.com/assets/learn_tutorials/6/4/Resistors.png" width="450">

**LED + current-limiting resistor** (SparkFun Learn):

<img src="https://cdn.sparkfun.com/assets/6/e/8/3/c/51f93d85757b7f2049270817.png" width="350">

## Datasheets

| Component | File |
|------------|---------|
| Arduino Nano (A000005) | [docs/datasheets/arduino-nano.pdf](datasheets/arduino-nano.pdf) |
| DFRobot SEN0189 turbidity sensor | [docs/datasheets/dfrobot-sen0189.pdf](datasheets/dfrobot-sen0189.pdf) |

## PCB manufacturing

The manufacturing files are already in the repo, ready to upload to any PCB
fab house (JLCPCB, PCBWay, OSH Park, etc.):

- Gerbers: [`hardware/gerber/`](../hardware/gerber/)
- Full design (KiCad): [`hardware/esque.kicad_pcb`](../hardware/esque.kicad_pcb)
- 3D model: [`hardware/esque.step`](../hardware/esque.step)

## Notes

- This table was built from the schematic's real references, it isn't a
  made-up list. The only unconfirmed thing is the resistors' exact value
  (the schematic leaves them as a generic `R` component, with no value
  assigned) — it's worth fixing this in KiCad before ordering the PCB in
  bulk.
- Still to add (once defined): enclosure/case, sensor sample chamber, and
  final power source (battery/USB).
