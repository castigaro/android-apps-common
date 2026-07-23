# android-apps-common

Gemeinsame Android-Bibliothek der AppSonar-Apps: Basis-Theme
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

## Nach jedem Push auf `main`: App-Builds anstoßen

Eine Änderung hier betrifft alle Apps, aber deren Release-APKs bauen sich
nicht von selbst neu (bewusst kein PAT-Secret für einen automatischen
Cross-Repo-Trigger). Deshalb direkt nach dem Push die Build-Workflows über
die lokal angemeldete GitHub-CLI starten:

```powershell
foreach ($r in 'app-markdown-viewer','app-homesonar','app-storyteller',
               'app-nutrisonar','app-kap-krakenzahn',
               'app-the-llm-adventure','app-weightsonar') {
    gh workflow run build.yml -R "AppSonar/$r"
}
```

## Lizenz

Copyright © 2026 Torsten Klein

Dieses Projekt steht unter der **GNU Affero General Public License v3.0 oder
später** (AGPL-3.0-or-later), siehe [LICENSE](LICENSE): Wer den Code — auch als
Netzwerkdienst — weiterverwendet oder verändert, muss den Quellcode unter
derselben Lizenz offenlegen. Eine kommerzielle Lizenz ist auf Anfrage möglich.
