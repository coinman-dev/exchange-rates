# -*- coding: utf-8 -*-
"""Генератор справочника валют и стартового снимка курсов для Android-приложения."""
import json, csv, io, os, collections

OUT_DIR = "/home/modulator/Development/exchange-rates/app/src/main/assets"

frank = json.load(open("frank_currencies.json"))
faw_names = json.load(open("faw_currencies.json"))
faw_usd = json.load(open("faw_usd.json"))
cur2country = json.load(open("cur2country.json"))
erapi = json.load(open("erapi_usd.json"))
coinbase = json.load(open("cb_usd.json"))["data"]["rates"]

icu = {}
for row in csv.reader(open("icu.tsv", encoding="utf-8"), delimiter="\t"):
    if len(row) >= 6:
        icu[row[0]] = dict(nameEn=row[1], nameRu=row[2], symEn=row[3], symRu=row[4], frac=int(row[5]))

METALS = ["XAU", "XAG", "XPT", "XPD"]

# --- канонические страны для валют нескольких стран / отсутствующих в CLDR ---
COUNTRY_OVERRIDE = {
    "USD": "US", "EUR": "EU", "GBP": "GB", "CHF": "CH", "AUD": "AU", "NZD": "NZ",
    "DKK": "DK", "ILS": "IL", "INR": "IN", "NOK": "NO", "MAD": "MA", "ZAR": "ZA",
    "CNY": "CN", "CNH": "CN", "ANG": "CW", "XCG": "CW", "AWG": "AW", "TRY": "TR",
    "GGP": "GG", "IMP": "IM", "JEP": "JE", "FKP": "FK", "SHP": "SH", "GIP": "GI",
    "XOF": None, "XAF": None, "XCD": None, "XPF": None, "XDR": None,
    "MRO": "MR", "STN": "ST", "SLE": "SL", "ZWG": "ZW", "VES": "VE", "SSP": "SS",
    "KPW": "KP", "SYP": "SY", "CUP": "CU", "IRR": "IR", "BTN": "BT", "LSL": "LS",
    "SZL": "SZ", "NAD": "NA", "TWD": "TW", "MOP": "MO", "HKD": "HK", "BMD": "BM",
    "KYD": "KY", "TVD": "TV", "MVR": "MV",
}

# --- названия для кодов, которых нет в ICU ---
MANUAL_FIAT_NAMES = {
    "CNH": ("Chinese Yuan (offshore)", "китайский юань (офшорный)"),
    "GGP": ("Guernsey Pound", "гернсийский фунт"),
    "IMP": ("Isle of Man Pound", "мэнский фунт"),
    "JEP": ("Jersey Pound", "джерсийский фунт"),
}

# --- металлы: свои названия (ICU даёт просто «Золото») ---
METAL_NAMES = {
    "XAU": ("Gold (troy ounce)", "Золото (тройская унция)", "oz t"),
    "XAG": ("Silver (troy ounce)", "Серебро (тройская унция)", "oz t"),
    "XPT": ("Platinum (troy ounce)", "Платина (тройская унция)", "oz t"),
    "XPD": ("Palladium (troy ounce)", "Палладий (тройская унция)", "oz t"),
}

# --- основные криптовалюты: код -> (nameEn, nameRu, symbol, decimals) ---
# отобраны те, что есть и в fawazahmed0, и в публичных эндпоинтах Coinbase/Binance
CRYPTO = collections.OrderedDict([
    ("BTC",   ("Bitcoin", "Биткойн", "₿", 8)),
    ("ETH",   ("Ethereum", "Эфириум", "Ξ", 8)),
    ("USDT",  ("Tether", "Tether", "₮", 4)),
    ("USDC",  ("USD Coin", "USD Coin", "", 4)),
    ("BNB",   ("BNB", "BNB", "", 6)),
    ("SOL",   ("Solana", "Solana", "", 6)),
    ("XRP",   ("XRP", "XRP", "", 6)),
    ("TON",   ("Toncoin", "Toncoin", "", 6)),
    ("TRX",   ("TRON", "TRON", "", 6)),
    ("ADA",   ("Cardano", "Cardano", "", 6)),
    ("DOGE",  ("Dogecoin", "Dogecoin", "", 6)),
    ("LTC",   ("Litecoin", "Litecoin", "Ł", 8)),
    ("DOT",   ("Polkadot", "Polkadot", "", 6)),
    ("AVAX",  ("Avalanche", "Avalanche", "", 6)),
    ("LINK",  ("Chainlink", "Chainlink", "", 6)),
    ("XMR",   ("Monero", "Monero", "ɱ", 8)),
    ("BCH",   ("Bitcoin Cash", "Bitcoin Cash", "", 8)),
    ("XLM",   ("Stellar", "Stellar", "", 6)),
    ("ATOM",  ("Cosmos", "Cosmos", "", 6)),
    ("NEAR",  ("NEAR Protocol", "NEAR Protocol", "", 6)),
    ("SUI",   ("Sui", "Sui", "", 6)),
    ("APT",   ("Aptos", "Aptos", "", 6)),
    ("UNI",   ("Uniswap", "Uniswap", "", 6)),
    ("ETC",   ("Ethereum Classic", "Ethereum Classic", "", 8)),
    ("SHIB",  ("Shiba Inu", "Shiba Inu", "", 8)),
    ("ARB",   ("Arbitrum", "Arbitrum", "", 6)),
    ("OP",    ("Optimism", "Optimism", "", 6)),
    ("FIL",   ("Filecoin", "Filecoin", "", 6)),
    ("ICP",   ("Internet Computer", "Internet Computer", "", 6)),
    ("HBAR",  ("Hedera", "Hedera", "", 6)),
    ("ALGO",  ("Algorand", "Algorand", "", 6)),
    ("XTZ",   ("Tezos", "Tezos", "", 6)),
    ("AAVE",  ("Aave", "Aave", "", 6)),
    ("PAXG",  ("PAX Gold", "PAX Gold", "", 8)),
])

