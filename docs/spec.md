# WebDock: Android Web App Manager

> "WebDock" is a working name only. Ask me for the final app name and package name before writing any code (see section 12). Keep the name in one place (`app_name` string and a Gradle constant) so it is trivial to rename.

## 1. Purpose

A single-purpose Android app. I give it URLs of web apps that are hosted somewhere else, and it:

1. keeps them in a list,
2. launches each one full-screen in its own WebView,
3. keeps each one's data isolated and manageable.

It is an app manager/launcher for web apps. It is **not** a browser: there is no address bar and no free browsing.

Example URLs (use as manual test targets and as parsing fixtures):

- https://trade-journel.pages.dev/
- https://futures-playbook.pages.dev/

## 2. Goals and non-goals

**Goals**

- Add a URL and get a good-looking card (name, icon, theme color) with no manual work.
- Launch any app in one tap into a full-screen experience that feels like a native app.
- Each app has its own entry in Android's recents and can be pinned to the home screen.
- Per-app settings and per-app storage isolation.
- Back up and restore the app list as JSON.

**Non-goals for v1**

- Push notifications (WebView cannot receive web push; a Trusted Web Activity would be a separate future project).
- Accounts, cloud sync, analytics, or any network traffic other than to the user's own URLs.
- Ad blocking, extensions, tabs, or a general browsing UI.
- Tablet or foldable-specific layouts (must work, need not be optimized).
- Play Store release work.

## 3. Tech stack and constraints

- Kotlin 2.x, JDK 17, current stable Android Gradle Plugin and Gradle, Gradle version catalog (`gradle/libs.versions.toml`).
- `minSdk` 26. `compileSdk` and `targetSdk` = latest stable.
- Jetpack Compose with Material 3, dynamic color on Android 12+, light and dark themes.
- Navigation Compose for the manager UI.
- Room (with KSP) for persistence. DataStore Preferences for global settings.
- kotlinx.coroutines and kotlinx.serialization.
- OkHttp for network, Jsoup for HTML parsing, Coil for image loading.
- `androidx.webkit` (recent enough to include the Profile APIs) and `androidx.browser` (Custom Tabs).
- WorkManager, only in milestone 7 and only if periodic uptime checks are implemented.
- Tests: JUnit 4, kotlinx-coroutines-test, OkHttp MockWebServer.
- No DI framework. Use a small manual container created in the `Application` class.
- No Firebase, no analytics, no crash-reporting SDKs.
- Do not add any dependency that is not listed here without asking me first. Also ask me to confirm the exact toolchain and library versions before M0 (section 12 lists the kickoff questions).

## 4. Architecture

Single Gradle module, MVVM, unidirectional data flow (`StateFlow` for UI state, one-shot events through a channel).

```
app/src/main/java/<package>/
  App.kt                      Application + manual dependency container
  data/
    db/                       Room database, DAO, entities, type converters
    prefs/                    DataStore wrapper for global settings
    WebAppRepository.kt
    backup/                   Export/import (JSON, versioned)
    meta/                     Metadata fetching and parsing
    status/                   Uptime checks
  domain/
    UrlNormalizer.kt
    WebApp.kt, AppSettings.kt
  ui/
    theme/
    home/                     Grid/list, search, tag filter
    edit/                     Add and edit screen
    settings/                 Global settings
    components/
  webview/
    WebAppActivity.kt         Hosts one web app
    WebAppWebView.kt          WebView configuration
    clients/                  WebViewClient, WebChromeClient, download handling
    ProfileManager.kt         Wraps androidx.webkit Profile APIs
    NavigationPolicy.kt       Decides in-app vs. external for each URL
  shortcuts/                  Pinned and dynamic shortcut helpers
```

Keep `UrlNormalizer`, `NavigationPolicy`, the metadata parsers, and the backup serializer free of Android framework dependencies where practical, so they are plain JVM unit-testable.

## 5. Data model

### WebApp (Room entity)

