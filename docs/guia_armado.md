# Guía de armado y uso del turbidímetro (EJEMPLO FICTICIO)

> ⚠️ **Este documento es un borrador de ejemplo con contenido ficticio.**
> Sirve como plantilla para tener una idea de formato y contenido.
> Debe reemplazarse por el diseño, materiales y procedimiento reales del proyecto.

## 1. Objetivo

Construir un turbidímetro de bajo costo que los voluntarios de la comunidad puedan
armar en casa y usar para medir la turbidez del agua de ríos, arroyos o canales
cercanos, y compartir los datos con el proyecto.

## 2. Materiales (lista ficticia de ejemplo)

| # | Material | Cantidad aprox. |
|---|----------|------------------|
| 1 | Microcontrolador ESP32 | 1 |
| 2 | LED infrarrojo 940nm | 1 |
| 3 | Fotodiodo / LDR | 1 |
| 4 | Tubo de PVC opaco (cámara de muestra) 5 cm diám. | 1 |
| 5 | Resistencias 220Ω y 10kΩ | 1 c/u |
| 6 | Protoboard o placa perforada | 1 |
| 7 | Cables dupont | ~10 |
| 8 | Frasco de vidrio transparente (muestra de agua) | 1 |
| 9 | Batería 9V o power bank USB | 1 |
| 10 | Caja estanca para protección | 1 |

Costo estimado (ficticio): **USD 15–20** por unidad.

## 3. Pasos de armado

1. **Preparar la cámara de muestra:** cortar el tubo de PVC a 8 cm y hacer dos
   perforaciones enfrentadas a 4 cm de altura para alojar el LED y el fotodiodo.
2. **Montar el emisor:** insertar el LED infrarrojo en una perforación, fijarlo
   con silicona y conectarlo a la resistencia de 220Ω hacia GPIO 25 del ESP32.
3. **Montar el receptor:** insertar el fotodiodo/LDR en la perforación opuesta,
   conectarlo en divisor de tensión con la resistencia de 10kΩ hacia GPIO 34
   (entrada analógica).
4. **Sellar la cámara:** aplicar silicona alrededor de ambos componentes para
   evitar el ingreso de luz externa y humedad.
5. **Cablear el ESP32:** alimentar con 3.3V/GND desde el power bank, verificar
   continuidad con multímetro antes de energizar.
6. **Cargar el firmware:** flashear el sketch `firmware/turbidimetro.ino`
   (pendiente de escribir) que lee el valor analógico y lo convierte a NTU.
7. **Calibrar:**
   - Medir con agua destilada (0 NTU de referencia).
   - Medir con 2–3 soluciones patrón de turbidez conocida (ej. 10, 50, 100 NTU).
   - Ajustar la curva de calibración en el firmware.
8. **Probar en campo:** sumergir la cámara en la muestra de agua a analizar,
   esperar 10 segundos de estabilización y registrar la lectura.

## 4. Procedimiento para voluntarios (ciencia ciudadana)

1. Llenar el frasco con la muestra de agua del punto de monitoreo.
2. Verter la muestra en la cámara del turbidímetro.
3. Esperar a que el valor en pantalla/app se estabilice.
4. Registrar: fecha, hora, ubicación (o coordenadas GPS), valor NTU, condiciones
   climáticas (lluvia reciente, sí/no).
5. Cargar el dato en el formulario/app del proyecto o enviarlo por la vía
   acordada con el equipo coordinador.

## 5. Seguridad y buenas prácticas

- No mirar directamente el LED (aunque sea infrarrojo, evitar exposición directa).
- Lavar el frasco y la cámara entre muestras para evitar contaminación cruzada.
- Evitar tomar muestras en zonas de riesgo (corrientes fuertes, accesos inseguros).

---
*Fin del documento de ejemplo. Reemplazar por la guía real del hardware definitivo.*
