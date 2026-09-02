# Bill of materials (BOM)

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
