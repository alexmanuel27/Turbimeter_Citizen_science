# Explicación del firmware

Análisis de [`firmware/firmware.ino`](../firmware/firmware.ino): sensor de
turbidez DFRobot SEN0189 + Arduino Nano.

## Conexión del hardware

| Cable del sensor | Destino |
|-------------------|---------|
| Rojo (VCC) | 5V |
| Negro (GND) | GND |
| Azul (señal analógica) | Pin A1 |

<img src="https://raw.githubusercontent.com/DFRobot/DFRobotMediaWikiImage/master/Image/SEN0189_Probe_Connection.jpg" width="500">

*Diagrama de conexión del sensor. Fuente: DFRobot Wiki (oficial).*

Además, 3 LEDs indicadores (verde/amarillo/rojo) en pines digitales
configurables (por defecto 10, 9 y 8) para mostrar el nivel de turbidez de
forma visual, sin necesidad de mirar el Monitor Serial.

## Lógica general (`loop`)

1. **Lectura del sensor:** en vez de tomar una sola lectura analógica (que
   tendría ruido), promedia **800 muestras** consecutivas de `analogRead(A1)`
   con una pequeña pausa entre cada una (`delayMicroseconds(500)`). Esto
   estabiliza el valor antes de usarlo.
2. **Conversión a voltaje:** el promedio (0–1023, resolución del ADC de 10
   bits) se escala a un voltaje real usando la referencia de 5V del Arduino:
   `voltaje = promedio * (5.0 / 1023.0)`.
3. **Conversión a NTU:** se aplica una fórmula polinómica de segundo grado
   (`ntu = A*v² + B*v + C`) con coeficientes tomados de la comunidad de
   Arduino (no son un valor oficial de DFRobot — el fabricante solo publica
   una gráfica de referencia, no una ecuación). Por debajo de 2.5V se asume
   el máximo de turbidez (3000 NTU), ya que la curva del sensor deja de ser
   confiable en ese rango.
4. **Salida:** el valor de voltaje y NTU se imprime por el Monitor Serial, y
   si no se está calibrando, se evalúa a qué "nivel" corresponde para
   encender el LED correspondiente.

## Por qué tiene un asistente de calibración

Como la fórmula NTU es solo una aproximación genérica, el código no confía
en ella para decidir cuándo encender cada LED. En cambio, permite calibrar
los **umbrales** con muestras de agua reales del propio sensor:

1. Se escribe `C` por el Monitor Serial para iniciar.
2. Se pide sumergir el sensor en **agua clara**, y se fija ese punto con `F`.
3. Se pide **agua con turbidez media** (sugiere tinte amarillo), y se fija
   con `F`.
4. Se pide **agua con turbidez alta** (sugiere café o tinta oscura), y se
   fija con `F`.
5. Se finaliza con `X`, lo que:
   - Calcula los umbrales como el punto medio entre cada par de muestras
     (`recalcularUmbrales()`).
   - Guarda los 3 valores NTU de referencia en la **EEPROM** del Arduino
     (`guardarCalibracionEnEEPROM()`), para no perder la calibración al
     desconectar el equipo.

Al encender el Arduino, `cargarCalibracionDesdeEEPROM()` revisa si ya existe
una calibración guardada (usa una "bandera" en la dirección 12 de la
EEPROM) y, si la hay, la carga en lugar de usar los valores por defecto.

## Lógica de los LEDs (`evaluarLeds`)

Con los 3 puntos calibrados (clara / media / turbia) se calculan dos
umbrales intermedios:

- `UMBRAL_TURBIDEZ_MEDIA` = punto medio entre "clara" y "media"
- `UMBRAL_TURBIDEZ_ALTA` = punto medio entre "media" y "turbia"

Y luego:

- NTU > `UMBRAL_TURBIDEZ_ALTA` → **LED rojo** (turbidez alta)
- NTU > `UMBRAL_TURBIDEZ_MEDIA` → **LED amarillo** (turbidez media)
- en cualquier otro caso → **LED verde** (turbidez baja)

## Notas para quien vaya a modificar el código

- Los pines de los LEDs (`PIN_LED_ROJO/AMARILLO/VERDE`) están marcados en el
  código como `<-- CAMBIAR` porque dependen de cómo se cablee cada placa.
- `NUMERO_DE_MUESTRAS = 800` es un compromiso entre estabilidad de la
  lectura y velocidad de actualización (a más muestras, más preciso pero
  más lento el refresco, hoy cada ~0.5s más el tiempo de muestreo).
- Los coeficientes `COEF_A/B/C` solo deberían tocarse si se hace una
  regresión propia con NTU reales (ej. con un turbidímetro de referencia),
  algo pendiente para este proyecto — hoy la fiabilidad viene de los
  umbrales calibrados manualmente, no de la fórmula.
