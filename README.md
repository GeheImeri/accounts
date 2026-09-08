# Accounts — 个人离线记账 App（Android · 全本地 · 零联网）

> 为自己做的记账工具：记账快、统计顺、界面干净、数据全在本地、永远免费。
> 当前设计：**云轨 · 漫游** —— 云白、浅蓝、雾青与航线式交互；预览见 `mockups/themes-light/c-cloud.html`

## 当前状态

- [x] 需求文档：`docs/01-需求文档.md`（功能/数据模型/统计口径/里程碑）
- [x] UI 草图：`docs/02-UI草图.md` + `mockups/`（7 屏可视化 Demo，双击 HTML 即看）
- [x] **视觉定稿并落地**：云轨 · 漫游（原生 Compose v0.3.1）
- [x] **Android 项目骨架**（本目录即 Gradle 工程根）：
  Kotlin + Jetpack Compose（Material3）+ Room；云轨亮色 / 夜雾蓝双模式主题；
  记一笔 / 明细 / 统计 / 设置 / 分类管理 / 账户管理 / 主题 已搭通；
  无 INTERNET 权限
- [x] GitHub Actions 云构建配置：`.github/workflows/build-apk.yml`
- [x] GitHub Actions 云端编译已跑通
- [x] v0.3：首页分类/账户双轨选择，账户图标/颜色/名称/类型可编辑，收入与支出共用默认账户；明细副标题仅显示账户

## 目录结构

| 路径 | 内容 |
|---|---|
| `docs/` | 需求 / UI 草图 / 历史视觉规范 / 构建与签名指南 |
| `mockups/themes-light/` | 五版浅色主题预览（云轨方案已落地） |
| `app/src/main/java/com/accounts/app/` | Kotlin 源码（data / ui / util） |
| `.github/workflows/build-apk.yml` | push 到 main 自动出 APK |
| `scripts/push-to-github.ps1` | 一键初始化并推送 GitHub |

## 快速开始（云端，本机零安装）

1. 先在 GitHub 网页建一个**空仓库**（如 `accounts`，不要勾选初始化文件）
2. 一键推送（PowerShell 会自动 init/commit/push）：
   ```powershell
   cd "F:\deepseek harness\jianji"
   .\scripts\push-to-github.ps1 -RepoUrl https://github.com/你的用户名/accounts.git
   ```
   手动步骤见 `docs/04-构建与签名指南.md` 方式 A
3. Actions 自动构建 → Artifacts 下载 `app-release.apk`
4. 手机下载安装（允许"安装未知来源应用"）
5. 想覆盖升级不丢数据：按指南第 3 节配置一次 release 签名 Secret

本地开发（可选）：Android Studio 打开本目录即可 Sync 构建。

## 隐私承诺

- Manifest **未声明 INTERNET 权限** → 系统层面无法联网
- 数据仅存本机 Room(SQLite) `jianji.db`；无账号、无广告、无 SDK

## 技术栈

Kotlin 1.9.24 · Compose BOM 2024.06 (Material3) · Room 2.6.1 (KAPT) ·
AGP 8.5.2 · Gradle 8.9 · minSdk 26 / target 34
应用名 Accounts · 包名 `com.accounts.app`
