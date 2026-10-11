[English](README.en.md) · Español

# Saldaria

Simulador de préstamos e hipotecas para Android. Calcula la cuota mensual, genera el cuadro de amortización completo y compara el efecto de las amortizaciones anticipadas, usando el **sistema de amortización francés** (cuota constante), el más habitual en préstamos e hipotecas en España.

Kotlin · Jetpack Compose · Material 3 · MVVM · minSdk 26

> **Proyecto educativo.** No constituye asesoramiento financiero. La TAE es una aproximación y no incluye comisiones, gastos de formalización ni productos vinculados.

## Funcionalidades

- **Simulador:** cuota mensual, total de intereses, total pagado y TAE aproximada.
- **Cuadro de amortización:** detalle mes a mes agrupado por años expandibles, con cuota, interés, capital amortizado y saldo pendiente.
- **Gráfico de reparto:** evolución de intereses frente a capital, dibujado con `Canvas` sin librerías de gráficos.
- **Comparador de amortización anticipada:** ahorro en intereses y en meses con las dos estrategias habituales, reducir plazo o reducir cuota.
- **Tipo de referencia del BCE:** consulta del último tipo oficial de las operaciones principales de financiación, aplicable como TIN con un toque.
- **Tema claro y oscuro.**

## Resultados de referencia

Préstamo de 100.000 € al 3,00 % a 25 años (300 meses):

| Concepto | Resultado |
|---|---|
| Cuota mensual | 474,21 € |
| Última cuota (ajustada) | 474,70 € |
| Interés del mes 1 | 250,00 € |
| Saldo al final del año 1 | 97.272,18 € |

Con una amortización anticipada de 20.000 € en el mes 12:

| Estrategia | Resultado |
|---|---|
| Reducir plazo | Termina en 222 meses (78 menos). Ahorro de 17.207,90 € en intereses |
| Reducir cuota | Plazo de 300 meses. Cuota de 376,71 € desde el mes 13. Ahorro de 8.080,77 € en intereses |

Estos valores están fijados en los tests y se contrastaron con una implementación de referencia independiente.

## Cómo funciona el cálculo

La cuota constante se obtiene con la fórmula del sistema francés:

```
cuota = P · r · (1 + r)^n / ((1 + r)^n − 1)
```

donde `P` es el capital, `r = TIN / 1200` es el tipo mensual y `n` es el número de meses.

El cuadro se construye mes a mes: el interés se calcula sobre el saldo pendiente, el resto de la cuota amortiza capital y la última cuota se ajusta para dejar el saldo en exactamente 0,00 €. Las amortizaciones anticipadas se aplican tras la cuota del mes indicado; en el modo "reducir cuota" se recalcula la cuota sobre los meses restantes.

## Decisiones técnicas

| Decisión | Motivo |
|---|---|
| `BigDecimal` para todo el dinero, con `MathContext.DECIMAL128` y redondeo `HALF_UP` a 2 decimales | Evita los errores de representación binaria de `Double`. La capa `domain` no usa `Double` ni `Float`. |
| Ajuste de la última cuota | Los redondeos mensuales acumulan una pequeña deriva. La última cuota la absorbe y garantiza saldo final 0,00 €. |
| Dominio como código Kotlin puro, con funciones y data classes inmutables | Se prueba sin emulador ni dependencias de Android, y la lógica financiera queda separada de la interfaz. |
| Validación con `require` en el motor y errores de entrada como enum `InputError` en el ViewModel | El dominio rechaza datos inválidos con `IllegalArgumentException`. La interfaz traduce el error a un texto de `strings.xml` y marca el campo afectado. |
| Estado único (`UiState`) expuesto con `StateFlow` | Flujo de datos unidireccional: las pantallas solo pintan estado y envían eventos al ViewModel mediante lambdas (state hoisting). |
| Resultado explícito en la capa de red (`Result`) | Un fallo de red o de parseo no rompe la app: se muestra un estado de error y el resto de funciones sigue disponible. |
| Gráfico propio con `Canvas` | Evita una dependencia por un único gráfico sencillo. |
| Sin framework de inyección de dependencias | El tamaño del proyecto no lo justifica. |

## Arquitectura

La aplicación sigue **MVVM**. La capa `domain` es un paquete sin dependencias de Android.

```text
com.bryantgiorgini.saldaria
|   MainActivity.kt
+---data
|       EcbApi.kt
|       EcbRepository.kt
+---domain
|       AmortizationCalculator.kt
+---navigation
|       AppNavigation.kt
+---ui
|   |   CompareScreen.kt
|   |   Formatters.kt
|   |   ScheduleScreen.kt
|   |   SimulatorScreen.kt
|   \---theme
|           Color.kt
|           Theme.kt
|           Type.kt
\---viewmodel
        SimulatorViewModel.kt
```

