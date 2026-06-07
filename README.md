# README

An Android client for [subsd](https://git.myservermanager.com/varakh/subsd), a self-hosted music
server daemon from Subsonic-compatible libraries and a satellite playback system.

See [screenshots](./fastlane/metadata/android/en-US/images/phoneScreenshots/).

The main git repository is hosted
at [https://git.myservermanager.com/varakh/subsd-android](https://git.myservermanager.com/varakh/subsd-android).
Other repositories are mirrors and pull requests, issues, and planning are managed there.

## Features

- Browse your music library by artist and album
- Search across artists, albums, and songs
- Manage and reorder a playback queue
- Browse and play playlists
- Control and monitor satellite devices connected to the subsd daemon
- Local audio playback via ExoPlayer with a media notification and lock-screen controls
- **Satellite mode**: the app registers itself as a satellite over gRPC, receives playback commands
  from the server, and reports player state back in real time

## Requirements

- Android 8.0 (API 26) or higher
- A running [subsd](https://git.myservermanager.com/varakh/subsd) instance reachable from the device

## Setup

On first launch you will be taken to the setup screen. Fill in:

| Field          | Required       | Description                                                                   |
| -------------- | -------------- | ----------------------------------------------------------------------------- |
| Server URL     | Yes            | Base HTTP URL of the subsd daemon, e.g. `http://192.168.1.100:8080`           |
| HTTP token     | No             | API token if authentication is enabled on the server                          |
| Satellite mode | No             | Enable to act as a local audio output device                                  |
| Device name    | No (satellite) | Name shown in the daemon UI; defaults to the device model                     |
| gRPC address   | No (satellite) | `host:port` for the gRPC endpoint; defaults to the server host on port `9090` |
| gRPC token     | No (satellite) | Shared secret sent as `x-subsd-token` metadata                                |
| TLS            | No (satellite) | Enable TLS for the gRPC connection                                            |

Cleartext HTTP is permitted so that self-hosted instances on a local network work without a TLS
certificate.

## Building

> Prefer `fastlane` for related tasks, but you can also invoke `gradle` manually.

```shell
# gradle
./gradlew assembleDebug

# fastlane
fastlane android build_debug
```

1. A **release** build requires signing credentials provided via environment variables.
2. Make sure version is properly set in `app/build.gradle.kts`

```shell
export SUBSD_KEYSTORE_FILE=/path/to/keystore.jks
export SUBSD_KEYSTORE_PASSWORD=...
export SUBSD_KEY_ALIAS=...
export SUBSD_KEY_PASSWORD=...

# gradle
./gradlew assembleRelease

# fastlane
fastlane android build_apk
```

There are other `fastlane` tasks which could be used for Play Store publication.

When release is done, bump version properly in `app/build.gradle.kts` for the next version.

## Architecture

- **UI**: Jetpack Compose with Material 3, single-activity, tab-based navigation
- **Networking**: OkHttp for the REST/WebSocket API; gRPC-Kotlin over OkHttp for the satellite
  protocol
- **Playback**: ExoPlayer (`media3-exoplayer`) running in a foreground `PlaybackService` with a
  `MediaSession`
- **Satellite protocol**: bidirectional gRPC stream defined in `satellite.proto`; the app sends
  registration, heartbeats, player state, and track-ended events upstream and receives playback
  commands downstream
- **Image loading**: Coil 3, sharing the same OkHttp client as the API so session cookies are
  forwarded automatically for cover art requests
- **Preferences**: Jetpack DataStore

## License

See the [subsd](https://git.myservermanager.com/varakh/subsd) repository for licensing information.
