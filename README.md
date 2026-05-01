<p align="center">
  <img src="docs/assets/app-icon.png" width="112" alt="OpenDeskCalendar 图标">
</p>

<h1 align="center">OpenDeskCalendar</h1>

<p align="center">
  <a href="https://github.com/wwek/OpenDeskCalendar/actions/workflows/android-build.yml"><img src="https://img.shields.io/github/actions/workflow/status/wwek/OpenDeskCalendar/android-build.yml?branch=main&label=Android%20Build" alt="Android Build"></a>
  <img src="https://img.shields.io/badge/Android-4.1%2B%20(API%2016%2B)-2563eb" alt="Android 4.1+ API 16+">
  <img src="https://img.shields.io/badge/version-0.1.10--beta-0f766e" alt="version 0.1.10-beta">
  <img src="https://img.shields.io/badge/language-Java-f59e0b" alt="Java">
  <img src="https://img.shields.io/badge/license-Apache--2.0-111827" alt="Apache-2.0">
</p>

OpenDeskCalendar 是一个面向旧手机、旧平板和 Android 电纸书设备的开源桌面日历天气屏。

当前仓库已经实现 `v0.1.10-beta` 原生 Android 版本，需求来源见 `docs/open_desk_calendar_prd.md`。

## 界面预览

亮色、暗色、黑白、电纸书主题均支持竖屏和横屏。

<p align="center">
  <img src="docs/assets/readme-showcase.png" alt="OpenDeskCalendar 主题与横竖屏截图拼图">
</p>

## 当前范围

本 beta 已实现：

- 原生 Android 工程，最低支持 Android 4.1 Jelly Bean（API 16，`minSdkVersion 16`）。
- 全屏、常亮的桌面信息屏。
- 横屏优先主界面：大时钟、日期、农历、天气、三日预报、月历、Wi-Fi 状态和宜忌。
- 竖屏适配布局。
- 浅色、深色、黑白、电纸书主题。
- 本地农历、节气、传统节日，以及 2026 中国大陆休/班角标。
- Open-Meteo 天气源和本地缓存回退。
- 城市搜索和手动经纬度输入。
- HTTP / Home Assistant JSON 室内温湿度接入。
- 设置页：外观、天气、室内温湿度、日历、自启、桌面模式、诊断。
- 可选整点 / 半点语音播报，播报前带本地“叮咚”提示音，并支持夜间不播报和立即测试。
- 用户主动开启的开机自启。
- Home/Launcher Activity，以及长按进入设置和系统入口的防锁死路径。
- 夜间自动降亮度、退出确认开关和诊断日志导出。
- 默认开启的防烧屏轻微位移。
- 本机自签名 release 脚本和 GitHub Actions release 构建入口。
- 简体中文和繁体中文界面资源。

暂未完成：

- 直接 MQTT 客户端接入。
- 假日数据在线更新。
- 多城市轮播。
- 空气质量和天气预警。
- 自定义天气接口字段映射。

## 构建

要求：

- Android 4.1 Jelly Bean（API 16）或更高版本的设备 / 模拟器。
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

## Release 自签名

这是开源项目，仓库不会包含作者的 release keystore，作者也不会上传自己的签名私钥。自己编译或 fork 发布时，请生成并保管你自己的签名；同一个 APK 后续升级必须使用同一个 keystore。

本地构建带签名 APK：

```sh
./scripts/build-release.sh
```

首次运行如果没有签名配置，脚本会自动生成本机 keystore，并写入已忽略的 `local.properties`。也可以提前手动生成：

```sh
./scripts/create-release-keystore.sh
```

输出：

```text
app/build/outputs/apk/release/app-release.apk
```

默认 keystore 路径是 `.local/signing/opendeskcalendar-release.jks`。请备份这个文件和 `local.properties` 里的密码；丢失后将无法用同一签名身份更新已经安装的 release APK。

CI 或其他机器也可以用环境变量提供签名配置：

```sh
OPEN_DESK_CALENDAR_STORE_FILE=/path/to/release.jks
OPEN_DESK_CALENDAR_STORE_PASSWORD=...
OPEN_DESK_CALENDAR_KEY_ALIAS=opendeskcalendar
OPEN_DESK_CALENDAR_KEY_PASSWORD=...
```

