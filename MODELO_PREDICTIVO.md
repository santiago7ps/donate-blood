# Predicción de asistencia (versión simple)

**Idea:** la probabilidad de que un donante asista a su cita depende de cuántas de sus citas anteriores cumplió.

    probabilidad = (citas realizadas + 2 × promedio general) / (citas terminadas + 2)

Ejemplo: Ana cumplió 3 de 4 citas y el promedio general es 37,5 % → (3 + 0,75) / (4 + 2) = **63 %**.
Un donante sin historial recibe el promedio general.

## Dónde está
| Qué | Archivo |
|---|---|
| Cálculo de la probabilidad (una sola clase) | `ml/Prediccion.java` |
| Pantalla del banco con las citas de menor a mayor probabilidad | `controller/PrediccionController.java` + `templates/banco_prediccion.html` |
| Columna «Prob. de asistir» en Ver citas | `BancoController` + `templates/banco_citas.html` |
| Datos de prueba con lógica (cada donante tiene su constancia) | `seed/HistorialSintetico.java` |
| Prueba | `src/test/.../ml/PrediccionTest.java` |

Riesgo: **alto** < 50 %, **medio** 50–75 %, **bajo** ≥ 75 %.
Las citas PENDIENTE no cuentan para calcular, solo se predicen.

## Cómo se muestran los resultados
No se muestran números sueltos: cada cita trae una explicación en palabras. La pantalla solo lista citas pendientes de hoy en adelante y tiene un buscador por nombre o usuario del donante.

| Resultado | Qué significa |
|---|---|
| Muy probable (≥ 75 %) | Es muy probable que asista |
| Probable (50–75 %) | Es probable que asista, pero no es seguro |
| Poco probable (< 50 %) | Es poco probable que asista |

Arriba de la tabla hay un resumen: cuántas citas pendientes hay y cuántas se espera que asistan.

## Recordatorios para el donante
En **Mi panel** y **Mis citas** el donante ve sus recordatorios, armados solo con sus datos guardados
(`ml/Recordatorio.java`):
- Cada cita pendiente: «Tu cita de donación es MAÑANA (2026-11-10) a las 09:00 en Cruz Roja 3.»
- Si ya canceló citas antes: «Has cumplido 3 de 4 citas anteriores. Si no puedes asistir, cancela o cambia tu cita…»
- Sin citas próximas: invitación a agendar.

## Landing page
La página pública muestra cifras reales de asistencia (`ml/Patrones.java`), siempre agregadas y sin nombres:
citas agendadas, donaciones realizadas, % de citas que se cumplen, y la franja (mañana/tarde) y el día de la
semana con mayor asistencia, más una gráfica de barras de asistencia por día de la semana (hecha con CSS, sin librerías). La sección solo aparece si hay al menos 30 citas terminadas, y la franja o el día
solo si cada grupo tiene al menos 10 citas, para no mostrar conclusiones con muy pocos datos.
