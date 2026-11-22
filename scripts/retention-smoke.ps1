#requires -Version 7.0
<#
  Completion-moment / repeat-use smoke test - the runtime half of the retention pass.

  Drives the real UI on an attached emulator/device through the actions a first-time user takes
  after a batch finishes: the empty-state doors, a two-file batch, backgrounding the app, the
  completion notification and its tap target, "Share all", and the queue's way back to Convert.

  Destructive: clears the Sigha app data before starting and writes two small PNG fixtures to
  /sdcard/Download. Uses the fixed 1080x2400 portrait layout of the QA emulator (the UI-dump
  coordinates are screen pixels), so run it on the dedicated QA AVD, not a phone in use.

  Usage: pwsh -File scripts/retention-smoke.ps1 [-Serial <serial>] [-Apk <path>]
#>
[CmdletBinding()]
param(
    [string]$Serial = "",
    [string]$Apk = ""
)

$ErrorActionPreference = "Stop"
$root = Split-Path -Parent $PSScriptRoot
Set-Location $root
$stamp = Get-Date -Format "yyyyMMdd-HHmmss"
$outDir = Join-Path (Join-Path $root "docs/qa/evidence") ("retention-" + $stamp)
New-Item -ItemType Directory -Force -Path $outDir | Out-Null

$summary = [ordered]@{
    generatedAt = (Get-Date).ToString("o")
    apk = ""
    serial = ""
    facts = [ordered]@{}
    steps = @()
    shots = @()
    failures = @()
}
$script:failures = @()

function Add-Step([string]$name, [bool]$ok, [string]$detail = "") {
    $summary.steps += [ordered]@{ step = $name; ok = $ok; detail = $detail }
    Write-Host ("[{0}] {1} {2}" -f ($(if ($ok) { "PASS" } else { "FAIL" })), $name, $detail)
    if (-not $ok) { $script:failures += "$name $detail" }
}

$sdk = if ($env:ANDROID_HOME) { $env:ANDROID_HOME } elseif ($env:ANDROID_SDK_ROOT) { $env:ANDROID_SDK_ROOT } else { "C:\Android\Sdk" }
$adb = Join-Path $sdk "platform-tools/adb.exe"
if (-not (Test-Path $adb)) { throw "adb not found at $adb" }

if (-not $Apk) {
    $Apk = Join-Path $root "app/build/outputs/apk/debug/app-x86_64-debug.apk"
    if (-not (Test-Path $Apk)) {
        $candidate = Get-ChildItem (Join-Path $root "app/build/outputs/apk/debug") -Filter *.apk -ErrorAction SilentlyContinue |
            Sort-Object Length -Descending | Select-Object -First 1
        if ($candidate) { $Apk = $candidate.FullName }
    }
}
$summary.apk = $Apk
Add-Step "debug APK exists" (Test-Path $Apk) $Apk
if ($script:failures.Count -gt 0) { throw $script:failures[0] }

$devices = @(& $adb devices 2>&1 | Where-Object { $_ -match "\sdevice$" } | ForEach-Object { ($_ -split "\s+")[0] })
Add-Step "device attached" ($devices.Count -gt 0) ($devices -join ", ")
if ($Serial) { Add-Step "requested serial attached" ($devices -contains $Serial) $Serial }
if ($script:failures.Count -gt 0) { throw $script:failures[0] }
$serial = if ($Serial) { $Serial } else { $devices[0] }
$summary.serial = $serial
$summary.facts.sdk = ((& $adb -s $serial shell getprop ro.build.version.sdk 2>&1) -join "").Trim()
$summary.facts.abi = ((& $adb -s $serial shell getprop ro.product.cpu.abi 2>&1) -join "").Trim()
$summary.facts.model = ((& $adb -s $serial shell getprop ro.product.model 2>&1) -join "").Trim()

function Invoke-Adb {
    param([string[]]$Arguments)
    return (& $adb -s $serial @Arguments 2>&1)
}

