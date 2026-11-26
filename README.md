# WebDock

An Android app manager for web apps. Add a URL once, then launch it full-screen in its own WebView with its own storage, settings, and recents entry.

WebDock is not a browser: there is no address bar and no free browsing.

## Status

Feature complete for the v1 scope. Working: the app list (grid/list, search, tag filters, pinning, long-press drag reorder for unpinned apps, delete, clear data), add/edit with live metadata preview and duplicate blocking, icon caching, per-app WebView profiles (storage isolation), per-app settings, global settings, and the in-app WebView runtime (navigation policy, recents entry, error pages, pull-to-refresh, file upload, camera/mic and location permissions, fullscreen video, JS dialogs, popups, and http(s)/blob/data downloads).

Out of scope for this version (removed from the spec): uptime checks, home-screen shortcuts, and JSON backup/restore.

## Releases

Every push to `main` runs the full check (`assembleDebug`, unit tests, `lintDebug`) and, when it passes, publishes a signed APK on the [Releases](../../releases) page. Versions are `0.1.<run number>`.

## Building locally

The local build uses the Android SDK Docker image `ghcr.io/cirruslabs/android-sdk:35`:

```sh
scripts/docker-gradle.sh            # runs scripts/check.sh
scripts/docker-gradle.sh ./gradlew assembleDebug
```

`scripts/check.sh` runs `./gradlew assembleDebug testDebugUnitTest lintDebug`.

Toolchain: Kotlin 2.2.21, AGP 8.13.2, Gradle 8.13, JDK 21 (Java/Kotlin target 17), compileSdk/targetSdk 35, minSdk 26.

## Manual testing

See [docs/manual-test.md](docs/manual-test.md) for the device checklist. The spec lives in [docs/spec.md](docs/spec.md), and [docs/webview-test.html](docs/webview-test.html) is a local page that exercises every WebView feature.
