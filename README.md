# Utrošak – Android aplikacija za utrošak namirnica (CW restoran)

Aplikacija za popunjavanje dnevnog utroška namirnica po šablonu „Utrošak za restoran za zaposlene”.

- **Datum** – svaki datum je posebna lista (kartica **Istorija** otvara ranije liste).
- **Jela** – naziv jela, sastav: artikal, količina i jedinica mere **kg / l / kom / kutija**.
  Ispod jela: koliko je napravljeno GN, preostala količina u GN, otpis u porcijama.
- **Oduzimanje** – dugme **+ / −** na svakoj stavki; stavka sa − se oduzima od zbira (u Excel-u ide u kolonu *Otpis*).
  Odeljak **Korekcije i povrat** uvek oduzima.
- **Besplatni prilozi**, **Salata**, **Salate vezane** – kao u šablonu.
- **Zbir** – isti artikli iz svih jela, priloga i salata se automatski sabiraju po jedinici mere
  (velika/mala slova i kvačice ne smetaju: „crni luk” = „Crni Luk”). Uz svaki artikal piše odakle dolazi.
  Artikal koji se ponavlja u više jela dobija oznaku „u više jela · sabira se”.
- **Izvezi u Excel (.xlsx)** – list „Utrošak” (po šablonu) i list „Zbir”.
- **Podeli zbir** – šalje zbir kao tekst (Viber, WhatsApp, e-mail…).
- Podaci se čuvaju automatski na telefonu. Aplikacija radi bez interneta.

## Kako je napravljena

Ekran je HTML stranica (`app/src/main/assets/index.html`) prikazana u Android WebView-u
(`MainActivity.java`). Excel fajl pravi biblioteka SheetJS (`xlsx.full.min.js`, Apache 2.0 licenca).

## Preuzimanje APK-a

Svaki push na GitHub pokreće GitHub Actions koji pravi APK i objavljuje ga u sekciji **Releases** (`Utrosak.apk`).
Na telefonu dozvolite instalaciju iz nepoznatih izvora.
