English · [Español](README.md)

# Saldaria

Mortgage and loan simulator for Android. Calculates the monthly payment, generates the complete amortization schedule, and compares the effect of early repayments, using the **French amortization system (constant payment)**, the most common for loans and mortgages in Spain.

Kotlin · Jetpack Compose · Material 3 · MVVM · minSdk 26

> **Educational project.** This does not constitute financial advice. The TAE (Spanish effective annual rate) is an approximation and does not include commissions, formalization expenses, or linked products.

## Features

- **Simulator:** monthly payment, total interest, total paid, and approximate TAE (Spanish effective annual rate).
- **Amortization schedule:** month-by-month detail grouped by expandable years, with payment, interest, principal paid, and remaining balance.
- **Distribution chart:** evolution of interest versus principal, drawn with `Canvas` without charting libraries.
- **Early repayment comparator:** savings in interest and months with the two common strategies: reduce term or reduce payment.
- **ECB reference rate:** checks the latest official rate of the main refinancing operations, applicable as the nominal annual rate (TIN) with one tap.
- **Light and dark theme.**

## Reference results

Loan of 100,000.00 € at 3.00 % for 25 years (300 months):

| Concept | Result |
|---|---|
| Monthly payment | 474.21 € |
| Last payment (adjusted) | 474.70 € |
| Interest of month 1 | 250.00 € |
| Balance at the end of year 1 | 97,272.18 € |

With an early repayment of 20,000.00 € in month 12:

| Strategy | Result |
|---|---|
| Reduce term | Ends in 222 months (78 less). Savings of 17,207.90 € in interest |
| Reduce payment | Term of 300 months. Payment of 376.71 € from month 13. Savings of 8,080.77 € in interest |

These values are locked in the tests and were verified against an independent reference implementation.

## How the calculation works

The constant payment is obtained with the French system formula:

```
payment = P · r · (1 + r)^n / ((1 + r)^n − 1)
```

where `P` is the principal, `r = nominal annual rate (TIN) / 1200` is the monthly rate, and `n` is the number of months.

The schedule is built month by month: the interest is calculated on the remaining balance, the rest of the payment pays off the principal, and the last payment is adjusted to leave the balance at exactly 0.00 €. Early repayments are applied after the payment of the indicated month; in the "reduce payment" mode, the payment is recalculated over the remaining months.

## Technical decisions

| Decision | Reason |
|---|---|
| `BigDecimal` for all money, with `MathContext.DECIMAL128` and `HALF_UP` rounding to 2 decimals | Avoids the binary representation errors of `Double`. The `domain` layer does not use `Double` or `Float`. |
| Last payment adjustment | Monthly roundings accumulate a small drift. The last payment absorbs it and guarantees a final balance of 0.00 €. |
| Domain as pure Kotlin code, with immutable functions and data classes | It is tested without an emulator or Android dependencies, and the financial logic is separated from the interface. |
| Validation with `require` in the engine and input errors as `InputError` enum in the ViewModel | The domain rejects invalid data with `IllegalArgumentException`. The interface translates the error to a text from `strings.xml` and marks the affected field. |
| Single state (`UiState`) exposed with `StateFlow` | Unidirectional data flow: screens only draw state and send events to the ViewModel through lambdas (state hoisting). |
| Explicit result in the network layer (`Result`) | A network or parsing failure does not break the app: an error state is shown and the rest of the functions remain available. |
| Custom chart with `Canvas` | Avoids a dependency for a single simple chart. |
| No dependency injection framework | The size of the project does not justify it. |

## Architecture

The application follows **MVVM**. The `domain` layer is a package without Android dependencies.

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

## Technical areas covered

