# Generare l'APK di AUTOMICOSTA

## Metodo consigliato: GitHub Actions
1. Crea un repository GitHub e carica tutto il contenuto di questa cartella.
2. Apri la scheda **Actions** del repository.
3. Seleziona **Build AUTOMICOSTA APK**.
4. Premi **Run workflow**.
5. Al termine, scarica l'artifact **AUTOMICOSTA-APK**.
6. Dentro troverai `AUTOMICOSTA-debug.apk`, installabile su Android abilitando l'installazione da fonti consentite per il browser/file manager usato.

La workflow usa JDK 17, Gradle 9.6.0 e compila `:app:assembleDebug`.

## Da Android Studio
Apri la cartella come progetto, attendi il Gradle Sync e scegli:
**Build > Build App Bundle(s) / APK(s) > Build APK(s)**.

L'APK debug viene generato normalmente in:
`app/build/outputs/apk/debug/app-debug.apk`