| Field | Type | Notes |
|---|---|---|
| `id` | String | UUID, primary key, never changes |
| `url` | String | Normalized start URL. Unique: the same normalized URL cannot be added twice |
| `name` | String | Default from manifest `name`/`short_name`, then `<title>`, then host |
| `nameEdited` | Boolean | If true, metadata refresh never overwrites `name` |
| `iconUrl` | String? | Source URL of the chosen icon |
| `iconFile` | String? | Cached local file name under `filesDir/icons/` |
| `themeColor` | Int? | ARGB, from manifest or `<meta name="theme-color">` |
| `backgroundColor` | Int? | From manifest |
| `tags` | List<String> | Stored through a type converter |
| `pinned` | Boolean | Pinned apps sort first |
| `sortOrder` | Int | Manual order, used by drag-to-reorder |
| `createdAt`, `lastOpenedAt` | Long | Epoch millis |
| `settings` | AppSettings | Embedded, see below |

### AppSettings (embedded)

| Field | Default | Notes |
|---|---|---|
| `isolateStorage` | true | Use a dedicated WebView profile for this app |
| `desktopMode` | false | Desktop user agent, wide viewport |
| `zoomPercent` | 100 | Range 50 to 200 |
| `orientation` | `AUTO` | `AUTO`, `PORTRAIT`, `LANDSCAPE` |
| `keepScreenOn` | false | |
| `pullToRefresh` | true | |
| `javascriptEnabled` | true | |
| `thirdPartyCookies` | true | Some login flows need it |
| `linkPolicy` | `SAME_HOST_IN_APP` | `SAME_HOST_IN_APP`, `ALL_IN_APP`, `ALL_EXTERNAL` |
| `allowedHosts` | empty | Extra hosts that stay in-app (for example an auth domain) |
| `immersive` | false | Hide system bars |

### Global settings (DataStore)

Theme (system, light, dark), dynamic color on/off, default view (grid or list), default values for new apps' settings.

## 6. Screens

**Home**
- Grid or list toggle. Cards show icon, name, and host.
- Search by name, host, or tag. Filter chips for tags.
- Pinned apps first, then manual order. Long-press then drag to reorder.
- Long-press menu: Open, Edit, Pin/Unpin, Clear data, Delete.
- FAB to add.
- Empty state with a clear call to action.

**Add / Edit**
- A URL field. After the user finishes typing or pastes, fetch metadata and show a live preview card (icon, name, theme color).
- The user can override the name, pick tags, and open the per-app settings.
- Validation errors are shown inline: not a valid http(s) URL, or a URL that normalizes to one already in the list. A duplicate is a hard block: the same web app URL cannot be added more than once.
- Saving must work even if metadata fetching fails (use the host and a generated letter avatar).

**Web app screen** (`WebAppActivity`): see section 7.

**Settings**: global preferences, about, and a link to the project's README.

## 7. Web app runtime (WebAppActivity)

This is the hardest part. Each item below is a requirement, not a suggestion.

**Launching and recents**
- One `WebAppActivity` instance per web app, started with an explicit intent carrying the app id and a unique data URI (for example `webdock://app/<id>`).
- Declare `android:documentLaunchMode="intoExisting"` so each web app gets its own card in Android's system recents, and relaunching the same app resumes it.
- Verify on a device or emulator that two different apps show as two recents cards. If the manifest setting alone is not enough, add `FLAG_ACTIVITY_NEW_DOCUMENT` and `FLAG_ACTIVITY_MULTIPLE_TASK` when launching.
- Set the task description (label, icon, and color) so the recents card shows the app's own name and icon.

