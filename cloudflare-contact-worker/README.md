# Mazsola Klub contact Worker

Ez a Worker szolgálja ki az óvodai/bölcsődei dalötlet űrlapot:

`POST https://mazsolaklub.com/api/contact`

A meglévő statikus weboldalhoz nem nyúl. Külön Worker route-ként csak az `/api/contact*` útvonalat kezeli.

## Mit tud

- JSON űrlapkérés fogadása
- kötelező e-mail és üzenet validáció
- hosszkorlátok
- honeypot spamvédelem
- minimum kitöltési idő ellenőrzés
- túl sok linket tartalmazó üzenetek blokkolása
- HTML escaping
- Reply-To a beküldő e-mail címére
- értesítő levél küldése Cloudflare Email Sending bindinggel

## Cloudflare dashboard beállítás

1. Workers & Pages → Create → Worker
2. Worker neve: `mazsola-contact-form`
3. A Worker kódjához másold be a `src/index.js` tartalmát
4. Settings / Bindings alatt adj hozzá Email Sending bindingot:
   - Binding name: `EMAIL`
5. Adj hozzá route-ot:
   - `mazsolaklub.com/api/contact*`
6. Variables:
   - `CONTACT_TO=klubmazsola@gmail.com`
   - `CONTACT_FROM=web@mazsolaklub.com`

A `CONTACT_TO` címnek Cloudflare-ben verified destination címnek kell lennie. Jelenleg: `klubmazsola@gmail.com`.
A `CONTACT_FROM` feladó domainjének Email Service küldésre engedélyezett domainnek kell lennie.

## Frontend

Az `ovodaknak/contact.js` már a fenti végpontra küld, és elküldi a honeypot + kitöltési idő mezőket is.
