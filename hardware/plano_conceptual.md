# Plano conceptual del turbidímetro (EJEMPLO FICTICIO)

> ⚠️ **Diagrama y descripción ficticios**, solo para ilustrar el formato.
> Reemplazar por los planos y esquemáticos reales (ej. exportados de KiCad/Fritzing).

## Diagrama de bloques

Ver [plano_conceptual.svg](plano_conceptual.svg) para el diagrama visual.

```
[LED Emisor IR] --> [Cámara de muestra con agua] --> [Fotodiodo receptor]
                                                            |
                                                            v
                                                   [ESP32 - ADC + firmware]
                                                            |
                                                            v
                                        [Pantalla local]  y  [Envío de datos: WiFi/App]
```

## Esquema de conexiones (ficticio)

| Componente | Pin ESP32 |
|------------|-----------|
| LED IR (ánodo, vía R 220Ω) | GPIO 25 |
| Fotodiodo (salida divisor) | GPIO 34 (ADC) |
| Pantalla OLED SDA | GPIO 21 |
| Pantalla OLED SCL | GPIO 22 |
| Alimentación | 3.3V / GND |

## Dimensiones de referencia (ficticias)

- Cámara de muestra: tubo PVC de 50 mm de diámetro x 80 mm de largo.
- Caja estanca exterior: 120 mm x 80 mm x 50 mm.
- Separación LED–fotodiodo: 40 mm (perpendicular al eje del tubo).

## Notas de diseño

- La geometría de 90° entre emisor y receptor (nefelometría) es la más común
  para medir turbidez por dispersión de luz, en vez de solo transmitancia.
- Usar material opaco a la luz ambiente para minimizar ruido en la lectura.
- Considerar una versión impresa en 3D de la cámara para estandarizar el diseño
  entre voluntarios.

---
*Documento y diagrama ficticios. Reemplazar por el diseño validado del equipo técnico.*