**Storage isolation (`ProfileManager`)**
- If `isolateStorage` is true and `WebViewFeature.MULTI_PROFILE` is supported, call `WebViewCompat.setProfile(webView, profileName)` **before any other call on that WebView** (including `loadUrl` and settings changes).
- `profileName` is derived from the app id and restricted to `[a-z0-9_]`.
- If the feature is unsupported, fall back to the default shared profile and show a one-time notice in the app's settings. Never crash.
- Deleting an app deletes its profile (cookies, web storage, cache, service workers). If the profile is currently in use, mark the deletion as pending and retry on next launch of the manager.
- "Clear data" in the per-app menu clears that profile's data without deleting the app.
- Note for the agent and for me: browsers already separate storage by origin, so two different sites cannot read each other's `localStorage` regardless. Isolation matters for clean per-app data wipes and for keeping third-party cookies and other shared profile state from leaking across apps. Because the same URL can only be added once, the duplicate-account scenario is out of scope.

**WebView settings**
- `domStorageEnabled` on. JavaScript per setting. File access and content access off. Mixed content never allowed. Safe Browsing on.
- `setWebContentsDebuggingEnabled(true)` in debug builds only, so I can inspect with `chrome://inspect`.
- Desktop mode: swap in a desktop Chrome user agent, enable wide viewport and overview mode.
- Zoom: apply `zoomPercent` through `textZoom`.

**Navigation policy (`NavigationPolicy`, pure and unit-tested)**
- Decide per URL: stay in the WebView, open in a Custom Tab, or hand to another app.
- `SAME_HOST_IN_APP` means the exact same host, plus any host in `allowedHosts`. **Do not** compare the last two domain labels: `pages.dev` is a public suffix, so `trade-journel.pages.dev` and `futures-playbook.pages.dev` are different sites and must be treated as such.
- Non-http(s) schemes (`mailto:`, `tel:`, `geo:`, `intent:`) go to `ACTION_VIEW` inside a try/catch for `ActivityNotFoundException`. Never load `file:` or `javascript:` URLs from a navigation.

**Back behavior**
- Back goes through the WebView's history first. When there is no more history, back finishes the activity and returns to wherever the user came from.
- An open fullscreen video or a popup window is dismissed before history is touched.

**Chrome client features** (each fails silently in a bare WebView, so each must be implemented)
- File upload: `onShowFileChooser` using the Activity Result APIs, supporting single and multiple selection and camera capture.
- Permissions: `onPermissionRequest` (camera, microphone) mapped to Android runtime permissions, and `onGeolocationPermissionsShowPrompt`. Ask once per origin per session and handle denial gracefully.
- Fullscreen video: `onShowCustomView` and `onHideCustomView`, with correct orientation and system bar handling.
- JavaScript dialogs: `onJsAlert`, `onJsConfirm`, `onJsPrompt` as Material dialogs.
- Popups: handle `window.open` and `target="_blank"` through `onCreateWindow`, routed through the same navigation policy.
- Progress bar for page loads. In debug builds, forward console messages to Logcat.

**Downloads**
- `DownloadListener` for http(s) URLs: hand off to `DownloadManager`, forwarding the cookies from the active profile and the user agent.
- `blob:` and `data:` URLs need a small, tightly scoped JavaScript bridge. Add it only when a download of that type is triggered, expose a single narrow method, and save through `MediaStore` Downloads. No storage permission should be required on API 29+.

**Errors**
- Main-frame load errors show a custom error page (Retry, Open in browser, Back to manager) instead of the default WebView error.
- SSL errors: cancel the load and show the error page. Never call `handler.proceed()`.
- HTTP 4xx/5xx on the main frame should not replace the page; surface a small snackbar.
- Offline: service-worker-backed PWAs must still load from cache. Do not pre-empt the WebView with an offline error screen.

**Lifecycle**
- `onPause`: flush the profile's cookies and call `webView.onPause()`. `onResume`: `webView.onResume()`.
- Save and restore WebView state in `onSaveInstanceState` so process death returns the user to where they were.
- In `onDestroy`: remove the WebView from its parent and call `destroy()`. No leaked activity references.

