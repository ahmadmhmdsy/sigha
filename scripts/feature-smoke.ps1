#requires -Version 7.0
<#
  Media-routing / retry / priming smoke test - the runtime half of this feature pass.

  Drives the real UI on an attached emulator or device through:
    1. a two-image batch, the contextual notification rationale and the system prompt;
    2. the gallery path: JPGs appear in Pictures/Sigha and in MediaStore images;
    3. the remembered-target path: re-picking one image shows "Convert to JPG";
    4. a corrupt image failing, "Retry failed" re-running it, and a deleted source being
       reported as FILE_ACCESS_LOST instead of being retried into the same failure;
    5. a text file converted to PDF and landing in Download/Sigha;
    6. a WAV converted to MP3 and landing in Music/Sigha plus MediaStore audio;
    7. Settings -> Completion alerts opening the system notification page;
    8. a backgrounded batch still posting its completion notification.

  Destructive: clears the Sigha app data and writes fixtures to /sdcard/Download, then deletes
  one of them on purpose. Uses the fixed 1080x2400 portrait layout of the QA emulator, so run
  it on the dedicated QA AVD, not a phone in use.

  Usage: pwsh -File scripts/feature-smoke.ps1 [-Serial <serial>] [-Apk <path>]
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
$outDir = Join-Path $root "docs/qa/evidence" ("features-" + $stamp)
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
    # adb has no timeout of its own: a wedged "uiautomator dump" on an emulator would hang the
    # whole run (observed on API 36), so every call is bounded and the timeout is returned as a
    # value the XML loop can recognise and retry.
    param([string[]]$Arguments, [int]$TimeoutSec = 60)
    $outFile = [System.IO.Path]::GetTempFileName()
    $errFile = [System.IO.Path]::GetTempFileName()
    try {
        $proc = Start-Process -FilePath $adb -ArgumentList (@("-s", $serial) + $Arguments) -NoNewWindow -PassThru -RedirectStandardOutput $outFile -RedirectStandardError $errFile
        if (-not $proc.WaitForExit($TimeoutSec * 1000)) {
            try { $proc.Kill($true) } catch { }
            return "<adb-timeout $($TimeoutSec)s: $($Arguments -join ' ')>"
        }
        $stdout = Get-Content -Raw -LiteralPath $outFile -ErrorAction SilentlyContinue
        $stderr = Get-Content -Raw -LiteralPath $errFile -ErrorAction SilentlyContinue
        return ("" + $stdout + $stderr)
    } finally {
        Remove-Item -LiteralPath $outFile, $errFile -Force -ErrorAction SilentlyContinue
    }
}

$script:DumpPath = Join-Path ([System.IO.Path]::GetTempPath()) ("sigha-features-" + $stamp + ".xml")
$script:LastXml = $null
$script:LastXmlAt = [datetime]::MinValue
function Get-UiXml {
    # uiautomator dump occasionally wedges on API 36; keep a very short cache so the polling
    # loops do not spawn a dump process every half second, and clear the device-side process a
    # timed-out dump leaves behind (it keeps the accessibility connection and wedges the next).
    $age = ((Get-Date) - $script:LastXmlAt).TotalMilliseconds
    if ($null -ne $script:LastXml -and $age -lt 300) { return $script:LastXml }
    for ($i = 0; $i -lt 3; $i++) {
        $dumpOutput = Invoke-Adb -Arguments @("shell", "uiautomator", "dump", "/sdcard/sigha-features.xml") -TimeoutSec 20
        if ($dumpOutput -match "adb-timeout") {
            Invoke-Adb -Arguments @("shell", "pkill", "-f", "com.android.commands.uiautomator") -TimeoutSec 10 | Out-Null
            Start-Sleep -Milliseconds 400
            continue
        }
        Invoke-Adb @("pull", "/sdcard/sigha-features.xml", $script:DumpPath) | Out-Null
        try {
            $xml = [xml](Get-Content $script:DumpPath -Raw)
            if ($xml.hierarchy.node) {
                $script:LastXml = $xml
                $script:LastXmlAt = Get-Date
                return $xml
            }
        } catch { }
        Start-Sleep -Milliseconds 400
    }
    return $null
}

# A dump killed on the host leaves its device-side process running; clear it before the run.
Invoke-Adb -Arguments @("shell", "pkill", "-f", "uiautomator") -TimeoutSec 10 | Out-Null

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

