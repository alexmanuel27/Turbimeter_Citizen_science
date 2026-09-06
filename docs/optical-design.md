# Optical redesign: discrete 90° nephelometric sensor

This replaces the DFRobot SEN0189 module (straight-through, 0°, 10-bit ADC,
no LED-drift compensation) with a custom optical bench built from discrete
parts, following the same principle real turbidimeters use (ISO 7027
nephelometry). The old SEN0189-based firmware is kept for reference in
[`firmware/legacy_sen0189/`](../firmware/legacy_sen0189/).

## The principle

<img src="diagrams/optical-geometry.svg" width="700">

- An **IR LED** shines through the water sample.
- A **photodiode placed at 90°** from the LED's beam picks up light
  *scattered* by suspended particles — not light that passed straight
  through. This is what "NTU" (Nephelometric Turbidity Unit) is defined
  against, and it doesn't saturate/invert at high turbidity the way
  straight-through transmittance does.
- A second **reference photodiode**, near the LED and shielded from the
  sample, tracks the LED's own brightness. Dividing the 90° signal by the
  reference signal cancels drift from LED aging/temperature — something
  none of the DFRobot sensors we evaluated do.
- Both signals are read through an **ADS1115** (external 16-bit ADC)
  instead of the Nano's built-in 10-bit ADC, for much better resolution and
  noise performance.

## Circuit

<img src="diagrams/circuit-schematic.svg" width="800">

Full wiring table:

| From | To |
|------|-----|
| IR LED anode | Nano D7, through a 220Ω resistor |
| IR LED cathode | GND |
| Photodiode A (90°) cathode | +5V |
| Photodiode A anode | TL072 channel A inverting input |
| TL072 channel A output | ADS1115 AIN0 |
| Photodiode B (reference) cathode | +5V |
| Photodiode B anode | TL072 channel B inverting input |
| TL072 channel B output | ADS1115 AIN1 |
| Feedback resistor (1MΩ, per channel) | Between each op-amp's inverting input and its output |
| ADS1115 VDD | 5V |
| ADS1115 GND, ADDR | GND (ADDR→GND sets I2C address 0x48) |
| ADS1115 SDA / SCL | Nano A4 / A5 |

The 1MΩ feedback resistors are a starting point, not a final value — tune
them on the breadboard so each op-amp's output lands roughly between 0.5V
and 3.5V at your expected light levels (photodiode B, right next to the
LED, usually needs a *smaller* resistor than photodiode A, since it
receives far more light).

**Recommendation: prototype this on a breadboard before touching the PCB.**
The existing `hardware/esque.kicad_pcb` was laid out for the SEN0189 +
status LEDs, not for this optical bench — it needs a new PCB revision
(new footprints, the 90° mechanical layout, the sample chamber cutout).
That's a separate next step once the breadboard version is validated and
the resistor values are tuned for your actual LED/photodiode batch.

## New components

Sourced from **amazon.it** (prices in EUR, as seen when this was checked —
Amazon prices vary by region/time, treat these as reference, not quotes).
Doesn't include the Arduino Nano or resistors, which the project already
has on hand.

