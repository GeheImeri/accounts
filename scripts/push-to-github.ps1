# ============================================================
# Accounts · 一键初始化 Git 仓库并推送到 GitHub
# 用法（在 PowerShell 中）：
#   cd F:\deepseek harness\jianji
#   .\scripts\push-to-github.ps1                       # 按提示输入仓库地址
#   .\scripts\push-to-github.ps1 -RepoUrl https://github.com/你的用户名/accounts.git
# ============================================================
param(
    [string]$RepoUrl = ""
)

$ErrorActionPreference = 'Stop'
$root = Split-Path -Parent $PSScriptRoot   # 脚本在 jianji/scripts 下，父目录即工程根
Set-Location $root

Write-Host ""
Write-Host "== Accounts 推送助手 ==" -ForegroundColor Cyan
Write-Host "工程根：$root"

# 1) git 是否安装
if (-not (Get-Command git -ErrorAction SilentlyContinue)) {
    Write-Host "[X] 未检测到 git，请先安装：https://git-scm.com/download/win" -ForegroundColor Red
    exit 1
}

# 2) git 身份（仅当未全局配置时询问，写入本仓库 local）
$userName  = git config user.name
$userEmail = git config user.email
if (-not $userName)  { $userName  = Read-Host "   Git 用户名（用于提交记录）" }
if (-not $userEmail) { $userEmail = Read-Host "   Git 邮箱" }
if (-not $userName  -or -not $userEmail) { Write-Host "[X] 用户名/邮箱不能为空" -ForegroundColor Red; exit 1 }
git config user.name  $userName
git config user.email $userEmail

# 3) 仓库地址
if (-not $RepoUrl) {
    $RepoUrl = Read-Host "   GitHub 仓库地址（形如 https://github.com/你的用户名/accounts.git；没有就先在网页建空仓库）"
}
if ($RepoUrl -notmatch '^https?://.*\.git$' -and $RepoUrl -notmatch '^git@') {
    Write-Host "[!] 地址格式可疑，请确认形如 https://github.com/user/accounts.git" -ForegroundColor Yellow
}

# 4) 初始化与提交
if (-not (Test-Path "$root\.git")) {
    git init
    Write-Host "[*] 已 git init" -ForegroundColor Green
}
git symbolic-ref HEAD refs/heads/main 2>$null
git add -A
git commit -m "Accounts v0.1.0: 晨雾主题骨架（Compose+Room 三主屏+设置子页）" 2>$null
if ($LASTEXITCODE -ne 0) {
    # 无改动可提交（可能重复执行）
    Write-Host "[*] 无新增改动可提交（已是最新）" -ForegroundColor Yellow
}

# 5) remote + push
if (git remote | Select-String '^origin$') {
    git remote set-url origin $RepoUrl
} else {
    git remote add origin $RepoUrl
}
git push -u origin main
if ($LASTEXITCODE -ne 0) {
    Write-Host "[X] push 失败。常见原因：仓库需先网页建好 / 需要认证（HTTPS 弹窗或 PAT）/ 网络" -ForegroundColor Red
    Write-Host "    认证可参考：https://docs.github.com/zh/authentication/keeping-your-account-and-data-secure/managing-your-personal-access-tokens"
    exit 1
}

Write-Host ""
Write-Host "== 推送成功！接下来 ==" -ForegroundColor Cyan
Write-Host "  1. 打开 Actions 页面查看自动构建："
Write-Host "     $($RepoUrl -replace '\.git$','')/actions"
Write-Host "  2. 构建完成后在对应 run 的 Artifacts 下载 jianji-apk → app-release.apk"
Write-Host "  3. 手机下载安装（允许'安装未知来源应用'）"
Write-Host "  4. 想覆盖升级不丢数据：按 docs/04-构建与签名指南.md 配置 release 签名 Secret"
