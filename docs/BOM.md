# Prototype purchase list

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

The existing PCB and cuvettes are on hand. No battery, display, status LEDs,
separate turbidimeter, or additional gland sizes are included. The current PCB
layout is from the earlier SEN0189 design and still requires revision.

Cable diameter source, Goobay 66508 manufacturer datasheet:
https://www.wentronic.com/media/perfion/perfion-product/66508_Produkt-Datenblatt%23Product-Data-Sheet.pdf
