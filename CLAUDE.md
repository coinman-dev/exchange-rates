# Заметки для работы над проектом

## Сборка и тесты

```bash
./gradlew :app:assembleDebug                        # отладочный APK
./gradlew :app:assembleRelease                      # релизный APK (R8), ~1,8 МБ
./gradlew :app:testDebugUnitTest                    # 84 теста
./gradlew :app:testDebugUnitTest -PskipLiveTests    # 73 теста, без обращений к сети
```

Тесты идут на JDK 21 через toolchain (это требование Robolectric для SDK 36+),
байт-код приложения остаётся Java 17. Нужна платформа SDK `android-37.2`.

## Проверка на устройстве

Есть глобальный инструмент `droid` (документация в `~/.droid/README.md`):

```bash
droid doctor                          # есть ли устройство и KVM
droid emu start && droid wait         # эмулятор без окна
droid install && droid launch com.exchangerates.app.debug
droid shot конвертер                  # PNG в ~/.droid/shots, путь печатается
droid tree                            # подписи и координаты элементов
droid tap "Добавить валюту"
droid crash                           # ошибки из logcat
```

Эмулятору нужен `/dev/kvm`. В WSL2 он появляется только после
`nestedVirtualization=true` в `%USERPROFILE%\.wslconfig` и `wsl --shutdown`.
Без этого остаётся физический телефон по Wi-Fi (`droid pair`, `droid connect`)
или очень медленная программная эмуляция.

## Что важно знать про сборку

AGP 9 несёт **встроенный Kotlin**: плагин `org.jetbrains.kotlin.android`
применять нельзя, а версия Kotlin задаётся самим AGP. Для AGP 9.4.0 это
Kotlin 2.2.10, поэтому плагины Compose и kotlinx.serialization берутся той же
версии, а KSP — `2.2.10-2.0.2`. KSP этой версии регистрирует сгенерированные
исходники через `kotlin.sourceSets`, что встроенный Kotlin запрещает, поэтому в
`gradle.properties` стоит `android.disallowKotlinSourceSets=false`.

При обновлении AGP сначала посмотрите в POM `com.android.tools.build:gradle`,
какие версии `kotlin-gradle-plugin` и KSP он тянет, и приведите каталог версий
в соответствие.

## Устройство кода

| Слой | Где | За что отвечает |
| :--- | :--- | :--- |
| Справочник | `data/catalog` | 199 активов из `assets/currencies.json` |
| Источники | `data/source` | опрос API, слияние в `RateMerger` |
| Хранение | `data/local` | Room (курсы, список валют, история), DataStore (настройки) |
| Домен | `domain/model` | `RateTable` с опорой USD, кросс-курсы |
| Конвертер | `presentation/converter` | экран по макету Xe, калькулятор в поле |
| Графики | `presentation/chart` | Canvas-график с прицелом |

Ассеты `currencies.json` и `initial_rates.json` **генерируются**: правьте
`tools/gen_catalog.py`, а не файлы напрямую (инструкция в `tools/README.md`).

Курсы всегда хранятся как «сколько единиц валюты за 1 USD»; база в интерфейсе
может быть любой, кросс-курс считается делением в `RateTable`.

## Что не доделано

Виджет рабочего стола (Glance), уведомления о курсе, перетаскивание карточек,
расширенный список криптовалют. План и решения заказчика — в
`.ai/spec_v2_research_design_datasources.md`.
