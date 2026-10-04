# secrets/

Всё, что не должно попасть на GitHub, лежит в этой папке: ключ подписи релиза и
пароли к нему. `.gitignore` игнорирует `/secrets/*` и оставляет только этот
README, поэтому соглашение приезжает вместе с репозиторием, а ключи — нет.

## Что здесь лежит

| Файл | Что это | Если потерять |
| :--- | :--- | :--- |
| `exchange-rates.jks` | Хранилище с ключом подписи релиза, псевдоним `exchange-rates` | Обновить приложение на телефоне, где оно уже стоит, будет нельзя: Android не принимает APK с другой подписью. **Держите копию вне этой машины.** |
| `signing.properties` | Четыре значения `RELEASE_*`: путь к хранилищу, пароли и псевдоним | Пароли записаны только здесь, без них хранилище бесполезно. |

Резервная копия — это копия всей папки: `cp -a secrets/ …`.

## Как этим пользуется сборка

`app/build.gradle.kts` читает `secrets/signing.properties`:

```properties
RELEASE_STORE_FILE=exchange-rates.jks
RELEASE_STORE_PASSWORD=…
RELEASE_KEY_ALIAS=exchange-rates
RELEASE_KEY_PASSWORD=…
```

Путь к хранилищу задаётся относительно `secrets/`. Если файла нет, те же четыре
имени берутся из переменных окружения — так значения получает GitHub Actions.
Если нет ни файла, ни переменных, `assembleRelease` не падает, а собирает
`app-release-unsigned.apk`: он годится для проверки R8, но на телефон не ставится.

Проверить, чем подписан собранный APK:

```bash
~/Android/Sdk/build-tools/37.0.0/apksigner verify --print-certs \
  app/build/outputs/apk/release/app-release.apk
```

## GitHub Actions

Сборка на GitHub берёт ключ из секретов репозитория
(Settings → Secrets and variables → Actions):

| Секрет | Значение |
| :--- | :--- |
| `KEYSTORE_BASE64` | файл `exchange-rates.jks` в base64 |
| `KEYSTORE_PASSWORD` | `RELEASE_STORE_PASSWORD` |
| `KEY_ALIAS` | `RELEASE_KEY_ALIAS` |
| `KEY_PASSWORD` | `RELEASE_KEY_PASSWORD` |

Записать их заново из этой папки (значения идут через stdin и на экран не выводятся):

```bash
val() { grep "^$1=" secrets/signing.properties | cut -d= -f2- | tr -d '\n'; }
base64 -w0 secrets/exchange-rates.jks | gh secret set KEYSTORE_BASE64
val RELEASE_STORE_PASSWORD | gh secret set KEYSTORE_PASSWORD
val RELEASE_KEY_ALIAS      | gh secret set KEY_ALIAS
val RELEASE_KEY_PASSWORD   | gh secret set KEY_PASSWORD
```

Прочитать секрет обратно из GitHub нельзя, так что резервной копией он не служит.

## На другой машине

```bash
mkdir -p secrets && chmod 700 secrets
cp /путь/к/копии/exchange-rates.jks /путь/к/копии/signing.properties secrets/
chmod 600 secrets/exchange-rates.jks secrets/signing.properties
```
