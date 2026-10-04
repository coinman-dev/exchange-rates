# tools — генерация справочника валют

`gen_catalog.py` собирает `app/src/main/assets/currencies.json` (199 валют: фиат + крипта + металлы)
и `app/src/main/assets/initial_rates.json` (стартовый снимок курсов, pivot USD).

Как перегенерировать:

```bash
cd /tmp && mkdir -p ratesgen && cd ratesgen
curl -s "https://cdn.jsdelivr.net/npm/@fawazahmed0/currency-api@latest/v1/currencies.json" -o faw_currencies.json
curl -s "https://cdn.jsdelivr.net/npm/@fawazahmed0/currency-api@latest/v1/currencies/usd.json" -o faw_usd.json
curl -s "https://api.frankfurter.dev/v2/currencies" -o frank_currencies.json
curl -s "https://open.er-api.com/v6/latest/USD" -o erapi_usd.json
curl -s "https://api.coinbase.com/v2/exchange-rates?currency=USD" -o cb_usd.json
cp <repo>/tools/{IcuDump.java,MapGen.java,gen_catalog.py} .
javac MapGen.java && java MapGen > cur2country.json          # валюта -> страны (CLDR)
javac IcuDump.java
python3 -c "import json;print('\n'.join(sorted(c['iso_code'] for c in json.load(open('frank_currencies.json')))))" | java IcuDump > icu.tsv
python3 gen_catalog.py
```

Названия и символы валют берутся из ICU (JDK), поэтому русские названия совпадают с системными.

## Логотип и значок приложения

`make_logo.sh` вырезает из `images/logos-orig.png` два круга и собирает из них
ресурсы (нужен ImageMagick 7):

- `images/logo-round.png` и `images/logo-text-round.png` — круги без надписи и с надписью;
- `res/drawable-nodpi/logo_about.webp` — логотип в разделе «О приложении»;
- `res/mipmap-*/ic_launcher_foreground.webp` и `ic_launcher_monochrome.webp` — слои
  адаптивного значка приложения.

```bash
tools/make_logo.sh
```