**Window chrome**
- Edge-to-edge. Status bar and navigation bar use `themeColor` when present, otherwise `backgroundColor`, otherwise the app theme. Choose light or dark system bar icons based on the color's luminance.
- Pull-to-refresh follows the per-app setting.
- No persistent toolbar. A small, translucent edge handle (can be disabled globally) opens a bottom sheet: Reload, Back to manager, Open in browser, Share URL, App settings, Clear data.

## 8. Metadata fetching

`MetadataFetcher` takes a URL and returns `Metadata(title, name, iconUrl, themeColor, backgroundColor)`. All parsing functions are pure and tested against saved HTML and manifest fixtures. Tests must not hit the real network.

1. Fetch the HTML with OkHttp: 8 second timeout, follow up to 5 redirects, cap the body at 2 MB, send a realistic mobile user agent.
2. Parse with Jsoup: `<title>`, `<meta name="theme-color">`, `<meta name="application-name">`, `<link rel="manifest">`, and all `<link rel="icon|shortcut icon|apple-touch-icon">` entries.
3. If a manifest is linked, fetch and parse it: `name`, `short_name`, `theme_color`, `background_color`, `icons` (with `sizes`, `type`, `purpose`), `start_url`.
4. Resolve every relative URL against the document or manifest URL, not the original input.
5. Icon choice: prefer manifest icons, then `apple-touch-icon`, then `rel=icon`. Prefer the smallest icon at or above 192 px, otherwise the largest available. Prefer PNG over SVG (Coil may not render every SVG consistently). Fallback: `/favicon.ico`. Last fallback: a generated letter avatar whose color is a stable hash of the host.
6. Download the chosen icon, downscale to at most 512 px, and store it in `filesDir/icons/<id>.png`.
7. Parse CSS color strings tolerantly (`#rgb`, `#rrggbb`, `#rrggbbaa`, named colors, `rgb()`); ignore invalid values rather than failing.
8. A failure at any step degrades to the next fallback and never blocks saving the app.

## 9. Milestones

Work in order. The milestones below describe build order only: work through the app as one continuous effort and do not stop for milestone sign-off. Before starting a batch of work, ask me every decision it needs (section 12). After each merge to `main`, update `README.md` (what works, how to run it) and tell me anything surprising directly in chat.

**M0: Scaffold and toolchain**
- Gradle project, version catalog, Compose + Material 3 theme, `App` container, empty Home screen with an empty state.
- `scripts/check.sh` runs `./gradlew assembleDebug testDebugUnitTest lintDebug`.
- Done when: `scripts/check.sh` passes and the app launches on a device or emulator.

**M1: Data and Home**
- `UrlNormalizer` (see section 10), Room database and DAO, `WebAppRepository`.
- Home grid and list with letter-avatar icons, search, tags and filter chips, pin, delete, drag-to-reorder.
- Add/Edit screen with validation but no metadata yet (name defaults to the host).
- Done when: I can add, edit, reorder, pin, and delete entries, duplicate URLs are rejected, they persist across app restarts, and `UrlNormalizer` tests pass.

**M2: Metadata**
- `MetadataFetcher`, parsers, icon caching, live preview on the Add/Edit screen, "refresh metadata" action per app.
- Done when: both example URLs produce a title and icon in the preview and on their cards, a bad or offline URL still saves cleanly, and parser tests pass against fixtures.

**M3: WebView runtime, core**
- `WebAppActivity`, per-app recents entries, back behavior, navigation policy, error page, lifecycle and state restore, pull-to-refresh, edge-to-edge with theme color.
- Done when: both example apps load and are usable, back and recents behave as in section 7, killing the process and reopening restores the page, and `NavigationPolicy` tests pass (including the `pages.dev` case).

**M4: WebView feature parity**
- File upload, downloads (including blob), permissions, geolocation, fullscreen video, JS dialogs, popups.
- Done when: each feature works on a small local test page that I can host (include `docs/webview-test.html` with a button for each feature) and none of them fails silently.

