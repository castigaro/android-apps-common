# android-apps-common

Gemeinsame Android-Bibliothek der castigaro-Apps: Basis-Theme
(`Theme.AppSonar`), Update-Erkennung (`UpdateChecker`/`UpdateUi`) und die
komplette KI-Provider-/API-Key-Verwaltung (`ProviderSettings`,
`ProviderSettingsController`, `ModelPricing`).

## Einbindung

Dieses Repository liegt als **Geschwister-Verzeichnis** neben den App-Repos
und wird von jeder App als Gradle-Modul eingebunden (`settings.gradle.kts`):

```kotlin
include(":common")
project(":common").projectDir = file("../android-apps-common/android")
```

Dazu in `app/build.gradle.kts`: `implementation(project(":common"))`.

Die CI der Apps checkt dieses Repository neben dem App-Repo aus und baut
immer gegen den aktuellen `main`-Stand.

## Grundsätze

- **Lokal gewinnt:** App-Ressourcen überstimmen gleichnamige Defaults der
  Bibliothek.
- Kein eigener Gradle-Wrapper — die Bibliothek wird nie allein gebaut; ihre
  Unit-Tests laufen in jeder App-CI über `testDebugUnitTest` mit.
- Ein Push auf `main` stößt die Build-Workflows aller App-Repos an
  (`.github/workflows/trigger-apps.yml`).
