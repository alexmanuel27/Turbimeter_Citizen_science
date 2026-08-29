# Resultados de ejemplo - Ciencia ciudadana (DATOS FICTICIOS)

> ⚠️ **Todos los datos, nombres y ubicaciones de este documento son inventados.**
> Es solo un ejemplo de cómo se vería el reporte de resultados una vez que la
> comunidad empiece a cargar mediciones reales. Ver el dataset completo en
> [`data/ejemplo_resultados.csv`](../data/ejemplo_resultados.csv).

## Resumen ficticio de campaña

- Período simulado: 01/06/2026 – 15/06/2026
- Voluntarios participantes (ejemplo): 6
- Puntos de monitoreo (ejemplo): 4
- Mediciones cargadas (ejemplo): 12

## Tabla de mediciones (ejemplo)

| Fecha | Punto de muestreo | Voluntario | Turbidez (NTU) | Lluvia reciente | Observaciones |
|-------|--------------------|------------|-----------------|------------------|----------------|
| 2026-06-01 | Arroyo El Sauce | Voluntario A | 8.2 | No | Agua clara |
| 2026-06-01 | Río Norte - Puente | Voluntario B | 42.5 | Sí | Turbio tras lluvia |
| 2026-06-03 | Canal Sur | Voluntario C | 15.1 | No | — |
| 2026-06-03 | Arroyo El Sauce | Voluntario A | 7.9 | No | Similar a medición previa |
| 2026-06-05 | Laguna Este | Voluntario D | 30.4 | No | Posible actividad de fauna |
| 2026-06-05 | Río Norte - Puente | Voluntario B | 25.0 | No | Bajó tras cesar la lluvia |
| 2026-06-08 | Canal Sur | Voluntario C | 60.8 | Sí | Muy turbio, posible obra cercana |
| 2026-06-08 | Arroyo El Sauce | Voluntario E | 9.0 | No | — |
| 2026-06-10 | Laguna Este | Voluntario D | 28.6 | No | — |
| 2026-06-12 | Río Norte - Puente | Voluntario F | 18.3 | No | — |
| 2026-06-14 | Canal Sur | Voluntario C | 22.4 | No | Recuperación tras lluvia del 08/06 |
| 2026-06-15 | Arroyo El Sauce | Voluntario A | 8.5 | No | Consistente con línea base |

## Interpretación de ejemplo

- **Arroyo El Sauce** muestra valores estables y bajos (~8 NTU), lo que sugiere
  una línea de base de agua clara en este punto (dato ficticio).
- **Canal Sur** presenta el pico más alto (60.8 NTU) coincidiendo con lluvia y
  posible obra cercana, con recuperación gradual en mediciones posteriores.
- **Río Norte - Puente** muestra sensibilidad a eventos de lluvia, con picos
  que bajan en pocos días.

*(Estas interpretaciones son solo ilustrativas del tipo de análisis que se podría
hacer, no reflejan datos reales.)*

## Próximos pasos (placeholder)

1. Reemplazar esta tabla con datos reales cargados por la comunidad.
2. Definir el formulario/app de carga de datos definitivo.
3. Automatizar un reporte o dashboard a partir de `data/*.csv`.

---
*Documento de ejemplo. Todos los valores son ficticios.*