$script:DumpPath = Join-Path ([System.IO.Path]::GetTempPath()) ("sigha-retention-" + $stamp + ".xml")
function Get-UiXml {
    # uiautomator prints "null root node" while a window is being recreated; that is transient.
    for ($i = 0; $i -lt 5; $i++) {
        Invoke-Adb @("shell", "uiautomator", "dump", "/sdcard/sigha-retention.xml") | Out-Null
        Invoke-Adb @("pull", "/sdcard/sigha-retention.xml", $script:DumpPath) | Out-Null
        try {
            $xml = [xml](Get-Content $script:DumpPath -Raw)
            if ($xml.hierarchy.node) { return $xml }
        } catch { }
        Start-Sleep -Milliseconds 600
    }
    return $null
}

function Get-UiNodes {
    param($Xml)
    if ($null -eq $Xml) { return @() }
    return @($Xml.SelectNodes("//node"))
}

function Find-UiNode {
    param($Xml, [string]$Text, [string]$Desc)
    foreach ($node in (Get-UiNodes $Xml)) {
        if ($Text -and [string]$node.text -eq $Text) { return $node }
        if ($Desc -and [string]$node.'content-desc' -eq $Desc) { return $node }
    }
    return $null
}

function Wait-UiNode {
    param([string]$Text, [string]$Desc, [int]$TimeoutSec = 20)
    $deadline = (Get-Date).AddSeconds($TimeoutSec)
    while ((Get-Date) -lt $deadline) {
        $node = Find-UiNode -Xml (Get-UiXml) -Text $Text -Desc $Desc
        if ($node) { return $node }
        Start-Sleep -Milliseconds 500
    }
    return $null
}

function Get-NodeCenter {
    param($Node)
    $numbers = @([regex]::Matches($Node.bounds, "\d+") | ForEach-Object { [int]$_.Value })
    return @{ x = [int](($numbers[0] + $numbers[2]) / 2); y = [int](($numbers[1] + $numbers[3]) / 2) }
}

function Tap-Node {
    param($Node)
    if (-not $Node) { return $false }
    $center = Get-NodeCenter $Node
    Invoke-Adb @("shell", "input", "tap", "$($center.x)", "$($center.y)") | Out-Null
    return $true
}

function Tap-Clickable {
    param($Node)
    $hop = $Node
    for ($i = 0; $i -lt 6 -and $hop; $i++) {
        if ([string]$hop.clickable -eq "true") { return (Tap-Node $hop) }
        $hop = $hop.ParentNode
    }
    return $false
}

function Scroll-UntilMatch {
    param([string]$Pattern, [int]$TimeoutSec = 20)
    $deadline = (Get-Date).AddSeconds($TimeoutSec)
    while ((Get-Date) -lt $deadline) {
        foreach ($node in (Get-UiNodes (Get-UiXml))) {
            if ([string]$node.text -match $Pattern) { return $node }
        }
        Invoke-Adb @("shell", "input", "swipe", "540", "1400", "540", "300", "400") | Out-Null
        Start-Sleep -Milliseconds 800
    }
    return $null
}

function Save-Shot {
    param([string]$Name)
    $target = Join-Path $outDir $Name
    Invoke-Adb @("shell", "screencap", "-p", "/sdcard/sigha-retention.png") | Out-Null
    Invoke-Adb @("pull", "/sdcard/sigha-retention.png", $target) | Out-Null
    $summary.shots += $target
    return $target
}

function Start-App {
    Invoke-Adb @("shell", "am", "start", "-n", "com.sigha.converter/.MainActivity") | Out-Null
}

