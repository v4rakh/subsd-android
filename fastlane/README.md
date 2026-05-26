fastlane documentation
----

# Installation

Make sure you have the latest version of the Xcode command line tools installed:

```sh
xcode-select --install
```

For _fastlane_ installation instructions, see [Installing _fastlane_](https://docs.fastlane.tools/#installing-fastlane)

# Available Actions

## Android

### android build_debug

```sh
[bundle exec] fastlane android build_debug
```

Build debug APK

### android build

```sh
[bundle exec] fastlane android build
```

Build release AAB (signed, for Play Store)

### android build_apk

```sh
[bundle exec] fastlane android build_apk
```

Build release APKs (signed, for sideloading)

### android alpha

```sh
[bundle exec] fastlane android alpha
```

Deploy a new version to the Google Play as Alpha

### android beta

```sh
[bundle exec] fastlane android beta
```

Deploy a new version to the Google Play as Beta

### android deploy

```sh
[bundle exec] fastlane android deploy
```

Deploy a new version to the Google Play

----

This README.md is auto-generated and will be re-generated every time [_fastlane_](https://fastlane.tools) is run.

More information about _fastlane_ can be found on [fastlane.tools](https://fastlane.tools).

The documentation of _fastlane_ can be found on [docs.fastlane.tools](https://docs.fastlane.tools).
