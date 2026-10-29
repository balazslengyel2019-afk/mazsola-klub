# Mazsola Klub Hógolyócsata – Android v1.0

Native Android full-screen shell around the **v1.8** web game. All graphics, SFX, music and animated intro are bundled in `app/src/main/assets/game/` at build time. The game is playable offline; outbound social-media links open the phone's default browser.

- package ID: `com.mazsolaklub.hogolyocsata`
- min Android: 7.0 / API 24
- landscape orientation; immersive full-screen; touch controls
- build: `gradle -p android-hogolyocsata :app:assembleDebug`
- APK artifact: `Mazsola_Klub_Hogolyocsata_v1.0.apk`

Builds here use the Android debug signing key. For long-term production upgrades, set up a persistent private signing key before a public release. Do not commit a private signing key to GitHub.