try {
    $install = (Invoke-Adb @("install", "-r", $Apk) | Out-String).Trim()
    Add-Step "APK installed" ($install -match "Success") $install
    if ($script:failures.Count -gt 0) { throw $script:failures[0] }

    # Two tiny PNGs so the batch needs no host fixtures.
    $tinyPng = "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mP8z8BQDwAEhQGAhKmMIQAAAABJRU5ErkJggg=="
    Invoke-Adb @("shell", "mkdir", "-p", "/sdcard/Download") | Out-Null
    foreach ($name in @("uitest1.png", "uitest2.png")) {
        Invoke-Adb @("shell", "echo $tinyPng | base64 -d > /sdcard/Download/$name") | Out-Null
    }
    $fixtureList = (Invoke-Adb @("shell", "ls", "-l", "/sdcard/Download/uitest1.png", "/sdcard/Download/uitest2.png") | Out-String)
    Add-Step "PNG fixtures on device" ($fixtureList -match "uitest1.png" -and $fixtureList -match "uitest2.png") ""

    # A clean first-run state; the app also gets the notification permission without a prompt.
    Invoke-Adb @("shell", "input", "keyevent", "KEYCODE_WAKEUP") | Out-Null
    Invoke-Adb @("shell", "wm", "dismiss-keyguard") | Out-Null
    Invoke-Adb @("shell", "cmd", "statusbar", "collapse") | Out-Null
    Invoke-Adb @("shell", "am", "force-stop", "com.android.documentsui") | Out-Null
    Invoke-Adb @("shell", "am", "force-stop", "com.sigha.converter") | Out-Null
    Invoke-Adb @("shell", "pm", "clear", "com.sigha.converter") | Out-Null
    Invoke-Adb @("shell", "pm", "grant", "com.sigha.converter", "android.permission.POST_NOTIFICATIONS") | Out-Null
    Start-App

    # 1. The History empty state is not a dead end.
    $historyTab = Wait-UiNode -Text "History" -TimeoutSec 30
    $onHistory = $false
    for ($i = 0; $i -lt 3 -and -not $onHistory; $i++) {
        Tap-Node $historyTab | Out-Null
        if (Wait-UiNode -Text "No conversions yet" -TimeoutSec 8) { $onHistory = $true }
        else { Start-Sleep -Milliseconds 700; $historyTab = Find-UiNode -Xml (Get-UiXml) -Text "History" }
    }
    Add-Step "history empty state shown" $onHistory ""
    if ($onHistory) {
        $historyCta = Find-UiNode -Xml (Get-UiXml) -Text "Choose files"
        Save-Shot "history-empty.png" | Out-Null
        $back = $false
        for ($i = 0; $i -lt 3 -and -not $back; $i++) {
            $historyCta = Find-UiNode -Xml (Get-UiXml) -Text "Choose files"
            if (-not $historyCta) { break }
            Tap-Clickable $historyCta | Out-Null
            if (Wait-UiNode -Text "Your files, converted on this phone" -TimeoutSec 8) { $back = $true }
            else { Start-Sleep -Milliseconds 700 }
        }
        Add-Step "history empty CTA returns to Convert" $back ""
    }

    # 2. Two-file batch through the real picker.
    $pickerOpen = $false
    for ($open = 0; $open -lt 4 -and -not $pickerOpen; $open++) {
        $cta = Wait-UiNode -Text "Choose files" -TimeoutSec 20
        if (-not $cta) { break }
        Tap-Node $cta | Out-Null
        Start-Sleep -Milliseconds 1500
        if ((Wait-UiNode -Desc "Show roots" -TimeoutSec 8) -or (Wait-UiNode -Text "Files in Downloads" -TimeoutSec 2)) { $pickerOpen = $true }
    }
    Add-Step "DocumentsUI picker opened" $pickerOpen ""
    $picked = $false
    if ($pickerOpen) {
        $reachDownloads = $false
        for ($nav = 0; $nav -lt 4 -and -not $reachDownloads; $nav++) {
            if ((Wait-UiNode -Text "Files in Downloads" -TimeoutSec 2) -or (Wait-UiNode -Text "uitest1.png" -TimeoutSec 1)) { $reachDownloads = $true; break }
            $roots = Wait-UiNode -Desc "Show roots" -TimeoutSec 6
            if (-not $roots) { break }
            Tap-Node $roots | Out-Null
            Start-Sleep -Milliseconds 1500
            $downloads = $null
            foreach ($node in (Get-UiNodes (Get-UiXml))) {
                if ([string]$node.text -eq "Downloads") {
                    $center = Get-NodeCenter $node
                    if ($center.y -gt 300) { $downloads = $node; break }
                }
            }
            if ($downloads) { Tap-Node $downloads | Out-Null }
            Start-Sleep -Milliseconds 1500
            if ((Wait-UiNode -Text "Files in Downloads" -TimeoutSec 4) -or (Wait-UiNode -Text "uitest1.png" -TimeoutSec 2)) { $reachDownloads = $true }
        }
        if ($reachDownloads) {
            $first = $null
            for ($attempt = 0; $attempt -lt 4 -and -not $first; $attempt++) {
                $first = Wait-UiNode -Text "uitest1.png" -TimeoutSec 6
                if (-not $first) {
                    Invoke-Adb @("shell", "input", "swipe", "540", "1800", "540", "900", "300") | Out-Null
                    Start-Sleep -Milliseconds 800
                }
            }
            if ($first) {
                $center = Get-NodeCenter $first
                Invoke-Adb @("shell", "input", "swipe", "$($center.x)", "$($center.y)", "$($center.x)", "$($center.y)", "1500") | Out-Null
                Start-Sleep -Milliseconds 1200
                $second = Wait-UiNode -Text "uitest2.png" -TimeoutSec 8
                if ($second) {
                    Tap-Node $second | Out-Null
                    Start-Sleep -Milliseconds 800
                    $openButton = $null
                    foreach ($guess in @("Open", "Select", "Done")) {
                        $openButton = Wait-UiNode -Text $guess -TimeoutSec 2
                        if ($openButton) { break }
                    }
                    if ($openButton) { Tap-Node $openButton | Out-Null }
                    $picked = [bool](Wait-UiNode -Text "Convert to" -TimeoutSec 25)
                }
            }
        }
    }
    Add-Step "two images selected" $picked ""
    if ($script:failures.Count -gt 0) { throw $script:failures[0] }

    # 3. JPG target; the batch output is shareable as image/jpeg on this image.
    $action = $null
    foreach ($node in (Get-UiNodes (Get-UiXml))) {
        if ([string]$node.text -ne "JPG") { continue }
        $hop = $node
        for ($i = 0; $i -lt 6 -and $hop; $i++) {
            if ([string]$hop.clickable -eq "true") {
                Tap-Node $hop | Out-Null
                Start-Sleep -Milliseconds 900
                foreach ($candidate in (Get-UiNodes (Get-UiXml))) {
                    if ([string]$candidate.text -match "^Convert 2 files to JPG$") { $action = $candidate; break }
                }
                break
            }
            $hop = $hop.ParentNode
        }
        if ($action) { break }
    }
    Add-Step "convert action offered" ($null -ne $action) ""
    if (-not $action) { throw "convert action not found" }
    Tap-Node $action | Out-Null

    # 4. Background the app: the batch must finish and announce itself.
    Invoke-Adb @("shell", "input", "keyevent", "KEYCODE_HOME") | Out-Null
    $deadline = (Get-Date).AddSeconds(240)
    $notification = $false
    while ((Get-Date) -lt $deadline) {
        $dump = (Invoke-Adb @("shell", "dumpsys", "notification", "--noredact") | Out-String)
        if ($dump -match "Conversion finished") { $notification = $true; break }
        Start-Sleep -Milliseconds 1500
    }
    Add-Step "completion notification while backgrounded" $notification ""
    if ($notification) {
        Invoke-Adb @("shell", "cmd", "statusbar", "expand-notifications") | Out-Null
        Start-Sleep -Milliseconds 1500
        Save-Shot "notification.png" | Out-Null
        $openedApp = $false
        for ($i = 0; $i -lt 3 -and -not $openedApp; $i++) {
            $notifNode = Find-UiNode -Xml (Get-UiXml) -Text "Conversion finished"
            if (-not $notifNode) { Start-Sleep -Milliseconds 800; continue }
            Tap-Clickable $notifNode | Out-Null
            Start-Sleep -Milliseconds 1500
            $activityDump = (Invoke-Adb @("shell", "dumpsys", "activity", "activities") | Out-String)
            if ($activityDump -match "(topResumedActivity|ResumedActivity).*com\.sigha\.converter") { $openedApp = $true }
            else {
                Invoke-Adb @("shell", "cmd", "statusbar", "expand-notifications") | Out-Null
                Start-Sleep -Milliseconds 800
            }
        }
        Add-Step "tapping the notification opens Sigha" $openedApp ""
    }

    # 5. One share action for the whole batch, and Sigha must not offer to re-import its own output.
    $shareAll = Scroll-UntilMatch -Pattern "^Share all 2 files$" -TimeoutSec 20
    Add-Step "Share all action present" ($null -ne $shareAll) ""
    if ($shareAll) {
        Save-Shot "results.png" | Out-Null
        Tap-Clickable $shareAll | Out-Null
        Start-Sleep -Milliseconds 1800
        $chooserDump = (Invoke-Adb @("shell", "dumpsys", "activity", "activities") | Out-String)
        $chooser = $chooserDump -match "ResolverActivity|ChooserActivity|intentresolver"
        Add-Step "Share all opens the system chooser" $chooser ""
        Save-Shot "share-chooser.png" | Out-Null
        if ($chooser) {
            $sheetLabels = @()
            foreach ($node in (Get-UiNodes (Get-UiXml))) {
                $text = [string]$node.text
                if (-not $text) { continue }
                $bounds = @([regex]::Matches($node.bounds, "\d+") | ForEach-Object { [int]$_.Value })
                if ($bounds.Count -ge 4 -and $bounds[1] -ge 1300) { $sheetLabels += $text }
            }
            $summary.facts.chooserLabels = ($sheetLabels -join ", ")
            Add-Step "Sigha excluded from its own share sheet" ($sheetLabels -notcontains "Sigha") ($sheetLabels -join ", ")
        }
        Invoke-Adb @("shell", "input", "keyevent", "KEYCODE_BACK") | Out-Null
        Start-Sleep -Milliseconds 1000
    }

    # 6. The queue's empty state leads back to Convert.
    $viewResults = Scroll-UntilMatch -Pattern "^View results$" -TimeoutSec 20
    Add-Step "View results action present" ($null -ne $viewResults) ""
    if ($viewResults) {
        $queueOpen = $false
        for ($i = 0; $i -lt 3 -and -not $queueOpen; $i++) {
            Tap-Clickable $viewResults | Out-Null
            if (Wait-UiNode -Text "Queue" -TimeoutSec 10) { $queueOpen = $true }
            else { Start-Sleep -Milliseconds 700; $viewResults = Find-UiNode -Xml (Get-UiXml) -Text "View results" }
        }
        Add-Step "queue opened from the results card" $queueOpen ""
        if ($queueOpen) {
            $cleared = $false
            for ($i = 0; $i -lt 3 -and -not $cleared; $i++) {
                $clear = Find-UiNode -Xml (Get-UiXml) -Text "Clear finished"
                if (-not $clear) { Start-Sleep -Milliseconds 800; continue }
                Tap-Node $clear | Out-Null
                if (Wait-UiNode -Text "Nothing in the queue" -TimeoutSec 8) { $cleared = $true }
                else { Start-Sleep -Milliseconds 700 }
            }
            Add-Step "queue empty state shown" $cleared ""
            if ($cleared) {
                Save-Shot "queue-empty.png" | Out-Null
                $queueBack = $false
                for ($i = 0; $i -lt 3 -and -not $queueBack; $i++) {
                    $queueCta = Find-UiNode -Xml (Get-UiXml) -Text "Choose files"
                    if (-not $queueCta) { break }
                    Tap-Clickable $queueCta | Out-Null
                    if (Wait-UiNode -Text "Convert to" -TimeoutSec 8) { $queueBack = $true }
                    else { Start-Sleep -Milliseconds 700 }
                }
                Add-Step "queue empty CTA returns to Convert" $queueBack ""
            }
        }
    }
} catch {
    Add-Step "unexpected failure" $false $_.Exception.Message
} finally {
    $summary.failures = @($script:failures)
    $jsonPath = Join-Path $outDir ("retention-" + $stamp + ".json")
    $summary | ConvertTo-Json -Depth 6 | Set-Content -Path $jsonPath -Encoding utf8
    Write-Host ""
    Write-Host ("Evidence: " + $jsonPath)
    if ($summary.failures.Count -eq 0) { Write-Host "ALL RETENTION CHECKS PASSED"; exit 0 }
    Write-Host ("FAILED: " + ($summary.failures -join "; "))
    exit 1
}