| Photo | Component | Needed/unit | Comes in packs of | Buy |
|------|------------|:---:|:---:|:---:|
| <img src="https://m.media-amazon.com/images/I/71QNRaEE6jS._AC_UL320_.jpg" width="80"> | IR LED, 850nm, 5mm | 1 | 100 — €14.99 | [Amazon.it](https://www.amazon.it/dp/B01BVGIZIU) |
| <img src="https://m.media-amazon.com/images/I/51-pCxQf9DL._AC_UL320_.jpg" width="80"> | BPW34 silicon PIN photodiode | 2 | 5 — €8.99 | [Amazon.it](https://www.amazon.it/dp/B07HBQNMYW) |
| <img src="https://m.media-amazon.com/images/I/71hlMEnRORL._AC_UL320_.jpg" width="80"> | TL072 dual JFET op-amp (DIP-8) | 1 | 12 — €14.99 | [Amazon.it](https://www.amazon.it/dp/B0CD76F382) |
| <img src="https://m.media-amazon.com/images/I/71UhzEvjZCL._AC_UL320_.jpg" width="80"> | ADS1115 16-bit I2C ADC module | 1 | 3 — €12.89 | [Amazon.it](https://www.amazon.it/dp/B0G7CGFY8G) |
| <img src="https://m.media-amazon.com/images/I/71k+M8nGnDL._AC_UL320_.jpg" width="80"> | 5mm status LED, assorted (red/yellow/green used) | 3 | 600 (5 colors) — €11.99 | [Amazon.it](https://www.amazon.it/dp/B08FJ6VC8M) |
| <img src="https://m.media-amazon.com/images/I/71vSu1fW9+L._AC_UL320_.jpg" width="80"> | 2.54mm pin header (male/female mix) | ~7 pins | 50 pieces — €10.99 | [Amazon.it](https://www.amazon.it/dp/B0BZH89PSS) |
| <img src="https://m.media-amazon.com/images/I/81yjq1pkiGL._AC_UL320_.jpg" width="80"> | Dupont jumper wires (M-M/M-F/F-F mix) | ~5 (F-F) | 120 (40 of each type) — €12.99 | [Amazon.it](https://www.amazon.it/dp/B01N40EK6M) |

Still needed but already covered by the existing [BOM](BOM.md) / on hand
per this order: Arduino Nano, and resistors (220-330Ω for the LED, 1MΩ×2
for the TIA feedback — a generic assorted resistor kit covers both). The
DFRobot SEN0189 line item is no longer needed at all.

### Shopping list for 8 units

How many packs to buy, and the actual total, assuming a batch of **8**
turbidimeters (excluding Nano + resistors, per the above):

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

That's **~€17.60 per unit** in new components for a batch of 8 (plus
whatever the 8 Arduino Nanos and resistors already on hand cost you). Most
line items come with meaningful leftover stock (e.g. 100 IR LEDs and 600
status LEDs for 8 units), which covers mistakes, breakage, and future
batches — buying exactly 8 of everything individually would actually cost
more per unit, not less.

## What the firmware reports now

[`firmware/firmware.ino`](../firmware/firmware.ino) no longer computes a
fabricated "NTU" from an unjustified formula. Instead it reports an honest
**turbidity index** = (90° scattered signal) / (reference signal), both
corrected for ambient light by reading each channel with the LED off and
on and subtracting. This index is:

- **Physically meaningful** (it's a real ratio of measured light, not a
  guessed polynomial).
- **Not yet an absolute NTU value** — same caveat as before, it needs
  calibration against a real reference to become traceable NTU.
- Used with the same 3-point calibration wizard as before (`C`, `F`, `X`
  over Serial) to set relative low/medium/high thresholds for the status
  LEDs.

## Getting to absolute NTU

Once this optical design is validated and you want real, traceable NTU
numbers (see the earlier discussion in this project about what NTU means):

1. Get **one** reference point source — either a real turbidimeter reading
   (borrow/rent one, or ask a local water utility/university lab to run a
   few of your water samples) or formazin standard solutions.
2. Take several real-world samples spanning your range of interest (e.g.
   clear tap water, slightly cloudy, very turbid) and record both the
   reference NTU and this sensor's `index` for each.
3. Fit a line (or simple curve) `NTU ≈ m × index + b` from those points —
   the ratiometric signal from a proper 90° design is usually much more
   linear than the SEN0189's raw voltage ever was, so a linear fit should
   go a long way.
4. Bake that fit into the firmware (replacing the raw `index` output) once
   you've built enough units with the *same* LED/photodiode batch that the
   fit reasonably transfers between them.

This is real calibration work (see the earlier price/time comparison for
formazin vs. a reference instrument) — worth doing once the optical design
itself is confirmed to behave well, not before.
