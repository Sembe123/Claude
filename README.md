# Punktjäger

Ein kleines Reaktionsspiel für Android: Bunte Punkte tauchen auf und schrumpfen –
tippe sie an, bevor sie verschwinden!

## Spielregeln

| Was | Wirkung |
| --- | --- |
| **Bunter Punkt** | Antippen: +10 Punkte. Verschwindet er, verlierst du ein Herz. |
| **Goldener Stern** | Selten und schnell weg: +50 Punkte und ein Herz zurück. |
| **Bombe** | Nicht antippen! Kostet ein Herz. Liegen lassen ist ungefährlich. |
| **Serie** | Alle 10 Treffer in Folge steigt der Multiplikator (bis ×5). Daneben tippen beendet die Serie. |

Du hast 3 Herzen. Je länger du durchhältst, desto schneller und zahlreicher werden die Punkte.
Dein Rekord wird auf dem Handy gespeichert.

## App aufs Handy bringen

Die APK wird bei jedem Push automatisch von GitHub Actions gebaut und im Emulator getestet.

1. Im Repository auf **Actions** gehen und den neuesten erfolgreichen Lauf von **Android** öffnen.
2. Unten bei **Artifacts** auf **Punktjaeger-APK** tippen (du musst bei GitHub angemeldet sein).
3. Die heruntergeladene ZIP-Datei entpacken – darin liegt `Punktjaeger.apk`.
4. Die APK auf dem Handy öffnen und die Installation aus unbekannten Quellen erlauben.

Wird ein Tag wie `v1.0` gepusht, erscheint die APK zusätzlich direkt unter **Releases**.

Neue Versionen lassen sich einfach über die alte installieren, der Rekord bleibt erhalten.

## Selbst bauen

Voraussetzungen: JDK 17 und das Android SDK (am einfachsten über Android Studio).

```bash
./gradlew testDebugUnitTest   # Tests der Spiellogik
./gradlew assembleRelease     # APK unter app/build/outputs/apk/release/
```

Oder das Projekt in Android Studio öffnen und auf ▶ drücken.

## Aufbau

- `app/src/main/java/.../game/GameEngine.kt` – die komplette Spiellogik, ohne Android-Abhängigkeiten
- `app/src/main/java/.../ui/` – Oberfläche mit Jetpack Compose (Menü, Spielfeld, Zeichnen der Punkte)
- `app/src/main/java/.../GameViewModel.kt` – verbindet Logik und Oberfläche, speichert den Rekord
- `app/src/test/` – Unit-Tests für die Spiellogik
- `.github/workflows/android.yml` – baut, testet und startet die App im Emulator

## Hinweis zum Signaturschlüssel

Die APK wird mit `app/punktjaeger.keystore` signiert, der bewusst im Repository liegt, damit sich
jede neue Version über die vorherige installieren lässt. Für eine Veröffentlichung im Play Store
solltest du einen eigenen, geheimen Schlüssel verwenden.
