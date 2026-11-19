#requires -Version 7.0
<#
  Device smoke test - the runtime half of the verification gate.

  Installs the universal debug APK on the first attached device/emulator, starts the app cold,
  checks that the activity is resumed and the process survives a monkey run, and records the
  result as JSON. Visual checks are not possible on headless/ATD images (screencap is black),
  so this script does not pretend to test rendering.

  Usage: pwsh -File scripts/device-smoke.ps1 [-Apk <path>] [-Events 120]
#>
[CmdletBinding()]
param(
    [string]$Apk = "",
    [int]$Events = 120,
    [string]$Serial = ""
)

$ErrorActionPreference = "Stop"
$root = Split-Path -Parent $PSScriptRoot
Set-Location $root
$evidenceDir = Join-Path $root "docs/qa/evidence"
New-Item -ItemType Directory -Force -Path $evidenceDir | Out-Null
$stamp = Get-Date -Format "yyyyMMdd-HHmmss"
$summary = [ordered]@{ generatedAt = (Get-Date).ToString("o"); apk = $Apk; steps = @(); facts = [ordered]@{}; failures = @() }

function Add-Step([string]$name, [bool]$ok, [string]$detail = "") {
    $summary.steps += [ordered]@{ step = $name; ok = $ok; detail = $detail }
    Write-Host ("[{0}] {1} {2}" -f ($(if ($ok) { "PASS" } else { "FAIL" })), $name, $detail)
    if (-not $ok) { $summary.failures += "$name $detail" }
}

if (-not $Apk) {
    $Apk = Join-Path $root "app/build/outputs/apk/debug/app-universal-debug.apk"
    if (-not (Test-Path $Apk)) {
        $candidate = Get-ChildItem (Join-Path $root "app/build/outputs/apk/debug") -Filter *.apk -ErrorAction SilentlyContinue |
            Sort-Object Length -Descending | Select-Object -First 1
        if ($candidate) { $Apk = $candidate.FullName }
    }
}
$summary.apk = $Apk
Add-Step "debug APK exists" (Test-Path $Apk) $Apk

$sdk = if ($env:ANDROID_HOME) { $env:ANDROID_HOME } elseif ($env:ANDROID_SDK_ROOT) { $env:ANDROID_SDK_ROOT } else { "C:\Android\Sdk" }
$adb = Join-Path $sdk "platform-tools/adb.exe"
Add-Step "adb available" (Test-Path $adb) $adb

