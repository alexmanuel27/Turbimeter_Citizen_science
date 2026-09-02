# Lista de materiales (BOM)

Componentes extraídos directamente del esquemático real (`hardware/esque.kicad_sch`,
exportado con `kicad-cli sch export bom`), con foto y un enlace de compra de
referencia para cada uno. Los enlaces son ejemplos de tiendas conocidas
(oficiales o de buena reputación) — el precio y el stock pueden variar, y
cualquier tienda equivalente sirve igual.

| Foto | Componente | Refs. esquema | Cant. | Notas | Dónde comprar |
|------|------------|:---:|:---:|-------|----------------|
| <img src="https://store.arduino.cc/cdn/shop/files/A000005_03.front_934x700.jpg?v=1777476619" width="90"> | **Arduino Nano** | A1 | 1 | Microcontrolador principal (ATmega328) | [Arduino Store](https://store.arduino.cc/products/arduino-nano) |
| <img src="https://dfimg.dfrobot.com/enshop/image/data/SEN0189/SEN0189-sp-01_0x0.png.webp" width="90"> | **Sensor de turbidez DFRobot SEN0189** | J2 (conector) | 1 | Sensor analógico de turbidez, salida 0–4.5V aprox., cable rojo=5V / negro=GND / azul=señal→A1 | [DFRobot (oficial)](https://www.dfrobot.com/product-1394.html) |
| <img src="https://www.sparkfun.com/media/catalog/product/cache/f3020b7489dcfc4d1d147cf4dad07b7f/1/2/12062-01.jpg" width="90"> | **LED 5mm** (rojo, amarillo, verde) | D1, D2, D3 | 3 | Indicadores de nivel de turbidez (bajo/medio/alto) | [SparkFun - pack surtido](https://www.sparkfun.com/led-assorted-20-pack.html) |
| <img src="https://www.sparkfun.com/media/catalog/product/1/4/14490-03.jpg" width="90"> | **Resistor 330 Ω, 1/4W** | R1, R2, R3, R5, R6 | 5 | Limitadoras de corriente para los LED. *El esquema no fija un valor explícito* — 220–330 Ω es lo típico para LED a 5V; ajustar si se usa otro valor | [SparkFun - pack x20](https://www.sparkfun.com/resistor-330-ohm-1-4-watt-pth-20-pack-thick-leads.html) |
| <img src="https://www.sparkfun.com/media/catalog/product/cache/f3020b7489dcfc4d1d147cf4dad07b7f/0/0/00116-02-L.jpg" width="90"> | **Header de pines macho, paso 2.54mm** | J2, J3 | 1 tira (cortar a 3 y 4 pines) | Conector de 3 pines para el sensor y de 4 pines para el header adicional (ver esquemático) | [SparkFun - tira de 40 pines](https://www.sparkfun.com/break-away-headers-straight.html) |
| <img src="https://www.sparkfun.com/media/catalog/product/cache/f3020b7489dcfc4d1d147cf4dad07b7f/J/u/JumperWire-Female-01-L.jpg" width="90"> | **Cables jumper hembra-hembra** | — | ~4 | Para conectar el cable del sensor al header de la PCB | [SparkFun - pack x10](https://www.sparkfun.com/jumper-wires-premium-6-f-f-pack-of-10.html) |

## Diagramas de referencia

**Pinout Arduino Nano** (oficial, Arduino):

<img src="https://content.arduino.cc/assets/Pinout-NANO_latest.png" width="500">

**Conexión del sensor DFRobot SEN0189** (oficial, DFRobot Wiki):

<img src="https://raw.githubusercontent.com/DFRobot/DFRobotMediaWikiImage/master/Image/SEN0189_Probe_Connection.jpg" width="500">

**Código de colores de resistores** (SparkFun Learn):

<img src="https://cdn.sparkfun.com/assets/learn_tutorials/6/4/Resistors.png" width="450">

**LED + resistor limitador de corriente** (SparkFun Learn):

<img src="https://cdn.sparkfun.com/assets/6/e/8/3/c/51f93d85757b7f2049270817.png" width="350">

## Datasheets

| Componente | Archivo |
|------------|---------|
| Arduino Nano (A000005) | [docs/datasheets/arduino-nano.pdf](datasheets/arduino-nano.pdf) |
| Sensor turbidez DFRobot SEN0189 | [docs/datasheets/dfrobot-sen0189.pdf](datasheets/dfrobot-sen0189.pdf) |

## Fabricación de la PCB

Los archivos de fabricación ya están en el repo, listos para subir a
cualquier fabricante de PCBs (JLCPCB, PCBWay, OSH Park, etc.):

- Gerbers: [`hardware/gerber/`](../hardware/gerber/)
- Diseño completo (KiCad): [`hardware/esque.kicad_pcb`](../hardware/esque.kicad_pcb)
- Modelo 3D: [`hardware/esque.step`](../hardware/esque.step)

## Notas

- Esta tabla se armó a partir de las referencias reales del esquemático, no
  es una lista inventada. Lo único sin confirmar es el valor exacto de los
  resistores (el esquema los deja como componente genérico `R`, sin valor
  asignado) — conviene fijarlo en KiCad antes de pedir la PCB en serie.
- Faltan agregar (cuando se definan): gabinete/carcasa, cámara de muestra
  para el sensor, y fuente de alimentación (batería/USB) definitiva.
