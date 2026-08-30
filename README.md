# Turbimeter - Citizen Science

Repositorio del proyecto de turbidímetro para ciencia ciudadana: diseño, firmware y datos para medir la turbidez del agua con participación comunitaria.

## Estructura

- `firmware/` - código para el microcontrolador del sensor
- `hardware/` - esquemáticos, PCB, diseños 3D (proyecto KiCad)
- `docs/` - documentación, guías de armado y calibración
- `data/` - datasets recolectados
- `analysis/` - notebooks y scripts de análisis de datos
- `img/` - fotos del armado/prototipo

## Hardware

- **Sensor:** DFRobot SEN0189 (turbidez)
- **Controlador:** Arduino Nano
- Proyecto KiCad completo en `hardware/`: esquemático (`esque.kicad_sch`),
  PCB (`esque.kicad_pcb`), modelo 3D (`esque.step`) y archivos de fabricación
  (`hardware/gerber/`).

![PCB girando](img/pcb_rotacion.gif)

*Render del PCB (`hardware/esque.step`) generado con `kicad-cli`.*

## Firmware

[`firmware/firmware.ino`](firmware/firmware.ino) lee el sensor, calcula un
valor aproximado en NTU y enciende un LED (verde/amarillo/rojo) según el
nivel de turbidez. Incluye un asistente de calibración interactiva por
Monitor Serial (ver comentarios al inicio del archivo).

Explicación detallada del funcionamiento del código: [docs/firmware.md](docs/firmware.md).

## Estado

Prototipo en desarrollo: primera versión de firmware y diseño de PCB
cargados. Pendiente: guía de armado y resultados reales de campo
(reemplazando el contenido de ejemplo que hubo en `docs/`).
