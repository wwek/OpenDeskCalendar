# Implementation Review

Date: 2026-04-29

## PRD Coverage

Implemented:

- `Milestone 0`: native Android project, `minSdkVersion 16`, `MainActivity`, fullscreen keep-awake dashboard view.
- `Milestone 1`: landscape dashboard, large clock, date/week, weather area, month calendar, today highlight.
- `Milestone 2`: local lunar calendar, solar terms, festival labels, 2026 China holiday/workday badges, week-start setting.
- `Milestone 3`: Open-Meteo provider, 3-day forecast, cache fallback, recent error recording.
- `Milestone 4`: settings page, theme, seconds, city, Host/Key, keep-awake, exit confirmation.
- `Milestone 5`: boot receiver, Home activity, launcher chooser, backup launcher, long-press escape entries.
- `Milestone 6`: black-and-white/e-ink theme, hidden seconds behavior, lower refresh cadence.

Partially implemented:

- Custom weather Host/Key is supported as a direct Open-Meteo-compatible endpoint, but custom field mapping is not yet implemented.
- Almanac data is a deterministic local placeholder and needs an audited licensed data source before being treated as product data.
- Holiday data online update is not implemented.
- Indoor temperature/humidity, MQTT, Home Assistant, weather warnings, and night dimming are not implemented.

## Review Findings

- No blocking build or lint issues after `./gradlew --no-daemon :app:assembleDebug :app:lintDebug`.
- The app intentionally avoids AndroidX, Compose, Google Play Services, and minSdk-incompatible UI APIs.
- The launcher mode is explicit and recoverable: settings are reachable by long-pressing time, and system entries are reachable by long-pressing the Wi-Fi area.
- Weather failures keep cached data visible and write a redacted diagnostic entry.
- The 2026 holiday asset includes the expected Labor Day adjusted workday: `2026-05-09`.
- Lint has one remaining warning: `OldTargetApi` for `targetSdkVersion 31`. This is intentional for AGP 4.2/JDK 8 compatibility in the current low-version Android project setup.
- User-facing Android strings now live in resource files with Simplified Chinese defaults and `values-zh-rTW` Traditional Chinese overrides. Dynamic Chinese data from weather, lunar calendar, holiday labels, and almanac terms is converted for Traditional Chinese locales at display time.
- Release signing is implemented through local `local.properties` values or CI environment variables. `scripts/create-release-keystore.sh` creates a local ignored keystore, and `scripts/build-release.sh` builds and verifies the signed release APK.

## Residual Risks

- Real Android 4.1/4.2/4.4 device testing is still required. The project builds for API 16, but old-device Web/TLS behavior can still affect Open-Meteo requests.
- Canvas layout should be visually checked on the target devices, especially very small phones and e-ink screens.
- The lunar/solar-term implementation should be compared with a trusted almanac dataset before release.
- The generated local release keystore is suitable for local distribution/testing. A production release should back up the keystore and move secrets into a secure CI secret store.

## Verification

```text
./gradlew --no-daemon :app:assembleDebug :app:lintDebug
BUILD SUCCESSFUL in 29s

./scripts/build-release.sh
BUILD SUCCESSFUL in 28s
Signed release APK: app/build/outputs/apk/release/app-release.apk
```
