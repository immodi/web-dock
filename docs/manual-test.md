# WebDock manual test checklist

Run this on a physical device or emulator after installing the APK from the GitHub release. Check each item and note failures.

## Install and launch

- [ ] Install the release APK on a device that has no previous WebDock install.
- [ ] Newer releases install over older ones without uninstalling (stable signing key).
- [ ] App launches without a crash and shows the empty state ("No web apps yet").

## Home

- [ ] Add a web app and it appears in the grid/list.
- [ ] Grid/list toggle takes effect and survives an app restart.
- [ ] Search filters by name, host, and tag.
- [ ] Tag filter chips filter the list.
- [ ] Pin puts an app first; unpin moves it back to the unpinned group.
- [ ] Long-press and drag reorders unpinned apps; pinned apps stay at the top and cannot be dragged.
- [ ] Reorder is disabled while searching or filtering; the order survives an app restart.
- [ ] Long-press menu: Open, Edit, Refresh metadata, Pin/Unpin, Clear data, Delete all work.
- [ ] Empty state returns after deleting the last app.

## Add / edit

- [ ] Pasting a URL shows a live preview with icon, name, and theme color.
- [ ] A bad or offline URL still saves, using the host and a letter avatar.
- [ ] Adding a URL that normalizes to an existing one is rejected with an inline error.
- [ ] Editing keeps a manually overridden name when metadata refreshes.
- [ ] Tapping Save with an invalid URL shows an inline error and does not save.
- [ ] Opening App settings from the Add screen and changing values applies them to the new app.
- [ ] New apps start with the values configured under Settings → Defaults for new apps.

## Settings

- [ ] The Settings icon opens the settings screen.
- [ ] Theme (System/Light/Dark) changes immediately.
- [ ] Dynamic color follows the global setting on Android 12+.
- [ ] Default view changes the Home layout.
- [ ] Turning off "Show edge handle" removes the handle from the web app screen.
- [ ] Settings → Defaults for new apps persists across restarts.
- [ ] Project README opens in a browser.
- [ ] App settings from the Edit screen and from the web app bottom sheet both persist and take effect the next time the app is opened.

## Web app runtime

- [ ] Both example URLs load and are usable.
- [ ] Each web app gets its own recents card with its own name, icon, and color.
- [ ] Relaunching a web app from recents resumes where it was.
- [ ] Back goes through WebView history, then returns to the manager.
- [ ] Killing the process and reopening restores the page.
- [ ] Offline launch of an installed PWA loads from cache.
- [ ] A main-frame load error shows the custom error page (Retry, Open in browser, Back to manager).
- [ ] An SSL error cancels the load; the error page appears.
- [ ] A 4xx/5xx main-frame response shows a snackbar and does not replace the page.
- [ ] Edge handle opens the bottom sheet; Reload, Back to manager, Open in browser, Share URL, App settings, and Clear data all work.
- [ ] Pull-to-refresh works when enabled for the app.
- [ ] System bars use the app's theme color and readable icon contrast.
- [ ] Changing orientation or immersive mode in app settings takes effect after the app reopens.

## Navigation policy

- [ ] Same-host links stay in the WebView.
- [ ] Links to other hosts follow the configured link policy (same host in app, all in app, or all external).
- [ ] Allowed hosts stay in the app.
- [ ] Two sibling `pages.dev` apps are treated as different sites.
- [ ] `mailto:`, `tel:`, and `geo:` hand off without crashing when no app handles them.
- [ ] `file:` and `javascript:` navigations never load.

## WebView features (use `docs/webview-test.html` served over http or https)

- [ ] File upload works (single, multiple, and camera capture).
- [ ] Camera and microphone permission prompts appear once per origin per session; denial is handled.
- [ ] Geolocation prompt works.
- [ ] Fullscreen video enters and exits correctly, with system bars restored.
- [ ] JavaScript alert, confirm, and prompt appear as Material dialogs.
- [ ] `window.open` / `target="_blank"` routes through the navigation policy.
- [ ] Progress bar reflects page loads; debug console messages appear in Logcat.
- [ ] A normal http(s) download succeeds, with cookies and user agent forwarded.
- [ ] A `blob:` download saves through MediaStore without a storage permission prompt on Android 10+.
- [ ] A `data:` download saves through MediaStore.
- [ ] On Android 8/9, the storage permission is requested once for downloads.

## Isolation and clear data

- [ ] Clearing data for an isolated app signs it out without touching other apps.
- [ ] Clearing data for an app with isolation off warns that all apps are affected, then signs out all apps.
- [ ] Deleting an app removes its profile; if the profile was in use, it is removed on the next manager launch.
- [ ] On a WebView without multi-profile support, app settings show the sharing notice and Clear data warns about shared storage.
- [ ] Desktop mode, zoom, keep screen on, pull-to-refresh, JavaScript, third-party cookies, link policy, allowed hosts, and immersive mode all take effect.