**M5: Isolation and per-app settings**
- `ProfileManager`, per-app settings screen, Clear data, profile deletion on app delete, global defaults for new apps.
- Done when: Clear data wipes only that app's profile, deleting an app removes its profile (retried later if it was in use), and all settings in section 5 take effect.

## 10. Testing

Unit tests (JVM), required:

- `UrlNormalizer`: trims whitespace, adds `https://` when no scheme is given, lowercases scheme and host, rejects non-http(s) schemes and empty hosts, handles IDN hosts, keeps path and query, treats `https://a.dev` and `https://a.dev/` consistently, and detects a duplicate when two inputs normalize to the same URL.
- `NavigationPolicy`: same host, allowed hosts, different subdomains, sibling `pages.dev` hosts, `mailto:`/`tel:`/`intent:`, and `javascript:`/`file:` rejection, for each `linkPolicy`.
- Metadata parsers: fixtures for a page with a full manifest, a page with only a `<title>`, a page with only a favicon, relative icon paths, and broken HTML.
- Color parsing and icon selection logic.

Instrumented tests (optional, require an emulator, ask me before adding them): Room DAO behavior and a Compose smoke test for Home.

Manual checklist (agent writes this into `docs/manual-test.md`, I run it before calling a milestone done): both example URLs load, back button behavior, recents cards, rotation, process death restore, offline launch, file upload, a download, camera permission, fullscreen video, adding a duplicate URL is rejected.

## 11. Definition of done (every merge)

- `./gradlew assembleDebug`, `./gradlew testDebugUnitTest`, and `./gradlew lintDebug` all pass.
- No new compiler warnings introduced without justification.
- `README.md` updated.
- Every decision made during the milestone was put to me and answered by me (section 12).
- One commit per feature (more are fine) with a clear message; merge to `main` only when the check is green.

## 12. Working agreement and decision policy

**I make every decision. The agent makes none.**

- Read this whole file first.
- Do not create `AGENTS.md`, `DECISIONS.md`, or any other file that logs decisions or holds instructions for agents. The only documentation files in the repo are `README.md` and `docs/manual-test.md`.
- Anything this spec does not explicitly state is a decision, and decisions are mine. That includes, without being limited to: names (app, package), toolchain, dependency and version choices, library or architecture alternatives, UI and UX details (layout, spacing, copy, colors, icons, animation), behavior in edge cases the spec does not cover, any point where two reasonable approaches exist, scope changes, skipping or reordering work, and how to proceed when a requirement turns out to be hard or impossible.
- If you are unsure whether something is a decision, treat it as one and ask.
- Never pick a "reasonable default" and keep going. Never deviate from this spec silently.

**How to ask**

1. Stop and state the decision in one or two sentences.
2. Give 2 to 4 options with their trade-offs, and say which one you would recommend and why.
3. Wait for my answer before doing the work that depends on it. Independent work may continue while you wait.
4. Batch questions: before starting each batch of work, ask everything you already know that batch needs, then ask follow-ups as they come up.
5. Do not weaken a requirement to make a test pass. If a requirement cannot be met, tell me why and ask how to proceed.
6. My answers live in the conversation and in commit messages, not in decision-log files.

**Kickoff questions (ask all of these before writing any code)**

1. Final app name and `applicationId` / package name.
2. Exact versions for Kotlin, Android Gradle Plugin, Gradle, JDK, `compileSdk`/`targetSdk`, the Compose BOM, and each library in section 3 (propose them, I confirm).
3. Theme fallback seed color for devices without dynamic color, and what to use for the launcher icon (a design from me or a simple placeholder).
4. Whether an emulator or a physical device is connected over `adb`, so you know whether you can install and verify on it.

**Working practice**

- Work in small, verifiable steps. Run the build and tests after each meaningful change instead of at the end.
- If a device or emulator is available over `adb`, install the debug APK and verify behavior there, including screenshots, instead of assuming it works.
- No features beyond this spec without asking.
