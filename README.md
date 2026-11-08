# GO Browser

A native Android TV browser built around a D-pad, not a fingertip.

GO Browser is a fast WebView browser for Android TV and older Android devices. It keeps the
parts of a browser people actually use on a television — search, tabs, bookmarks, history and
a remote-friendly pointer — and removes the phone-sized interface around them.

## What's in 0.4.0

- **A remote cursor that actually clicks.** Arrow keys move the pointer, OK sends a real
  touch down/up to the WebView, and holding OK turns the pointer into a drag/scroll gesture.
  Touch events are built with the correct source and tool type, which is what makes them work
  on TV WebView builds.
- **Search that works from the sofa.** Pressing OK on the search field opens the TV keyboard.
  `Enter` on a hardware keyboard submits, the on-screen **Go** button submits, and the search
  field is the first thing focused on the home screen.
- **A browser menu on MENU.** The remote's MENU key opens a large bottom bar with Back,
  Forward, Reload, Home, Search, Tabs, Save and Settings. Everything stays reachable even while
  the smart pointer is owning the arrow keys.
- **Real tabs.** Up to eight tabs, each keeping its last URL. Open the switcher from the tab
  button or the MENU bar, create a tab, switch, and close tabs with the D-pad.
- **Bookmarks and history.** A star in the browser bar saves the current page. Recent pages and
  saved bookmarks appear on the home screen and can be opened with one OK press.
- **Desktop mode.** Settings can ask websites for their full desktop layout; the choice is
  remembered between launches.
- **A 10-foot interface.** Warm graphite surfaces, one amber accent, focus states that grow and
  glow, and glyph-based shortcut tiles instead of fake brand logos.
- **A full-screen settings screen.** Desktop mode, history clearing, pointer mode, speed,
  acceleration, cursor size, shape, colour and feedback, all with TV-sized controls.
- **A crafted cursor.** Amber by default, with arrow, ring and crosshair shapes, an optional
  outline, shadow and click ripple.
- **A designed error page.** Connection failures stay inside the app and match the theme.

The browser is still a WebView browser at heart. Downloads and an ad-filter engine are not
implemented yet. Links that ask for a new window (target=_blank or window.open) are opened in
the current tab, which is the only place a 10-foot browser can sensibly put them. Sites that
block embedded WebViews (some Google sign-in flows, for example) may refuse to work; that is a
WebView limitation, not a styling problem.

## Remote controls

| Key | What it does |
| --- | --- |
| D-pad | Move the pointer (Smart/Trackpad) or move focus (D-pad mode) |
| OK / Enter | Click. Hold OK and move the arrows to drag or scroll |
| MENU | Open the browser action bar |
| BACK | Go back, close an overlay, or return home |
| SETTINGS | Open settings directly |
| SEARCH | Open the browser action bar |

### Searching

1. On the home screen the search field is already focused.
2. Press **OK** to open the TV keyboard.
3. Type a query and press the keyboard's **Go** action, or move to the **Go** button.
4. A word without a dot becomes a search; `example.com` becomes a website.

### Tabs

- Press the tab button in the browser bar, or choose **Tabs** from the MENU bar.
- **New tab** creates a blank tab and returns to the home screen.
- Selecting a tab restores its last URL.
- The × button closes a tab.

### Pointer notes

The pointer overlay is draw-only. Remote actions are dispatched to the WebView as Android
touch events with `SOURCE_TOUCHSCREEN` and `TOOL_TYPE_FINGER`. Hover-only interactions may not
behave exactly like a hardware mouse. If a site refuses synthetic input, switch the pointer to
D-pad Focus mode and navigate the page normally.

## Build

Open the repository in Android Studio with JDK 17 and Android SDK Platform 35 installed, or:

```
gradle --no-daemon :app:assembleDebug
```

The project uses Android Gradle Plugin 8.6.1 and Gradle 8.7. GitHub Actions automatically builds
the debug APK on pushes to `main` and uploads it as an artifact.

## Download the APK from GitHub

1. Open the repository's **Actions** tab.
2. Open the latest successful **GO Browser 0.4.0 Android build** run.
3. Scroll to **Artifacts** at the bottom of the run summary.
4. Download **`go-browser-0.4.0-debug`** and extract the downloaded ZIP.
5. Install `app-debug.apk` on your Android TV or Android device. This is a debug build; enable
   installation from unknown sources for the app you use to open it.

The APK is a CI artifact, not a signed Play Store release. It is available while the artifact is
within GitHub Actions' 30-day retention period.

## Project layout

```
app/src/main/java/com/gobrowser/tv/
  MainActivity.java      screen switching, WebView, pointer input, tabs
  RemotePointerView.java the draw-only cursor overlay
  PointerSettings.java   persisted cursor preferences
  BrowserStore.java      delimited history and bookmark storage
  ui/
    TvUi.java            shared spacing, type, focus and shape primitives
    HomeView.java        search, recent pages, shortcuts, bookmarks
    BrowserBar.java      compact address bar with tabs and bookmark controls
    QuickBar.java        MENU-key action bar
    TabSwitcher.java     full-screen tab switcher
    SettingsPanel.java   full-screen settings
```

Colours and dimensions live in `res/values/colors.xml` and `res/values/dimens.xml`. The palette
is intentionally narrow: warm near-black, warm white, muted stone, and amber.

## Licence

MIT. See [LICENSE](LICENSE) where present. Material icon paths are from the Material Design
icon set (Apache 2.0).