如果你要让自己的 GitHub Actions 在推送 tag 后自动发布签名 APK，需要把自己的 keystore 配到自己仓库的 Secrets。先转成单行 base64：

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

也可以用脚本写入当前 GitHub 仓库的 Actions secrets：

```sh
gh auth login
./scripts/configure-github-release-secrets.sh
```

PR 和 `main` 分支只构建 debug APK。推送 `v*` tag 时，GitHub Actions 会读取这些 secrets 构建签名 release APK，上传 workflow artifact，并创建 / 更新 GitHub Release，附加 `OpenDeskCalendar-vX.Y.Z.apk`。

一键发布新版本：

```sh
./scripts/release-tag.sh 0.1.3-beta
```

这个脚本会：

1. 要求工作区干净。
2. 更新 `versionCode`、`versionName` 和 README 徽标。
3. 提交版本变更。
4. 本机构建并校验签名 APK。
5. 创建 `v0.1.10-beta` tag。
6. 推送当前分支和 tag，触发 GitHub Actions 自动构建签名包并发布 Release。

Tag 必须指向已提交的代码，所以实际顺序是“先提交，再打 tag，再 push”。脚本已经按这个顺序处理。

## 运行说明

- 默认城市是北京海淀。
- 默认天气源是 Open-Meteo，不需要 API Key；也可在设置里切换到和风天气。
- 使用和风天气时需要填写自己的 Base API 和 Key/JWT。
- 天气刷新失败时，主屏会继续显示缓存或内置回退数据。
- API Key 写入诊断错误前会被脱敏。
- 开机自启默认关闭，必须由用户主动开启。
- Home 桌面模式不会静默启用，必须由用户在 Android 系统桌面选择器中手动选择。
- 整点语音播报和半点语音播报默认关闭，可在设置页分别开启；整点播报“现在是 X 点整”，半点播报“现在是 X 点半”。
- 语音播报使用系统本地 TTS 引擎，不在应用内调用网络 TTS；播报前的“叮咚”提示音由应用本地生成。
- 夜间不播报默认覆盖 22:00 至 06:00，设置页的“立即测试”可马上触发提示音和语音播报。
- 界面语言跟随系统语言；`zh-TW` / `zh-HK` / `zh-MO` 会显示繁体中文。

## 数据来源

- 天气：Open-Meteo 公共 API，或用户配置的和风天气 API。
- 宜忌：基于 MIT 许可的 cnlunar 离线数据，覆盖 2026-2099，不构成建议。

## 免责声明

本项目是一个开源项目，作者和贡献者尽力让它稳定、准确、可用，但不承诺它一定适合你的设备、场景或用途，也不保证它始终可用、完全准确或没有错误。本免责声明是对项目开源许可证中无担保和责任限制条款的补充，不替代或削弱 LICENSE 文件中的条款。

OpenDeskCalendar 展示的天气、农历、节气、节假日、宜忌等信息仅用于桌面展示和传统文化参考，不构成气象、医疗、法律、出行、祭祀、婚丧嫁娶或其他现实决策建议。天气数据来自第三方服务或用户自行配置的接口，可能存在延迟、缺失或错误；农历与宜忌数据也可能因数据源、地区习惯和历法口径不同而存在差异。请勿将本应用作为唯一依据，重要事项请以官方发布、专业机构或实际情况为准。

本应用可能被用于旧手机、旧平板、电子纸设备或长期亮屏场景。长期通电、常亮显示、设备老化、散热条件、电池健康状态、充电器或线材质量等因素可能带来烧屏、发热、鼓包、续航下降、电池老化、设备损坏、数据丢失或其他硬件与安全风险。使用者应根据设备状况自行评估使用方式，并避免在无人看管、高温、潮湿、易燃、散热不良或供电不稳定的环境中长期运行。

在适用法律允许的最大范围内，使用者应自行判断并承担安装、运行、依赖或长期使用本应用及其展示信息所产生的风险。项目作者和贡献者不对因此造成的任何直接、间接、偶然、特殊、惩罚性或后果性损失承担责任，包括但不限于数据丢失、设备损坏、电池老化、人身伤害、财产损失、业务中断或其他损失。

## 许可证

见 `LICENSE`。
