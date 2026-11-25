# WebDock

An Android app manager for web apps. Add a URL once, then launch it full-screen in its own WebView with its own storage, settings, and recents entry.

WebDock is not a browser: there is no address bar and no free browsing.

## Status

Work in progress. Working: the app list (grid/list, search, tag filters, pinning, long-press drag reorder for unpinned apps, delete), add/edit with URL validation and duplicate blocking, and a basic in-app WebView screen. Metadata, storage isolation, per-app settings, and the full WebView feature set are still being built.

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

See [docs/manual-test.md](docs/manual-test.md) for the device checklist. The spec lives in [docs/spec.md](docs/spec.md).