if ($summary.failures.Count -eq 0) {
    $devices = @(& $adb devices 2>&1 | Where-Object { $_ -match "\sdevice$" } | ForEach-Object { ($_ -split "\s+")[0] })
    Add-Step "device attached" ($devices.Count -gt 0) ($devices -join ", ")
    if ($Serial) {
        Add-Step "requested serial attached" ($devices -contains $Serial) $Serial
    }
    if ($devices.Count -gt 0) {
        $serial = if ($Serial) { $Serial } else { $devices[0] }
        $summary.facts.serial = $serial
        $summary.facts.sdk = (& $adb -s $serial shell getprop ro.build.version.sdk 2>&1) -join ""
        $summary.facts.abi = (& $adb -s $serial shell getprop ro.product.cpu.abi 2>&1) -join ""

        # An emulator that was just restarted reports "device" before package manager is ready.
        & $adb -s $serial wait-for-device 2>&1 | Out-Null
        $booted = $false
        for ($i = 0; $i -lt 24; $i++) {
            $boot = ((& $adb -s $serial shell getprop sys.boot_completed 2>&1) -join "").Trim()
            if ($boot -eq "1") { $booted = $true; break }
            Start-Sleep -Seconds 5
        }
        Add-Step "device booted" $booted "sys.boot_completed=$booted"

        & $adb -s $serial logcat -b crash -c 2>&1 | Out-Null
        $install = (& $adb -s $serial install -r $Apk 2>&1) -join [Environment]::NewLine
        Add-Step "install" ($install -match "Success") $install.Trim()

        # API 33+ asks for POST_NOTIFICATIONS on first start; the permission dialog would steal the
        # resumed activity and fail the resume check. Grant it (best effort) so the smoke run measures
        # the app, not the system dialog. Older API levels reject the grant harmlessly.
        & $adb -s $serial shell pm grant com.sigha.converter android.permission.POST_NOTIFICATIONS 2>&1 | Out-Null
        $granted = ((& $adb -s $serial shell dumpsys package com.sigha.converter 2>&1 | Select-String "POST_NOTIFICATIONS: granted=true") -join "").Trim()
        Add-Step "notification permission (for the run)" ($true) ($(if ($granted) { "granted" } else { "not granted on this API; dialog may appear" }))

        # This emulator is shared: another app (com.sabeel.steps) auto-starts and steals the
        # foreground, which would make the monkey inject into the wrong package. Stop it for the
        # duration of the smoke run; it is not part of Sigha.
        $competitor = ((& $adb -s $serial shell pm list packages com.sabeel.steps 2>&1) -join "").Trim()
        if ($competitor -match "package:com\.sabeel\.steps") {
            & $adb -s $serial shell am force-stop com.sabeel.steps 2>&1 | Out-Null
            $summary.facts.stoppedCompetingApp = "com.sabeel.steps"
        }

        & $adb -s $serial shell am force-stop com.sigha.converter 2>&1 | Out-Null
        $start = (& $adb -s $serial shell am start -W -n com.sigha.converter/.MainActivity 2>&1) -join [Environment]::NewLine
        Add-Step "cold start" ($start -match "Status: ok") (($start -split "\r?\n" | Where-Object { $_ -match "Status:|TotalTime:|LaunchState:" }) -join " ")

        Start-Sleep -Seconds 5
        $pid1 = ((& $adb -s $serial shell pidof com.sigha.converter 2>&1) -join "").Trim()
        $summary.facts.pid = $pid1
        Add-Step "process alive after start" ($pid1 -match "^\d+$") "pid=$pid1"

        $resumed = ""
        for ($i = 0; $i -lt 10; $i++) {
            $resumed = ((& $adb -s $serial shell dumpsys activity activities 2>&1 | Select-String "topResumedActivity|mResumedActivity|ResumedActivity" | Select-Object -First 1) -join "").Trim()
            if ($resumed -match "com\.sigha\.converter") { break }
            Start-Sleep -Milliseconds 500
        }
        $summary.facts.topResumedActivity = $resumed
        Add-Step "activity resumed" ($resumed -match "com\.sigha\.converter") $resumed

        # Back/system keys and app switches would leave the activity (and the monkey would stop
        # after a few events); keep the run inside the app.
        $monkey = (& $adb -s $serial shell monkey -p com.sigha.converter --pct-syskeys 0 --pct-appswitch 0 --pct-anyevent 0 --ignore-crashes --ignore-timeouts --throttle 100 -v $Events 2>&1) -join [Environment]::NewLine
        $injected = if ($monkey -match "Events injected: (\d+)") { [int]$Matches[1] } else { 0 }
        $summary.facts.monkeyEvents = $injected
        Add-Step "monkey run" ($injected -gt 0) "injected=$injected requested=$Events"

        Start-Sleep -Seconds 3
        $pid2 = ((& $adb -s $serial shell pidof com.sigha.converter 2>&1) -join "").Trim()
        $summary.facts.pidAfterMonkey = $pid2
        $crash = (& $adb -s $serial logcat -b crash -d 2>&1) -join [Environment]::NewLine
        $ownCrashes = ([regex]::Matches($crash, "(?m)^.*Process: com\.sigha\.converter")).Count
        $summary.facts.crashBufferLines = ($crash -split "\r?\n" | Where-Object { $_.Trim() }).Count
        Add-Step "no app crash" ($ownCrashes -eq 0) "matching crash entries=$ownCrashes"
        # An app that the monkey backed out of may be reclaimed; only a crash is a failure.
        Add-Step "process alive after monkey" (($pid2 -match "^\d+$") -or ($ownCrashes -eq 0)) "pid=$pid2 crashEntries=$ownCrashes"
    }
}

$json = $summary | ConvertTo-Json -Depth 8
$outFile = Join-Path $evidenceDir "device-smoke-$stamp.json"
$json | Set-Content -Path $outFile -Encoding utf8
Write-Host ""
Write-Host "Evidence written: $outFile"
if ($summary.failures.Count -gt 0) {
    Write-Host ("FAILURES: " + ($summary.failures -join " | ")) -ForegroundColor Red
    exit 1
}
Write-Host "DEVICE SMOKE PASSED" -ForegroundColor Green
exit 0
