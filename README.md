# GO Browser

GO Browser is a native Java Android browser foundation designed for Android TV remotes and older Android devices.

> **Repository bootstrap:** this repository was empty before the initial project was created. The Android application ID `com.gobrowser.tv` is therefore a provisional new identity, not a recovered ID from an older APK. Change it before publishing if you need to match an existing app's signing identity and package name.

## Current foundation (0.2.0)

- Native Android application written in Java.
- Minimum Android version: API 23 (Android 6.0).
- TV launcher entry and touchscreen-optional manifest.
- Remote-friendly dashboard with YouTube, Google, Wikipedia and Netflix shortcuts.
- Address/search field with Google, Bing and DuckDuckGo provider switching.
- Basic URL-versus-search detection.
- Embedded Android WebView with JavaScript and DOM storage enabled.
- Back, forward, reload and home controls.
- HTML5 custom-view fullscreen callback handling.
- External intent handling for non-web URL schemes.
- **Smart remote pointer:** D-pad arrow keys move an on-screen pointer over webpages; OK/Enter sends touch down/up to the WebView; holding OK sends a longer press. Holding OK while moving the arrows sends touch-move events for drag/scroll gestures.
- **Pointer modes:** Smart Pointer, Trackpad (higher movement multiplier), D-pad Focus and Pointer Disabled.
- **Pointer Studio settings:** movement speed, repeat acceleration, size, arrow/ring/crosshair shape, cyan/white/yellow/green/orange colour, dark outline, soft shadow, click ripple and pointer visibility. Preferences persist between app launches.
- MENU / SETTINGS remote key opens pointer settings.
- GitHub Actions workflow assembles and uploads a debug APK.

## Remote pointer controls

1. Open any website.
2. In Smart Pointer or Trackpad mode, use remote UP / DOWN / LEFT / RIGHT to move the pointer.
3. Press OK, Enter or the remote's A button to click.
4. Hold OK to send a long touch. While holding it, use the arrows to drag or scroll.
5. Press MENU / SETTINGS or choose **Cursor settings** to customize the pointer.
6. Choose **D-pad Focus** or **Pointer Disabled** to return arrow keys to normal WebView navigation.

The pointer overlay is draw-only and does not intercept web content touches. Remote actions are dispatched to the WebView as Android touch events. Website hover-only interactions may not behave exactly like a hardware mouse, and actual TV remote testing is still required.

## Build

Open this repository in Android Studio with JDK 17 and Android SDK Platform 35 installed, or run:

```bash
gradle --no-daemon :app:assembleDebug
```

The project uses Android Gradle Plugin 8.6.1 and Gradle 8.7. The CI workflow installs the required Android SDK platform and builds the debug APK.

## Important limitations

This is an incremental browser foundation, **not** the complete browser specification. Multi-tab management, robust popup/window handling, downloads, APK installation, bookmarks/history UI, private tabs, a real ad-filter engine, phone pairing, automatic in-app updates and cloud browsing are not implemented yet.

The application ID is `com.gobrowser.tv`. No release signing key is included or generated. No signed release APK is published by this repository setup.

A successful CI build verifies compilation only. Real Android 6 and Xiaomi Mi TV testing is still required for device compatibility and remote-key mapping.
