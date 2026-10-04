# Заметки для работы над проектом

## Сборка и тесты

```bash
./gradlew :app:assembleDebug                        # отладочный APK
./gradlew :app:assembleRelease                      # релизный APK (R8), ~1,9 МБ
./gradlew :app:testDebugUnitTest                    # 102 теста
./gradlew :app:testDebugUnitTest -PskipLiveTests    # 91 тест, без обращений к сети
```

Тесты идут на JDK 21 через toolchain (это требование Robolectric для SDK 36+),
байт-код приложения остаётся Java 17. Нужна платформа SDK `android-37.2`.

## Подпись релиза и секреты

Ключ подписи и пароли лежат в `secrets/` (в git попадает только
`secrets/README.md`, там же описание). `assembleRelease` читает
`secrets/signing.properties`, на CI те же четыре значения `RELEASE_*` приходят
из секретов репозитория. Без ключа релиз собирается неподписанным
(`app-release-unsigned.apk`) и на телефон не ставится.

## Выпуск версии

Тег `vX.Y.Z` (он должен совпадать с `versionName` в `app/build.gradle.kts`)
запускает `.github/workflows/release.yml`: тесты, подписанный APK, публикация в
GitHub Releases. Тег с дефисом (`v0.1.0-beta`) публикуется как пререлиз.
С каждым выпуском нужно увеличивать `versionCode`. Описание релиза (на
английском) кладётся в `.github/release-notes/<тег>.md`; без файла GitHub
составит его из списка коммитов.

## Как посмотреть вёрстку без телефона

Экраны рендерятся в PNG прямо на JVM (Robolectric с нативной графикой),
параметры экрана заданы как у Galaxy S21 (1080×2400, xxhdpi, русский язык):

```bash
./gradlew :app:testDebugUnitTest --tests "*ScreenshotTest*"
ls app/build/screenshots/     # converter-dark-ru.png, settings-dark-ru.png, picker-dark-ru.png …
```

Это основной способ проверить длинные подписи и переносы: именно на узком
экране с русским языком строки не помещались. Новый экран добавляется одним
тестом в `ScreenshotTest`.

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
Без KVM эмулятор запускается (проверено: загрузка около двух минут на
облегчённом образе `aosp_atd`), приложение работает и управляется через
`droid tap` и `droid tree`, но **снимки экрана получаются пустыми** — на
программной эмуляции кадр не собирается. Поэтому для вёрстки используйте
`ScreenshotTest`, а `droid` — для проверки поведения.

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
`.ai/spec_v2_research_design_datasources.md` (папка `.ai/` в репозиторий не
входит и есть только на рабочей машине).
