# Exchange Rates — currency converter for Android

**English** · [Русский](README.ru.md)

A currency converter modelled on the new Xe design: currency cards, instant
recalculation, a calculator built into the amount field, charts, and full offline
operation. The app is free, has no ads and does not move money.

## What works today

- **Converter.** A base "You convert" card and a list of target cards. Typing in
  any card recalculates the others at once. Under each amount there is a unit
  rate such as `1 USD → 84.3300 RUB` and, optionally, the change over 24 hours.
  The card being edited stays above the keyboard.
- **Calculator in the amount field.** Expressions like `20.62+50`, parentheses,
  and percentages with calculator semantics (`100+10%` = 110). A `+ − × ÷ =` bar
  floats above the system keyboard. Typing a digit starts a new number, an
  operator continues from the current one. Both dot and comma are accepted, as
  are grouping separators.
- **199 assets:** 161 world currencies, 34 major cryptocurrencies, 4 precious
  metals. Russian and English names come from ICU, so they match the system
  ones. Search understands the code, the name in both languages, the symbol and
  legacy codes (`RUR`, `XBT`).
- **Central bank mode.** Besides the mid-market rate, official rates of the
  Central Bank of Russia, the Central Bank of Turkey and the National Bank of
  Kazakhstan are available.
- **Offline.** A snapshot of rates ships inside the APK, so the converter works
  from the first launch. Every successful update is saved to the database; the
  footer shows the time of the data and an offline mark.
- **Charts** for 1D / 1W / 1M / 1Y / 5Y with a gradient fill, a draggable
  crosshair and statistics (minimum, maximum, average, change).
- **Appearance:** dark AMOLED and light themes, Russian and English switched
  without restarting the app, 0 to 6 decimal places, optional digit grouping.
- **Background refresh** through WorkManager with a selectable interval and a
  Wi-Fi-only option.

## Data sources

Rates are reduced to a USD pivot and merged by priority with a plausibility
check. None of the sources needs a key.

| Purpose | Sources by priority |
| :--- | :--- |
| Live fiat rate | Coinbase → FloatRates → Frankfurter → open.er-api → fawazahmed0 |
| Cryptocurrencies | Binance → Coinbase → CoinGecko → fawazahmed0 |
| Metals | Gold-API → fawazahmed0 → Frankfurter |
| Official central bank rates | Frankfurter filtered by provider (CBR, TCMB, NBK) |
| Chart history | Frankfurter (fiat, metals), Binance and CoinGecko (crypto), Yahoo Finance (intraday fiat series) |
| Fallback | `assets/initial_rates.json`, bundled in the APK |

Merge rules: for each asset type the highest-priority source with fresh data
wins; its value is compared with the official daily Frankfurter rate and dropped
when it deviates by more than 5 % for fiat, 15 % for metals and 25 % for crypto.
This protects against scale errors, when a source quotes the rate per 100 units.

Source addresses and priorities can be overridden by a remote manifest
(`Endpoints.MANIFEST`) without shipping a new version. Until the manifest is
published, the values from the code are used.

Attribution: open.er-api requires the notice "Rates by exchangerate-api.com",
which is shown in the "Rates and sources" dialog. Binance crypto prices are
quoted in USDT and treated as equal to the dollar, as the same dialog says.

## Build

```bash
./gradlew :app:assembleDebug          # debug APK
./gradlew :app:assembleRelease        # release APK (R8), about 1.9 MB
./gradlew :app:testDebugUnitTest      # 102 tests, including live API checks
```

The release is signed with a key from the `secrets/` folder, which is not part
of the repository; without the key an unsigned APK is built. Signing and the
GitHub Actions secrets are described in [`secrets/README.md`](secrets/README.md)
(in Russian).

An Android SDK with the `android-37.2` platform is required. The build runs on
JDK 21 (Robolectric needs it for SDK 36+), the path is set in
`gradle.properties`; the app bytecode stays at Java 17.

## Releases

A tag of the form `v0.1.0-beta` makes GitHub Actions build a signed APK and
publish it under [Releases](../../releases). A tag with a hyphen (`alpha`,
`beta`, `rc`) is published as a pre-release. The tag has to match `versionName`
in `app/build.gradle.kts`:

```bash
git tag v0.1.0-beta && git push origin v0.1.0-beta
```

## Stack

Gradle 9.7.1, Android Gradle Plugin 9.4.0 with built-in Kotlin 2.2.10,
compileSdk 37.2, minSdk 26, Jetpack Compose (BOM 2026.09.00) with Material 3,
Hilt, Room, Retrofit 3 with kotlinx.serialization, DataStore, WorkManager.
The chart is drawn on a Canvas with no third-party libraries.

## Layout

```
app/src/main/
├── assets/           currencies.json, initial_rates.json (generated, see tools/)
└── java/com/exchangerates/app/
    ├── core/         theme, expression parser, number formatting, DI
    ├── data/         catalog, Room, network sources, rate merging, repositories
    ├── domain/       models: Currency, RateTable, RateMode, HistoryPoint
    ├── presentation/ converter, rates overview, charts, settings, currency picker
    └── work/         background refresh
```

## Tests

| Suite | What it checks |
| :--- | :--- |
| `ExpressionEvaluatorTest` | operator precedence, parentheses, percentages, grouping separators, errors |
| `AmountFormatterTest` | amount and rate formats, adaptive precision for crypto |
| `RateTableTest` | cross rates, conversion, change over 24 hours |
| `RateMergerTest` | source priorities, staleness, outlier rejection |
| `CurrencyFilterTest` | search order, the `RUR` and `XBT` aliases, sections |
| `PristineInputTest` | a digit replaces the pre-filled amount, an operator continues it |
| `CurrencyCatalogTest` | parsing of the generated assets (Robolectric) |
| `CurrencyCardTest` | rendering of the card and the operator bar (Robolectric + Compose) |
| `ConverterScreenTest` | the edited card stays above the keyboard, the list opens from the top |
| `ScreenshotTest` | screens rendered to PNG for a Galaxy S21-sized display |
| `LiveSourcesIntegrationTest` | parsing of live API responses, including every central bank mode |

The last suite talks to the network: it is skipped when there is none and can be
switched off with `-PskipLiveTests`.

## Not there yet

A home screen widget (Glance), rate alerts, drag-and-drop reordering of cards
(for now cards are moved through the long-press menu), an extended list of
cryptocurrencies.

## License

[MIT](LICENSE)
