# Utrošak – Android aplikacija za utrošak namirnica (CW restoran)

Aplikacija za popunjavanje dnevnog utroška namirnica po šablonu „Utrošak za restoran za zaposlene”.

- **Datum** – svaki datum je posebna lista (kartica **Istorija** otvara ranije liste).
- **Jela** – naziv jela, a sastav je spisak kratkih redova (npr. *Crni luk … 45 kg*). Dodir na red ili na
  **+ Dodaj artikal** otvara prozor za unos: naziv (sa predlozima ranije kucanih artikala), jedinica mere
  **kg / l / kom / kutija**, polje **+ Dodato** i polje **− Oduzeto**, a ispod **Konačno** (dodato − oduzeto).
  Oba polja su kalkulator: `20+15+15`, `3×15`, decimalni zarez; tasteri **+ × ⌫** su pored polja.
  *Sačuvaj i dodaj sledeći* ubrzava unos.
- **Besplatni prilozi**, **Salata**, **Salate vezane**, **Korekcije**, **Potpisi** – sekcije koje se otvaraju dodirom.
- **Zbir** – isti artikli iz svih jela, priloga i salata se automatski sabiraju po jedinici mere
  (velika/mala slova i kvačice ne smetaju: „crni luk” = „Crni Luk”). Uz svaki artikal piše odakle dolazi.
  Artikal koji se ponavlja u više jela dobija oznaku „u više jela · sabira se”.
- **Excel (.xlsx)** – sređena tabela sa bojama i okvirima: list „Zbir” (ukupno po artiklu, dodato, oduzeto, gde se koristi)
  i list „Utrošak” po šablonu (svako jelo kao posebna tabela, otpis crveno, GN, prilozi, salate, potpisi).
  Podešeno za štampu na A4. Pravi ga `app/src/main/assets/xlsx-writer.js` (bez spoljnih biblioteka).
- Kartice: **Unos**, **Zbir i slanje**, **Istorija**, **Podešavanja** (primaoci i nalog za slanje).
- **Pošalji Excel mejlom** – adrese primalaca se upišu jednom (kartica **Zbir** → *Slanje mejlom*) i pamte se.
  Dugme otvara Gmail (ili drugu mejl aplikaciju) sa upisanim primaocima, naslovom, zbirom u tekstu i Excel fajlom u prilogu;
  ostaje samo da se pritisne „Pošalji”. Dugme je i na dnu kartice **Unos**.
- **Automatsko slanje** – u kartici *Podešavanja → Nalog sa koga se šalje* upišite Gmail adresu (podrazumevano
  `utrosak.cw.restoran@gmail.com`) i **lozinku za aplikaciju** od 16 slova (myaccount.google.com/apppasswords).
  Lozinka se čuva samo na telefonu. Tada dugme **Pošalji Excel mejlom** šalje odmah, bez otvaranja Gmail-a
  (preko smtp.gmail.com, port 465). Dugme „Sačuvaj i pošalji probni mejl” proverava da li prijava radi.
- **Podeli zbir** – šalje zbir kao tekst (Viber, WhatsApp, e-mail…).
- Podaci se čuvaju automatski na telefonu. Aplikacija radi bez interneta.

## Kako je napravljena

Ekran je HTML stranica (`app/src/main/assets/index.html`) prikazana u Android WebView-u
(`MainActivity.java`). Excel fajl pravi `xlsx-writer.js`, a mejl šalje `SmtpSender.java`.

## Preuzimanje APK-a

Svaki push na GitHub pokreće GitHub Actions koji pravi APK i objavljuje ga u sekciji **Releases** (`Utrosak.apk`).
Na telefonu dozvolite instalaciju iz nepoznatih izvora.