| Area | Where to see it |
|---|---|
| Kotlin: null safety, lambdas, extension functions, and data classes | `domain/AmortizationCalculator.kt` (`LoanTerms`, `AmortizationRow`, `rowOrNull`, `money()`, `groupBy`, `sumOf`) |
| Compose: state hoisting, modifiers, Material 3 | `ui/SimulatorScreen.kt`, `ui/CompareScreen.kt`, `ui/ScheduleScreen.kt` |
| MVVM and navigation | `viewmodel/SimulatorViewModel.kt`, `navigation/AppNavigation.kt` |
| REST and JSON with Retrofit and Kotlinx Serialization | `data/EcbApi.kt`, `data/EcbRepository.kt` |
| Unit testing | `app/src/test` |

## Testing strategy

20 unit tests with JUnit 4 and `kotlinx-coroutines-test`:

| Class | Tests | Covers |
|---|---|---|
| `AmortizationCalculatorTest` | 12 | Payment, full schedule, totals, 0% rate, scale, both early repayment strategies, TAE (Spanish effective annual rate), `rowOrNull`, and validations |
| `SimulatorViewModelTest` | 6 | Calculation with valid data, input errors, domain exceptions, early repayment, and comma/dot decimal normalization |
| `EcbRepositoryTest` | 2 | Correct deserialization and network or format failure |

```bash
./gradlew testDebugUnitTest
```

## Tech stack

| Technology | Version | Use |
|---|---|---|
| Kotlin | 2.2.10 | Language |
| Android Gradle Plugin | 9.4.1 | Compilation (Gradle 9.6.0) |
| Compose BOM | 2026.02.01 | Declarative UI and Material 3 |
| Navigation Compose | 2.8.5 | Navigation between screens |
| Lifecycle ViewModel Compose | 2.11.0 | ViewModel in the UI |
| Retrofit | 2.11.0 | REST client |
| OkHttp | 4.12.0 | HTTP client |
| Retrofit Kotlinx Serialization Converter | 1.0.0 | JSON conversion in Retrofit |
| Kotlinx Serialization | 1.8.0 | JSON parsing |
| JUnit | 4.13.2 | Unit testing |
| Kotlinx Coroutines Test | 1.9.0 | ViewModel testing |

## ECB reference rate

The application queries the public statistical data API of the European Central Bank:

```
https://data-api.ecb.europa.eu/service/data/FM/B.U2.EUR.4F.KR.MRR_FR.LEV?lastNObservations=1&format=jsondata
```

- Returns the last official change in the rate of the main refinancing operations. It is not a daily quote.
- Value verified on 2026-10-09: 2.65 % (observation from 2026-09-16).
- The JSON is parsed with `ignoreUnknownKeys = true` and null observations are handled with `Double?`.
- If the network or parsing fails, the repository catches the exception and returns `Result.failure()`. The `CancellationException` is rethrown to respect coroutine cancellation.

## Interface design

- **Accessibility:** field borders maintain a minimum contrast of 3:1: `#788988` on `#F5F8F7` in light mode (3.43:1) and `#66807E` on `#0E1A1C` in dark mode (4.19:1).
- **Typography:** serif for highlighted figures (`displayLarge`) and tabular numbers (`tnum`) in the schedule rows to align columns.
- **Texts:** all visible texts are in resources, not in the code.
- **Numeric input:** both comma and dot are accepted as decimal separators.

## How to run it

1. Clone the repository: `git clone https://github.com/BryantJavaInt/Saldaria.git`
2. Open it with Android Studio.
3. Use the JDK included in Android Studio (JDK 17 or higher).
4. Run the app on an emulator or device (`Shift + F10`). The ECB query requires an internet connection; without it, the rest functions normally.

## Limitations and possible improvements

Current limitations:

- The TAE (Spanish effective annual rate) is an approximation: it does not include commissions, insurance, or expenses.
- Only a fixed rate is modeled for the entire loan.
- No persistence: simulations are lost when the app is closed.
- No automated UI tests.

Ideas for evolution (not planned):

- Save simulations locally.
- Support for variable rate (Euribor + spread).
- UI testing with Compose and continuous integration with GitHub Actions.

## Author

**Bryant Giorgini** · [github.com/BryantJavaInt](https://github.com/BryantJavaInt)
