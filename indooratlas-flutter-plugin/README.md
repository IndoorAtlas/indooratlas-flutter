# IndoorAtlas Flutter Plugin

IndoorAtlas SDK for flutter/dart, easily develop cross-platform applications

This project is still under development

Easily integratable UI and map widgets will come later

## Getting Started

Add this to your `pubspec.yaml`
```
indooratlas:
  git:
      url: https://github.com/IndoorAtlas/indooratlas-flutter.git
      ref: develop
      path: indooratlas-flutter-plugin
```

Requirements: Flutter 3.47 or newer (Dart 3.13+). The Android side is built with
AGP 9, Gradle 9 and Java 17, and needs `minSdk` 24 or higher (the Flutter default).

Declare these permissions in your app's `android/app/src/main/AndroidManifest.xml`:
`INTERNET`, `ACCESS_WIFI_STATE`, `CHANGE_WIFI_STATE`, `ACCESS_COARSE_LOCATION`,
`ACCESS_FINE_LOCATION` and `BLUETOOTH_SCAN`. See `example/` for a working app.

This project is a starting point for a Flutter
[plug-in package](https://flutter.dev/developing-packages/),
a specialized package that includes platform-specific implementation code for
Android and/or iOS.

For help getting started with Flutter, view our
[online documentation](https://flutter.dev/docs), which offers tutorials,
samples, guidance on mobile development, and a full API reference.

