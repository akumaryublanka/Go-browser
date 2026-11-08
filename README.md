# GO Browser

GO Browser is a native Android browser foundation designed for Android TV remotes and older Android devices.

> **Repository bootstrap:** this repository was empty before the initial project was created. The Android application ID `com.gobrowser.tv` is therefore a provisional new identity, not a recovered ID from an older APK. Change it before publishing if you need to match an existing app's signing identity and package name.

## Current foundation (0.1.0)

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
- GitHub Actions workflow to assemble and upload a debug APK.

## Build

Open this repository in Android Studio with JDK 17 and Android SDK Platform 35 installed, or run:

```bash
gradle --no-daemon :app:assembleDebug
```

The project uses Android Gradle Plugin 8.6.1 and Gradle 8.7. The CI workflow installs the required Android SDK platform and builds the debug APK.

## Important limitations

This is an initial working foundation, **not** the complete browser specification. Multi-tab management, popup/window handling, downloads, APK installation, bookmarks/history UI, private tabs, a real ad-filter engine, custom cursor modes, phone pairing, automatic in-app updates and cloud browsing are not implemented yet.

The application ID is `com.gobrowser.tv` and the first version is `0.1.0` / versionCode 1. No release signing key is included or generated. No signed release APK is published by this repository setup.

A successful CI build will only verify compilation. Real Android 6 and Xiaomi Mi TV testing is still required for device compatibility.
