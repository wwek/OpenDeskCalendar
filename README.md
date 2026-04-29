<p align="center">
  <img src="docs/assets/app-icon.png" width="112" alt="OpenDeskCalendar 图标">
</p>

<h1 align="center">OpenDeskCalendar</h1>

<p align="center">
  <a href="https://github.com/wwek/OpenDeskCalendar/actions/workflows/android-build.yml"><img src="https://img.shields.io/github/actions/workflow/status/wwek/OpenDeskCalendar/android-build.yml?branch=main&label=Android%20Build" alt="Android Build"></a>
  <img src="https://img.shields.io/badge/minSdk-16-2563eb" alt="minSdk 16">
  <img src="https://img.shields.io/badge/version-0.1.0--alpha-0f766e" alt="version 0.1.0-alpha">
  <img src="https://img.shields.io/badge/language-Java-f59e0b" alt="Java">
  <img src="https://img.shields.io/badge/license-Apache--2.0-111827" alt="Apache-2.0">
</p>

OpenDeskCalendar 是一个面向旧手机、旧平板和 Android 电纸书设备的开源桌面日历天气屏。

当前仓库已经实现 `v0.1.0-alpha` 原生 Android 版本，需求来源见 `docs/open_desk_calendar_prd.md`。

## 当前范围

本 alpha 已实现：

- 原生 Android 工程，`minSdkVersion 16`。
- 全屏、常亮的桌面信息屏。
- 横屏优先主界面：大时钟、日期、农历、天气、三日预报、月历、Wi-Fi 状态和宜忌。
- 竖屏适配布局。
- 浅色、深色、黑白、电纸书主题。
- 本地农历、节气、传统节日，以及 2026 中国大陆休/班角标。
- Open-Meteo 天气源和本地缓存回退。
- 城市搜索和手动经纬度输入。
- 设置页：外观、天气、日历、自启、桌面模式、诊断。
- 用户主动开启的开机自启。
- Home/Launcher Activity，以及长按进入设置和系统入口的防锁死路径。
- 简体中文和繁体中文界面资源。

暂未完成：

- 真实室内温湿度接入。
- MQTT / Home Assistant 接入。
- 假日数据在线更新。
- 自定义天气接口字段映射。
- 发布分发流水线。

## 构建

要求：

- 已安装 Android SDK。
- JDK 8。
- 首次构建需要网络访问，Gradle Wrapper 会下载 Gradle 6.9.4。

构建 debug APK：

```sh
./gradlew --no-daemon :app:assembleDebug
```

构建、安装并启动到当前模拟器：

```sh
./scripts/run-emulator.sh
```

如果没有正在运行的模拟器，脚本会尝试启动本机第一个 AVD。也可以显式指定设备：

```sh
./scripts/run-emulator.sh emulator-5554
```

输出：

```text
app/build/outputs/apk/debug/app-debug.apk
```

## Release 签名

生成本机 release keystore 并写入 `local.properties`：

```sh
./scripts/create-release-keystore.sh
```

构建并校验签名 release APK：

```sh
./scripts/build-release.sh
```

输出：

```text
app/build/outputs/apk/release/app-release.apk
```

签名配置不会提交到仓库。脚本会把 keystore 放在 `.local/signing/`，并把密码写入已忽略的 `local.properties`。请备份 keystore 和对应密码；丢失后将无法用同一签名身份更新已安装的 release APK。

CI 或其他机器也可以用环境变量提供签名配置：

```sh
OPEN_DESK_CALENDAR_STORE_FILE=/path/to/release.jks
OPEN_DESK_CALENDAR_STORE_PASSWORD=...
OPEN_DESK_CALENDAR_KEY_ALIAS=opendeskcalendar
OPEN_DESK_CALENDAR_KEY_PASSWORD=...
```

GitHub Actions 不提交 keystore 文件。把本机 release keystore 转成单行 base64：

```sh
base64 < .local/signing/opendeskcalendar-release.jks | tr -d '\n'
```

在 GitHub 仓库的 `Settings` -> `Secrets and variables` -> `Actions` 新增这些 Repository secrets：

```text
OPEN_DESK_CALENDAR_RELEASE_KEYSTORE_BASE64
OPEN_DESK_CALENDAR_STORE_PASSWORD
OPEN_DESK_CALENDAR_KEY_ALIAS
OPEN_DESK_CALENDAR_KEY_PASSWORD
```

PR 和 `main` 分支会构建 debug APK。手动触发 workflow 或推送 `v*` tag 时会用这些 secrets 构建签名 release APK，并上传为 workflow artifact。

## 运行说明

- 默认城市是北京海淀。
- 默认天气源是 Open-Meteo，不需要 API Key。
- 天气刷新失败时，主屏会继续显示缓存或内置回退数据。
- API Key 写入诊断错误前会被脱敏。
- 开机自启默认关闭，必须由用户主动开启。
- Home 桌面模式不会静默启用，必须由用户在 Android 系统桌面选择器中手动选择。
- 界面语言跟随系统语言；`zh-TW` / `zh-HK` / `zh-MO` 会显示繁体中文。

## 数据来源

- 天气：Open-Meteo 公共 API。
- 宜忌：本地确定性文化占位数据，不构成建议。

## 许可证

见 `LICENSE`。
