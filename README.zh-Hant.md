<p align="center">
  <img src="docs/assets/app-icon.png" width="112" alt="OpenDeskCalendar 圖示">
</p>

<h1 align="center">OpenDeskCalendar</h1>

<p align="center">
  <a href="https://github.com/wwek/OpenDeskCalendar/actions/workflows/android-build.yml"><img src="https://img.shields.io/github/actions/workflow/status/wwek/OpenDeskCalendar/android-build.yml?branch=main&label=Android%20Build" alt="Android Build"></a>
  <img src="https://img.shields.io/badge/Android-4.1%2B%20(API%2016%2B)-2563eb" alt="Android 4.1+ API 16+">
  <img src="https://img.shields.io/badge/version-0.1.1--alpha-0f766e" alt="version 0.1.1-alpha">
  <img src="https://img.shields.io/badge/language-Java-f59e0b" alt="Java">
  <img src="https://img.shields.io/badge/license-Apache--2.0-111827" alt="Apache-2.0">
</p>

OpenDeskCalendar 是一個面向舊手機、舊平板和 Android 電子紙裝置的開源桌面日曆天氣屏。

目前倉庫已經實作 `v0.1.1-alpha` 原生 Android 版本，需求來源見 `docs/open_desk_calendar_prd.md`。

## 目前範圍

本 alpha 已實作：

- 原生 Android 工程，最低支援 Android 4.1 Jelly Bean（API 16，`minSdkVersion 16`）。
- 全螢幕、常亮的桌面資訊屏。
- 橫屏優先主介面：大時鐘、日期、農曆、天氣、三日預報、月曆、Wi-Fi 狀態和宜忌。
- 直向螢幕適配版面。
- 淺色、深色、黑白、電子紙主題。
- 本地農曆、節氣、傳統節日，以及 2026 中國大陸休/班角標。
- Open-Meteo 天氣源和本地快取回退。
- 城市搜尋和手動經緯度輸入。
- HTTP / Home Assistant JSON 室內溫濕度接入。
- 設定頁：外觀、天氣、室內溫濕度、日曆、自動啟動、桌面模式、診斷。
- 使用者主動開啟的開機自動啟動。
- Home/Launcher Activity，以及長按進入設定和系統入口的防鎖死路徑。
- 夜間自動降亮度、退出確認開關和診斷日誌匯出。
- 本機 release 簽名腳本和 GitHub Actions release 建置入口。
- 簡體中文和繁體中文介面資源。

尚未完成：

- 直接 MQTT 客戶端接入。
- 假日資料線上更新。
- 多城市輪播。
- 空氣品質和天氣預警。
- 自訂天氣介面欄位映射。

## 建置

需求：

- Android 4.1 Jelly Bean（API 16）或更高版本的裝置 / 模擬器。
- 已安裝 Android SDK。
- JDK 8。
- 首次建置需要網路存取，Gradle Wrapper 會下載 Gradle 6.9.4。

建置 debug APK：

```sh
./gradlew --no-daemon :app:assembleDebug
```

建置、安裝並啟動到目前模擬器：

```sh
./scripts/run-emulator.sh
```

如果沒有正在執行的模擬器，腳本會嘗試啟動本機第一個 AVD。也可以明確指定裝置：

```sh
./scripts/run-emulator.sh emulator-5554
```

輸出：

```text
app/build/outputs/apk/debug/app-debug.apk
```

## Release 簽名

產生本機 release keystore 並寫入 `local.properties`：

```sh
./scripts/create-release-keystore.sh
```

建置並驗證簽名 release APK：

```sh
./scripts/build-release.sh
```

輸出：

```text
app/build/outputs/apk/release/app-release.apk
```

簽名設定不會提交到倉庫。腳本會把 keystore 放在 `.local/signing/`，並把密碼寫入已忽略的 `local.properties`。請備份 keystore 和對應密碼；遺失後將無法用同一簽名身分更新已安裝的 release APK。

CI 或其他機器也可以用環境變數提供簽名設定：

```sh
OPEN_DESK_CALENDAR_STORE_FILE=/path/to/release.jks
OPEN_DESK_CALENDAR_STORE_PASSWORD=...
OPEN_DESK_CALENDAR_KEY_ALIAS=opendeskcalendar
OPEN_DESK_CALENDAR_KEY_PASSWORD=...
```

GitHub Actions 不提交 keystore 檔案。把本機 release keystore 轉成單行 base64：

```sh
base64 < .local/signing/opendeskcalendar-release.jks | tr -d '\n'
```

在 GitHub 倉庫的 `Settings` -> `Secrets and variables` -> `Actions` 新增這些 Repository secrets：

```text
OPEN_DESK_CALENDAR_RELEASE_KEYSTORE_BASE64
OPEN_DESK_CALENDAR_STORE_PASSWORD
OPEN_DESK_CALENDAR_KEY_ALIAS
OPEN_DESK_CALENDAR_KEY_PASSWORD
```

PR 和 `main` 分支會建置 debug APK。手動觸發 workflow 或推送 `v*` tag 時會用這些 secrets 建置簽名 release APK，並上傳為 workflow artifact。

## 執行說明

- 預設城市是北京海淀。
- 預設天氣源是 Open-Meteo，不需要 API Key。
- 天氣更新失敗時，主屏會繼續顯示快取或內建回退資料。
- API Key 寫入診斷錯誤前會被脫敏。
- 開機自動啟動預設關閉，必須由使用者主動開啟。
- Home 桌面模式不會靜默啟用，必須由使用者在 Android 系統桌面選擇器中手動選擇。
- 介面語言跟隨系統語言；`zh-TW` / `zh-HK` / `zh-MO` 會顯示繁體中文。

## 資料來源

- 天氣：Open-Meteo 公共 API。
- 宜忌：本地確定性文化占位資料，不構成建議。

## 授權

見 `LICENSE`。