## Áreas técnicas cubiertas

| Área | Dónde verlo |
|---|---|
| Kotlin: null safety, lambdas, funciones de extensión y data classes | `domain/AmortizationCalculator.kt` (`LoanTerms`, `AmortizationRow`, `rowOrNull`, `money()`, `groupBy`, `sumOf`) |
| Compose: state hoisting, modifiers, Material 3 | `ui/SimulatorScreen.kt`, `ui/CompareScreen.kt`, `ui/ScheduleScreen.kt` |
| MVVM y navegación | `viewmodel/SimulatorViewModel.kt`, `navigation/AppNavigation.kt` |
| REST y JSON con Retrofit y Kotlinx Serialization | `data/EcbApi.kt`, `data/EcbRepository.kt` |
| Pruebas unitarias | `app/src/test` |

## Estrategia de tests

20 tests unitarios con JUnit 4 y `kotlinx-coroutines-test`:

| Clase | Tests | Cubre |
|---|---|---|
| `AmortizationCalculatorTest` | 12 | Cuota, cuadro completo, totales, tipo 0 %, escala, ambas estrategias de amortización anticipada, TAE, `rowOrNull` y validaciones |
| `SimulatorViewModelTest` | 6 | Cálculo con datos válidos, errores de entrada, excepciones del dominio, amortización anticipada y normalización de coma y punto decimal |
| `EcbRepositoryTest` | 2 | Deserialización correcta y fallo de red o formato |

```bash
./gradlew testDebugUnitTest
```

## Stack técnico

| Tecnología | Versión | Uso |
|---|---|---|
| Kotlin | 2.2.10 | Lenguaje |
| Android Gradle Plugin | 9.4.1 | Compilación (Gradle 9.6.0) |
| Compose BOM | 2026.02.01 | UI declarativa y Material 3 |
| Navigation Compose | 2.8.5 | Navegación entre pantallas |
| Lifecycle ViewModel Compose | 2.11.0 | ViewModel en la UI |
| Retrofit | 2.11.0 | Cliente REST |
| OkHttp | 4.12.0 | Cliente HTTP |
| Retrofit Kotlinx Serialization Converter | 1.0.0 | Conversión JSON en Retrofit |
| Kotlinx Serialization | 1.8.0 | Parseo del JSON |
| JUnit | 4.13.2 | Tests unitarios |
| Kotlinx Coroutines Test | 1.9.0 | Tests del ViewModel |

## Tipo de referencia del BCE

La aplicación consulta la API pública de datos estadísticos del Banco Central Europeo:

```
https://data-api.ecb.europa.eu/service/data/FM/B.U2.EUR.4F.KR.MRR_FR.LEV?lastNObservations=1&format=jsondata
```

- Devuelve el último cambio oficial del tipo de las operaciones principales de financiación. No es una cotización diaria.
- Valor comprobado el 2026-10-09: 2,65 % (observación del 2026-09-16).
- El JSON se parsea con `ignoreUnknownKeys = true` y las observaciones nulas se tratan con `Double?`.
- Si falla la red o el parseo, el repositorio captura la excepción y devuelve `Result.failure()`. La `CancellationException` se relanza para respetar la cancelación de corrutinas.

## Diseño de interfaz

- **Accesibilidad:** los bordes de los campos mantienen un contraste mínimo de 3:1: `#788988` sobre `#F5F8F7` en claro (3,43:1) y `#66807E` sobre `#0E1A1C` en oscuro (4,19:1).
- **Tipografía:** serif para las cifras destacadas (`displayLarge`) y números tabulares (`tnum`) en las filas del cuadro para alinear las columnas.
- **Textos:** todos los textos visibles están en recursos, no en el código.
- **Entrada numérica:** se aceptan coma y punto como separador decimal.

## Cómo ejecutarlo

1. Clona el repositorio: `git clone https://github.com/BryantJavaInt/Saldaria.git`
2. Ábrelo con Android Studio.
3. Usa el JDK incluido en Android Studio (JDK 17 o superior).
4. Ejecuta la app en un emulador o dispositivo (`Shift + F10`). La consulta del BCE necesita conexión a internet; sin ella, el resto funciona con normalidad.

## Limitaciones y posibles mejoras

Limitaciones actuales:

- La TAE es aproximada: no incluye comisiones, seguros ni gastos.
- Solo se modela interés fijo durante todo el préstamo.
- No hay persistencia: las simulaciones se pierden al cerrar la app.
- No hay pruebas de UI automatizadas.

Ideas para evolucionarlo (no planificadas):

- Guardar simulaciones en local.
- Soporte de tipo variable (Euríbor + diferencial).
- Pruebas de UI con Compose e integración continua con GitHub Actions.

## Autor

**Bryant Giorgini** · [github.com/BryantJavaInt](https://github.com/BryantJavaInt)
