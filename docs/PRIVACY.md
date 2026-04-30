# Privacy

OpenDeskCalendar is designed for local desk-display use.

## Data Collection

The app does not include accounts, ads, analytics, crash reporting, or default telemetry.

## Network Requests

The app sends network requests only for:

- Weather forecast refresh.
- City search.

Default endpoints are Open-Meteo APIs. Users can select another supported weather provider and configure a custom weather Base API and optional key in settings.

## Local Storage

The app stores the following on device:

- Display settings.
- City name and latitude/longitude.
- Optional weather provider, custom weather Base API, and key.
- Last successful weather response cache.
- Recent diagnostic errors.

API keys are redacted before diagnostic errors are stored.

## Permissions

- `INTERNET`: fetch weather and city search results.
- `ACCESS_NETWORK_STATE`: show online/offline state.
- `ACCESS_WIFI_STATE`: show Wi-Fi state.
- `RECEIVE_BOOT_COMPLETED`: optional boot autostart when enabled by the user.
- `WAKE_LOCK`: support always-on desk display behavior.

## User Control

Boot autostart is off by default. Home/Launcher mode is not set silently; the user must choose it in the Android system UI.