function Find-UiRegex {
    param($Xml, [string]$Pattern)
    foreach ($node in (Get-UiNodes $Xml)) {
        if ([string]$node.text -match $Pattern) { return $node }
    }
    return $null
}

function Wait-UiRegex {
    param([string]$Pattern, [int]$TimeoutSec = 20)
    $deadline = (Get-Date).AddSeconds($TimeoutSec)
    while ((Get-Date) -lt $deadline) {
        $node = Find-UiRegex -Xml (Get-UiXml) -Pattern $Pattern
        if ($node) { return $node }
        Start-Sleep -Milliseconds 500
    }
    return $null
}

function Wait-ScrolledNode {
    # The progress card sits below Options, so a failure/retry control is off-screen until the
    # workbench is scrolled; a plain dump would report a working feature as missing. The list may
    # also have moved while the check ran, so the search sweeps up and down instead of only up.
    param([string]$Text = "", [string]$Pattern = "", [int]$TimeoutSec = 120)
    $deadline = (Get-Date).AddSeconds($TimeoutSec)
    $iteration = 0
    while ((Get-Date) -lt $deadline) {
        $xml = Get-UiXml
        $node = if ($Text) { Find-UiNode -Xml $xml -Text $Text } else { Find-UiRegex -Xml $xml -Pattern $Pattern }
        if ($node) { return $node }
        if ($iteration % 2 -eq 0) {
            Invoke-Adb @("shell", "input", "swipe", "540", "1700", "540", "600", "300") | Out-Null
        } else {
            Invoke-Adb @("shell", "input", "swipe", "540", "700", "540", "1800", "300") | Out-Null
        }
        $iteration++
        Start-Sleep -Milliseconds 700
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

function Tap-ClickableRegex {
    param([string]$Pattern, [int]$TimeoutSec = 10)
    $node = Wait-UiRegex -Pattern $Pattern -TimeoutSec $TimeoutSec
    if (-not $node) { return $false }
    return (Tap-Clickable $node)
}

function Save-Shot {
    param([string]$name)
    $target = Join-Path $outDir $name
    Invoke-Adb @("shell", "screencap", "-p", "/sdcard/sigha-features.png") | Out-Null
    Invoke-Adb @("pull", "/sdcard/sigha-features.png", $target) | Out-Null
    $summary.shots += $name
    return $target
}

function Start-App {
    Invoke-Adb @("shell", "am", "start", "-n", "com.sigha.converter/.MainActivity") | Out-Null
}

function Reset-WorkbenchScroll {
    # The failure/retry sweeps scroll the workbench down; every later step starts from the top,
    # otherwise "Choose files" and the selection chip are simply not in the accessibility tree.
    for ($i = 0; $i -lt 3; $i++) {
        Invoke-Adb @("shell", "input", "swipe", "540", "800", "540", "1900", "250") | Out-Null
        Start-Sleep -Milliseconds 250
    }
    Start-Sleep -Milliseconds 400
}

function Clear-Selection {
    Reset-WorkbenchScroll
    $clear = Find-UiNode -Xml (Get-UiXml) -Desc "Clear selection"
    if ($clear) { Tap-Clickable $clear | Out-Null }
    Start-Sleep -Milliseconds 800
    return [bool](Wait-UiNode -Text "Choose files" -TimeoutSec 6)
}

function Open-Picker {
    $cta = Wait-UiNode -Text "Choose files" -TimeoutSec 8
    if (-not $cta) {
        Reset-WorkbenchScroll
        $cta = Wait-UiNode -Text "Choose files" -TimeoutSec 12
    }
    if (-not $cta) { return $false }
    Tap-Node $cta | Out-Null
    Start-Sleep -Milliseconds 1500
    $reachDownloads = $false
    for ($nav = 0; $nav -lt 4 -and -not $reachDownloads; $nav++) {
        if ((Wait-UiNode -Text "Files in Downloads" -TimeoutSec 2) -or
            (Wait-UiNode -Text "img_a.png" -TimeoutSec 1)) { $reachDownloads = $true; break }
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
        if ((Wait-UiNode -Text "Files in Downloads" -TimeoutSec 4) -or
            (Wait-UiNode -Text "img_a.png" -TimeoutSec 2)) { $reachDownloads = $true }
    }
    return $reachDownloads
}

function Scroll-ToName {
    param([string]$name, [int]$Attempts = 4)
    for ($attempt = 0; $attempt -lt $Attempts; $attempt++) {
        $node = Wait-UiNode -Text $name -TimeoutSec 6
        if ($node) { return $node }
        Invoke-Adb @("shell", "input", "swipe", "540", "1800", "540", "900", "300") | Out-Null
        Start-Sleep -Milliseconds 800
    }
    return $null
}

function Pick-One {
    param([string]$name)
    if (-not (Open-Picker)) { return $false }
    $node = Scroll-ToName $name
    if (-not $node) { return $false }
    Tap-Node $node | Out-Null
    Start-Sleep -Milliseconds 1200
    return [bool](Wait-UiNode -Text $name -TimeoutSec 20)
}

function Pick-Many {
    param([string[]]$names)
    if ($names.Count -lt 2) { return (Pick-One $names[0]) }
    if (-not (Open-Picker)) { return $false }
    $first = Scroll-ToName $names[0]
    if (-not $first) { return $false }
    $center = Get-NodeCenter $first
    Invoke-Adb @("shell", "input", "swipe", "$($center.x)", "$($center.y)", "$($center.x)", "$($center.y)", "1500") | Out-Null
    Start-Sleep -Milliseconds 1200
    for ($i = 1; $i -lt $names.Count; $i++) {
        $next = Scroll-ToName $names[$i]
        if (-not $next) { return $false }
        Tap-Node $next | Out-Null
        Start-Sleep -Milliseconds 800
    }
    $openButton = $null
    foreach ($guess in @("Open", "Select", "Done")) {
        $openButton = Wait-UiNode -Text $guess -TimeoutSec 2
        if ($openButton) { break }
    }
    if ($openButton) { Tap-Node $openButton | Out-Null }
    foreach ($name in $names) {
        if (-not (Wait-UiNode -Text $name -TimeoutSec 20)) { return $false }
    }
    return $true
}

function Tap-Target {
    param([string]$label)
    foreach ($node in (Get-UiNodes (Get-UiXml))) {
        if ([string]$node.text -ne $label) { continue }
        $hop = $node
        for ($i = 0; $i -lt 6 -and $hop; $i++) {
            if ([string]$hop.clickable -eq "true") {
                Tap-Node $hop | Out-Null
                Start-Sleep -Milliseconds 900
                return $true
            }
            $hop = $hop.ParentNode
        }
    }
    return $false
}

try {
    $install = (Invoke-Adb @("install", "-r", $Apk) | Out-String).Trim()
    Add-Step "APK installed" ($install -match "Success") $install
    if ($script:failures.Count -gt 0) { throw $script:failures[0] }
    # Crash attribution needs a buffer that only contains this run.
    Invoke-Adb @("logcat", "-c") | Out-Null

    # Output folders from an older QA run would make the "file exists" checks lie, so the
    # media destination tree starts absent; app data is cleared below as well.
    foreach ($outDirPath in @("/sdcard/Pictures/Sigha", "/sdcard/Download/Sigha", "/sdcard/Music/Sigha")) {
        Invoke-Adb @("shell", "rm", "-rf", $outDirPath) | Out-Null
    }
    $tinyPng = "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mP8z8BQDwAEhQGAhKmMIQAAAABJRU5ErkJggg=="
    Invoke-Adb @("shell", "mkdir", "-p", "/sdcard/Download") | Out-Null
    foreach ($name in @("img_a.png", "img_b.png")) {
        Invoke-Adb @("shell", "echo $tinyPng | base64 -d > /sdcard/Download/$name") | Out-Null
    }
    Invoke-Adb @("shell", "echo 'Sigha smoke note. Convert me to PDF.' > /sdcard/Download/note.txt") | Out-Null
    Invoke-Adb @("shell", "echo 'this is not a real png' > /sdcard/Download/broken.png") | Out-Null

    $wav = Join-Path ([System.IO.Path]::GetTempPath()) "sigha-tone.wav"
    $rate = 8000
    $samples = 4000
    $dataLen = $samples * 2
    $ms = [System.IO.MemoryStream]::new()
    $bw = [System.IO.BinaryWriter]::new($ms)
    $ascii = [System.Text.Encoding]::ASCII
    $bw.Write($ascii.GetBytes("RIFF")); $bw.Write([int](36 + $dataLen)); $bw.Write($ascii.GetBytes("WAVEfmt "))
    $bw.Write([int]16); $bw.Write([int16]1); $bw.Write([int16]1); $bw.Write([int]$rate)
    $bw.Write([int]($rate * 2)); $bw.Write([int16]2); $bw.Write([int16]16)
    $bw.Write($ascii.GetBytes("data")); $bw.Write([int]$dataLen)
    for ($i = 0; $i -lt $samples; $i++) {
        $value = [int16]([math]::Sin(2 * [math]::PI * 440 * $i / $rate) * 8000)
        $bw.Write($value)
    }
    $bw.Flush()
    [System.IO.File]::WriteAllBytes($wav, $ms.ToArray())
    $bw.Dispose(); $ms.Dispose()
    Invoke-Adb @("push", $wav, "/sdcard/Download/tone.wav") | Out-Null

    $fixtureList = (Invoke-Adb @("shell", "ls", "-l", "/sdcard/Download") | Out-String)
    Add-Step "fixtures on device" (($fixtureList -match "img_a.png") -and ($fixtureList -match "tone.wav") -and ($fixtureList -match "broken.png") -and ($fixtureList -match "note.txt")) ""

    Invoke-Adb @("shell", "input", "keyevent", "KEYCODE_WAKEUP") | Out-Null
    Invoke-Adb @("shell", "wm", "dismiss-keyguard") | Out-Null
    Invoke-Adb @("shell", "cmd", "statusbar", "collapse") | Out-Null
    Invoke-Adb @("shell", "am", "force-stop", "com.android.documentsui") | Out-Null
    Invoke-Adb @("shell", "am", "force-stop", "com.sigha.converter") | Out-Null
    Invoke-Adb @("shell", "pm", "clear", "com.sigha.converter") | Out-Null
    # Permission is deliberately NOT granted here: the app must ask contextually.
    Start-App

    # 1. Two-image batch and the contextual notification rationale.
    $ready = [bool](Wait-UiNode -Text "Your files, converted on this phone" -TimeoutSec 30)
    Add-Step "workbench ready" $ready ""
    if (-not $ready) { throw "workbench not ready" }

    $picked = Pick-Many @("img_a.png", "img_b.png")
    Add-Step "two images selected" $picked ""
    Save-Shot "00-two-images.png" | Out-Null
    if (-not $picked) { throw "could not pick two images" }

    $jpgTarget = Tap-Target "JPG"
    Add-Step "JPG target selected" $jpgTarget ""

    $action = Wait-UiRegex -Pattern "^Convert 2 files to JPG$" -TimeoutSec 10
    Add-Step "convert action offered" ($null -ne $action) ""
    Tap-Node $action | Out-Null

    $rationale = Wait-UiNode -Text "Turn on notifications?" -TimeoutSec 12
    Add-Step "contextual notification rationale shown before converting" ($null -ne $rationale) ""
    Save-Shot "01-notification-priming.png" | Out-Null
    if ($rationale) {
        $turnOn = Find-UiNode -Xml (Get-UiXml) -Text "Turn on"
        Tap-Node $turnOn | Out-Null
        Start-Sleep -Milliseconds 1200
        $allow = $null
        foreach ($guess in @("Allow", "While using the app", "OK")) {
            $allow = Wait-UiNode -Text $guess -TimeoutSec 5
            if ($allow) { break }
        }
        Add-Step "system notification prompt shown" ($null -ne $allow) ""
        Save-Shot "02-permission-prompt.png" | Out-Null
        if ($allow) { Tap-Node $allow | Out-Null }
    }

    $saved = Wait-UiRegex -Pattern "saved to Pictures/Sigha" -TimeoutSec 180
    Add-Step "image batch reports Pictures/Sigha" ($null -ne $saved) ""
    Save-Shot "03-pictures-saved.png" | Out-Null

    $lsPics = (Invoke-Adb @("shell", "ls", "/sdcard/Pictures/Sigha") | Out-String)
    $jpgCount = ([regex]::Matches($lsPics, "\.jpg")).Count
    $bothNamed = ($lsPics -match "img_a\.jpg") -and ($lsPics -match "img_b\.jpg")
    Add-Step "both JPGs on disk in Pictures/Sigha" ($jpgCount -ge 2 -and $bothNamed) "count=$jpgCount"
    $mediaImages = (Invoke-Adb @("shell", "content", "query", "--uri", "content://media/external/images/media", "--projection", "_display_name:relative_path") | Out-String)
    Add-Step "MediaStore images expose Pictures/Sigha" ($mediaImages -match "Pictures/Sigha") ""

    # 2. The remembered target for the same source category.
    $cleared = Clear-Selection
    Add-Step "selection cleared before memory check" $cleared ""
    $onePicked = Pick-One "img_a.png"
    Add-Step "single image re-picked" $onePicked ""
    $memory = Wait-UiRegex -Pattern "^Convert to JPG$" -TimeoutSec 8
    Add-Step "last JPG target remembered for images" ($null -ne $memory) ""
    Save-Shot "04-remembered-target.png" | Out-Null

    # 3. Failure -> retry -> access lost.
    Clear-Selection | Out-Null
    $brokenPicked = Pick-One "broken.png"
    Add-Step "corrupt image picked" $brokenPicked ""
    if (-not $brokenPicked) { throw "corrupt fixture not picked" }
    Save-Shot "04b-broken-selected.png" | Out-Null
    $action = Wait-UiRegex -Pattern "^Convert to JPG$" -TimeoutSec 8
    Tap-Node $action | Out-Null
    $retry = Wait-ScrolledNode -Text "Retry failed (1)" -TimeoutSec 120
    Add-Step "retry-failed button after a conversion failure" ($null -ne $retry) ""
    Save-Shot "05-retry-button.png" | Out-Null
    if ($retry) {
        Tap-Node $retry | Out-Null
        Start-Sleep -Milliseconds 2000
        $afterRetry = Wait-ScrolledNode -Text "Retry failed (1)" -TimeoutSec 150
        Save-Shot "05c-retry-again.png" | Out-Null
        # The history file records one failed attempt per run, so a second broken.png entry proves
        # the retry re-used the already-picked file even if the button was not caught on screen.
        $historyText = (Invoke-Adb @("shell", "run-as", "com.sigha.converter", "cat", "files/datastore/sigha_history.preferences_pb") | Out-String)
        $brokenAttempts = ([regex]::Matches($historyText, "broken\.png")).Count
        Add-Step "retry re-ran the same file without re-picking" ($brokenAttempts -ge 2) "attempts=$brokenAttempts"
        $retryAgain = $afterRetry
        if (-not $retryAgain) { $retryAgain = Wait-ScrolledNode -Text "Retry failed (1)" -TimeoutSec 30 }
        if ($retryAgain) {
            Invoke-Adb @("shell", "rm", "-f", "/sdcard/Download/broken.png") | Out-Null
            Start-Sleep -Milliseconds 500
            Tap-Node $retryAgain | Out-Null
            $lost = Wait-ScrolledNode -Pattern "opened any more" -TimeoutSec 30
            Add-Step "deleted source reported as FILE_ACCESS_LOST" ($null -ne $lost) ""
            Save-Shot "06-file-access-lost.png" | Out-Null
            # A lost source must not keep advertising a retry action that can never work; the
            # row's own message is the only instruction left.
            $deadRetry = Find-UiNode -Xml (Get-UiXml) -Text "Retry failed (1)"
            Add-Step "lost source offers no dead retry action" ($null -eq $deadRetry) ""
        } else {
            Add-Step "deleted source reported as FILE_ACCESS_LOST" $false "no retry button"
        }
    }

    # 4. Document path: text -> PDF in Download/Sigha.
    Clear-Selection | Out-Null
    $notePicked = Pick-One "note.txt"
    Add-Step "text file picked" $notePicked ""
    $pdfTarget = Tap-Target "PDF"
    Add-Step "PDF target selected" $pdfTarget ""
    $action = Wait-UiRegex -Pattern "^Convert to PDF$" -TimeoutSec 8
    Tap-Node $action | Out-Null
    $saved = Wait-UiRegex -Pattern "saved to Download/Sigha" -TimeoutSec 180
    Add-Step "text conversion reports Download/Sigha" ($null -ne $saved) ""
    $lsDocs = (Invoke-Adb @("shell", "ls", "/sdcard/Download/Sigha") | Out-String)
    Add-Step "note.pdf on disk in Download/Sigha" ($lsDocs -match "note\.pdf") ""

    # 5. Audio path: WAV -> MP3 in Music/Sigha.
    Clear-Selection | Out-Null
    $wavPicked = Pick-One "tone.wav"
    Add-Step "WAV picked" $wavPicked ""
    Save-Shot "05b-audio-targets.png" | Out-Null
    $mp3Target = Tap-Target "MP3"
    Add-Step "MP3 target selected" $mp3Target ""
    $action = Wait-UiRegex -Pattern "^Convert to MP3$" -TimeoutSec 8
    Tap-Node $action | Out-Null
    $saved = Wait-UiRegex -Pattern "saved to Music/Sigha" -TimeoutSec 180
    Add-Step "audio conversion reports Music/Sigha" ($null -ne $saved) ""
    $lsMusic = (Invoke-Adb @("shell", "ls", "/sdcard/Music/Sigha") | Out-String)
    Add-Step "tone.mp3 on disk in Music/Sigha" ($lsMusic -match "tone\.mp3") ""
    $mediaAudio = (Invoke-Adb @("shell", "content", "query", "--uri", "content://media/external/audio/media", "--projection", "_display_name:relative_path") | Out-String)
    Add-Step "MediaStore audio exposes Music/Sigha" ($mediaAudio -match "Music/Sigha") ""

    $permDump = (Invoke-Adb @("shell", "dumpsys", "package", "com.sigha.converter") | Out-String)
    Add-Step "notification permission granted through priming" ($permDump -match "POST_NOTIFICATIONS: granted=true") ""

    # 6. Settings row opens the system notification page.
    $settingsTab = Wait-UiNode -Text "Settings" -TimeoutSec 10
    Tap-Node $settingsTab | Out-Null
    Start-Sleep -Milliseconds 1200
    $notifRow = Wait-UiNode -Text "Completion alerts" -TimeoutSec 10
    Add-Step "settings shows Completion alerts row" ($null -ne $notifRow) ""
    if ($notifRow) {
        Save-Shot "07-settings-notifications.png" | Out-Null
        Tap-Clickable $notifRow | Out-Null
        Start-Sleep -Milliseconds 1800
        $activity = (Invoke-Adb @("shell", "dumpsys", "activity", "activities") | Out-String)
        Add-Step "row opens system notification settings" ($activity -match "com.android.settings") ""
        Invoke-Adb @("shell", "input", "keyevent", "KEYCODE_BACK") | Out-Null
        Start-Sleep -Milliseconds 800
    }

    # 7. A backgrounded batch still posts its completion notification.
    $convertTab = Wait-UiNode -Text "Convert" -TimeoutSec 10
    if ($convertTab) { Tap-Node $convertTab | Out-Null }
    Start-Sleep -Milliseconds 800
    Clear-Selection | Out-Null
    $picked = Pick-Many @("img_a.png", "img_b.png")
    Add-Step "second batch picked" $picked ""
    $jpgTarget = Tap-Target "JPG"
    $action = Wait-UiRegex -Pattern "^Convert 2 files to JPG$" -TimeoutSec 10
    Tap-Node $action | Out-Null
    Invoke-Adb @("shell", "input", "keyevent", "KEYCODE_HOME") | Out-Null
    $deadline = (Get-Date).AddSeconds(180)
    $notification = $false
    while ((Get-Date) -lt $deadline) {
        $dump = (Invoke-Adb @("shell", "dumpsys", "notification", "--noredact") | Out-String)
        if ($dump -match "Conversion finished") { $notification = $true; break }
        Start-Sleep -Milliseconds 1500
    }
    Add-Step "completion notification while backgrounded" $notification ""
    Start-App
    Start-Sleep -Milliseconds 1000
    Save-Shot "08-final.png" | Out-Null

    # 8. A fast-failing retry used to trip the foreground-service contract (startForegroundService
    #    followed by an immediate stop) and kill the process five seconds later; the log buffer
    #    now proves the whole run stayed crash-free.
    $runtimeErrors = (Invoke-Adb @("logcat", "-d", "-s", "AndroidRuntime:E") | Out-String)
    $crashed = $runtimeErrors -match "Process: com\.sigha\.converter"
    $crashDetail = if ($crashed) { ($runtimeErrors -split "`r?`n" | Select-String "FATAL EXCEPTION" | Select-Object -First 1).Line } else { "" }
    Add-Step "no app crash during the run" (-not $crashed) $crashDetail
} catch {
    Add-Step "script aborted" $false $_.Exception.Message
}

$summary.failures = @($script:failures)
$summaryPath = Join-Path $outDir "features.json"
$summary | ConvertTo-Json -Depth 8 | Set-Content $summaryPath -Encoding utf8
Write-Host ("Evidence: " + $summaryPath)
if ($script:failures.Count -gt 0) {
    Write-Host ("FAILURES: " + ($script:failures -join " | "))
    exit 1
}
Write-Host "ALL FEATURE CHECKS PASSED"