# --- порядок популярности (первые — самые востребованные) ---
POPULAR = [
    "USD", "EUR", "RUB", "TRY", "KZT", "GBP", "CNY", "JPY", "CHF", "AED",
    "UAH", "BYN", "UZS", "GEL", "AMD", "AZN", "KGS", "TJS", "TMT", "MDL",
    "INR", "CAD", "AUD", "PLN", "RON", "MXN", "BRL", "KRW", "THB", "VND",
    "IDR", "ILS", "EGP", "SAR", "QAR", "SEK", "NOK", "DKK", "CZK", "HUF",
    "BGN", "RSD", "MNT", "HKD", "SGD", "NZD", "ZAR", "TWD", "MYR", "PHP",
    "BTC", "ETH", "USDT", "XAU", "XAG",
]

entries = {}

# ---- фиат + металлы из Frankfurter ----
for c in frank:
    code = c["iso_code"]
    kind = "METAL" if code in METALS else "FIAT"
    i = icu.get(code, {})
    if kind == "METAL":
        name_en, name_ru, sym = METAL_NAMES[code]
        decimals = 4
    else:
        if code in MANUAL_FIAT_NAMES:
            name_en, name_ru = MANUAL_FIAT_NAMES[code]
        else:
            name_en = i.get("nameEn") or c.get("name") or code
            name_ru = i.get("nameRu") or name_en
        sym = i.get("symRu") or i.get("symEn") or c.get("symbol") or ""
        frac = i.get("frac", 2)
        decimals = 2 if frac < 0 else frac
    if code in COUNTRY_OVERRIDE:
        country = COUNTRY_OVERRIDE[code]
    else:
        cc = cur2country.get(code) or []
        country = cc[0] if len(cc) == 1 else None
    entries[code] = dict(
        code=code, kind=kind, country=country,
        nameEn=name_en, nameRu=name_ru, symbol=sym, decimals=decimals,
    )

# ---- криптовалюты ----
for code, (name_en, name_ru, sym, dec) in CRYPTO.items():
    entries[code] = dict(code=code, kind="CRYPTO", country=None,
                         nameEn=name_en, nameRu=name_ru, symbol=sym, decimals=dec)

# ---- ранги ----
for idx, code in enumerate(POPULAR):
    if code in entries:
        entries[code]["rank"] = idx + 1
rest = sorted(c for c in entries if "rank" not in entries[c])
for idx, code in enumerate(rest):
    entries[code]["rank"] = 1000 + idx

order = sorted(entries.values(), key=lambda e: (e["rank"], e["code"]))
os.makedirs(OUT_DIR, exist_ok=True)
with open(os.path.join(OUT_DIR, "currencies.json"), "w", encoding="utf-8") as f:
    json.dump(order, f, ensure_ascii=False, indent=1)

# ---- стартовый снимок курсов (pivot USD) ----
usd = faw_usd["usd"]
rates = {}
missing = []
for code in entries:
    v = usd.get(code.lower())
    if v is None:
        v = erapi.get("rates", {}).get(code)
        if v is not None:
            v = float(v)
    if v is None and code in coinbase:
        v = float(coinbase[code])
    if v is None:
        missing.append(code)
    else:
        rates[code] = round(float(v), 10)
rates["USD"] = 1.0
snapshot = dict(base="USD", date=faw_usd["date"], source="fawazahmed0/currency-api",
                fetchedAt=faw_usd["date"] + "T00:00:00Z", rates=dict(sorted(rates.items())))
with open(os.path.join(OUT_DIR, "initial_rates.json"), "w", encoding="utf-8") as f:
    json.dump(snapshot, f, ensure_ascii=False, indent=1)

kinds = collections.Counter(e["kind"] for e in order)
print("currencies.json:", len(order), dict(kinds))
print("no flag country:", sorted(e["code"] for e in order if e["kind"] == "FIAT" and not e["country"]))
print("initial_rates.json:", len(rates), "rates; missing:", missing)
print("date:", faw_usd["date"])
