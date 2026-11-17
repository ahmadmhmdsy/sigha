#requires -Version 7.0
<#
  Sigha verification script - the single source of truth for build/artefact facts.
  Usage:
    pwsh -File scripts/verify.ps1              # full: clean build, tests, APK facts, release, lint
    pwsh -File scripts/verify.ps1 -Quick       # debug build + facts only
    pwsh -File scripts/verify.ps1 -SkipBuild   # facts about existing artefacts only
  Writes docs/qa/evidence/verify-<timestamp>.json and prints a summary.
#>
[CmdletBinding()]
param(
    [switch]$Quick,
    [switch]$SkipBuild
)

$ErrorActionPreference = "Stop"
$root = Split-Path -Parent $PSScriptRoot
Set-Location $root

# Gradle needs a JDK. A background/scheduled run may not inherit an interactive shell's env,
# so fall back to the JDK this machine has installed instead of failing with exit 9009.
if (-not $env:JAVA_HOME -or -not (Test-Path (Join-Path $env:JAVA_HOME "bin\java.exe"))) {
    $jdkCandidates = @(
        "C:\Program Files\Eclipse Adoptium\jdk-21.0.13.9-hotspot",
        "C:\Program Files\Android\Android Studio\jbr"
    )
    $adoptium = Join-Path $env:ProgramFiles "Eclipse Adoptium"
    if (Test-Path $adoptium) {
        $jdkCandidates += @(Get-ChildItem $adoptium -Directory | Sort-Object Name -Descending | ForEach-Object { $_.FullName })
    }
    $jdk = $jdkCandidates | Where-Object { Test-Path (Join-Path $_ "bin\java.exe") } | Select-Object -First 1
    if ($jdk) {
        $env:JAVA_HOME = $jdk
        Write-Host "JAVA_HOME=$jdk"
    } else {
        Write-Host "WARNING: no JDK found; Gradle steps will fail" -ForegroundColor Yellow
    }
}
$evidenceDir = Join-Path $root "docs/qa/evidence"
New-Item -ItemType Directory -Force -Path $evidenceDir | Out-Null
$stamp = Get-Date -Format "yyyyMMdd-HHmmss"
$summary = [ordered]@{ generatedAt = (Get-Date).ToString("o"); root = $root; steps = @(); facts = [ordered]@{}; failures = @() }

function Add-Step([string]$name, [bool]$ok, [string]$detail = "") {
    $summary.steps += [ordered]@{ step = $name; ok = $ok; detail = $detail }
    $mark = if ($ok) { "PASS" } else { "FAIL" }
    Write-Host ("[{0}] {1} {2}" -f $mark, $name, $detail)
    if (-not $ok) { $summary.failures += "$name $detail" }
}

# A Gradle/Kotlin daemon or lint worker left over from an earlier run can hold files under
# app/build. On Windows that makes `:app:clean` fail with "Unable to delete directory", and the
# next lint run then trips over the same locked file. Clear them before building.
function Stop-StaleBuildProcesses {
    & (Join-Path $root "gradlew.bat") --stop 2>&1 | Out-Null
    Start-Sleep -Seconds 2
    Get-CimInstance Win32_Process -Filter "Name='java.exe'" -ErrorAction SilentlyContinue |
        Where-Object { $_.CommandLine -match "kotlin-daemon|GradleDaemon|digital_converter" } |
        ForEach-Object { Stop-Process -Id $_.ProcessId -Force -ErrorAction SilentlyContinue }
    Start-Sleep -Seconds 1
}

function Invoke-Gradle([string]$tasks) {
    $gradleArgs = @($tasks.Split(" ", [StringSplitOptions]::RemoveEmptyEntries)) + @("--no-build-cache", "--no-daemon", "--console=plain")
    & (Join-Path $root "gradlew.bat") @gradleArgs 2>&1 | Tee-Object -Variable gradleOut | Out-Host
    return [ordered]@{ exit = $LASTEXITCODE; output = ($gradleOut -join [Environment]::NewLine) }
}

function Get-ZipEntries([string]$apk) {
    Add-Type -AssemblyName System.IO.Compression.FileSystem -ErrorAction SilentlyContinue
    $zip = [System.IO.Compression.ZipFile]::OpenRead($apk)
    try {
        $rows = foreach ($e in $zip.Entries) {
            [pscustomobject]@{ name = $e.FullName; length = $e.Length; compressed = $e.CompressedLength; stored = ($e.CompressedLength -eq $e.Length) }
        }
        return @($rows)
    } finally { $zip.Dispose() }
}

function Get-ElfMinAlign([byte[]]$bytes) {
    if ($bytes.Length -lt 64 -or $bytes[0] -ne 0x7F -or $bytes[1] -ne 0x45 -or $bytes[2] -ne 0x4C -or $bytes[3] -ne 0x46) { return $null }
    $class = $bytes[4]; $data = $bytes[5]
    if ($class -ne 2 -or $data -ne 1) { return $null }
    $phoff = [BitConverter]::ToUInt64($bytes, 32)
    $phentsize = [BitConverter]::ToUInt16($bytes, 54)
    $phnum = [BitConverter]::ToUInt16($bytes, 56)
    $min = [uint64]::MaxValue
    for ($i = 0; $i -lt $phnum; $i++) {
        $off = [int]($phoff + $i * $phentsize)
        if ($off + 56 -gt $bytes.Length) { return $null }
        $type = [BitConverter]::ToUInt32($bytes, $off)
        if ($type -eq 1) {
            $align = [BitConverter]::ToUInt64($bytes, $off + 48)
            if ($align -lt $min) { $min = $align }
        }
    }
    if ($min -eq [uint64]::MaxValue) { return $null }
    return $min
}

function Get-ApkFacts([string]$apk) {
    $entries = Get-ZipEntries $apk
    $so = @($entries | Where-Object { $_.name -like "lib/*/*.so" })
    $stored = @($so | Where-Object { $_.stored }).Count
    $deflated = $so.Count - $stored
    $aligns = @()
    $zip = [System.IO.Compression.ZipFile]::OpenRead($apk)
    try {
        foreach ($e in $zip.Entries | Where-Object { $_.FullName -like "lib/arm64-v8a/*.so" }) {
            $ms = New-Object System.IO.MemoryStream
            $e.Open().CopyTo($ms)
            $a = Get-ElfMinAlign $ms.ToArray()
            $ms.Dispose()
            if ($null -ne $a) { $aligns += [pscustomobject]@{ name = $e.Name; align = $a } }
        }
    } finally { $zip.Dispose() }
    $minAlign = if ($aligns.Count -gt 0) { ($aligns | Measure-Object align -Minimum).Minimum } else { $null }
    return [ordered]@{
        path = $apk
        sizeBytes = (Get-Item $apk).Length
        sha256 = (Get-FileHash -Algorithm SHA256 $apk).Hash
        soEntries = $so.Count
        soStored = $stored
        soDeflated = $deflated
        elfArm64Checked = $aligns.Count
        elfMinPAlign = $minAlign
    }
}

function Find-BuildTool([string]$exe) {
    $sdk = if ($env:ANDROID_HOME) { $env:ANDROID_HOME } else { "C:\Android\Sdk" }
    $dirs = Get-ChildItem (Join-Path $sdk "build-tools") -Directory -ErrorAction SilentlyContinue | Sort-Object Name -Descending
    foreach ($d in $dirs) {
        $p = Join-Path $d.FullName $exe
        if (Test-Path $p) { return $p }
    }
    return $null
}

# ---------------------------------------------------------------- build + tests
if (-not $SkipBuild) {
    Stop-StaleBuildProcesses
    $r = Invoke-Gradle ":app:clean :app:testDebugUnitTest :app:assembleDebug"
    if ($r.exit -ne 0) {
        # One retry: a just-killed daemon can keep a file locked for a few more seconds.
        Stop-StaleBuildProcesses
        $r = Invoke-Gradle ":app:clean :app:testDebugUnitTest :app:assembleDebug"
    }
    Add-Step "gradle clean+test+assembleDebug" ($r.exit -eq 0) "exit=$($r.exit)"
}
# ABI splits produce one APK per ABI plus a universal APK; measure the universal one.
$debugDir = Join-Path $root "app/build/outputs/apk/debug"
$debugApks = @(Get-ChildItem $debugDir -Filter *.apk -ErrorAction SilentlyContinue | Sort-Object Name)
$apkItem = $debugApks | Where-Object { $_.Name -like "*universal*" } | Select-Object -First 1
if (-not $apkItem -and $debugApks.Count -gt 0) { $apkItem = $debugApks | Sort-Object Length -Descending | Select-Object -First 1 }
$apk = if ($apkItem) { $apkItem.FullName } else { Join-Path $debugDir "app-debug.apk" }

$results = Join-Path $root "app/build/test-results/testDebugUnitTest"
if (Test-Path $results) {
    $xml = Get-ChildItem $results -Filter *.xml -ErrorAction SilentlyContinue
    $tests = 0; $fail = 0; $err = 0; $skip = 0
    foreach ($f in $xml) {
        try {
            $doc = [xml](Get-Content $f.FullName -Raw)
            $tests += [int]$doc.testsuite.tests; $fail += [int]$doc.testsuite.failures; $err += [int]$doc.testsuite.errors; $skip += [int]$doc.testsuite.skipped
        } catch { }
    }
    $summary.facts.unitTests = [ordered]@{ classes = $xml.Count; tests = $tests; failures = $fail; errors = $err; skipped = $skip }
    Add-Step "unit tests" (($fail + $err) -eq 0 -and $tests -gt 0) "tests=$tests failures=$fail errors=$err"
} else {
    Add-Step "unit tests" $false "no test-results directory"
}

# ---------------------------------------------------------------- debug APK facts
if (Test-Path $apk) {
    $facts = Get-ApkFacts $apk
    $summary.facts.debugApk = $facts
    $summary.facts.debugApks = @($debugApks | ForEach-Object { [ordered]@{ name = $_.Name; sizeBytes = $_.Length; sha256 = (Get-FileHash -Algorithm SHA256 $_.FullName).Hash } })
    Add-Step "debug APK exists" $true "$($facts.sizeBytes) B $($facts.sha256) ($($apkItem.Name))"
    $splitText = ($debugApks | ForEach-Object { $_.Name + "=" + $_.Length }) -join ", "
    Add-Step "debug APK splits" ($debugApks.Count -ge 2) $splitText
    Add-Step "native libs stored (uncompressed)" ($facts.soEntries -gt 0 -and $facts.soDeflated -eq 0) "so=$($facts.soEntries) stored=$($facts.soStored) deflated=$($facts.soDeflated)"
    Add-Step "ELF 16 KB page alignment (arm64)" ($null -ne $facts.elfMinPAlign -and $facts.elfMinPAlign -ge 16384) "checked=$($facts.elfArm64Checked) minPAlign=$($facts.elfMinPAlign)"
    $zipalign = Find-BuildTool "zipalign.exe"
    if ($zipalign) {
        $out = & $zipalign -c -P 16 -v 4 $apk 2>&1
        Add-Step "zipalign -c -P 16" ($LASTEXITCODE -eq 0) "exit=$LASTEXITCODE"
    } else { Add-Step "zipalign -c -P 16" $false "zipalign not found" }
} else {
    Add-Step "debug APK exists" $false "missing $apk"
}

# ---------------------------------------------------------------- release artefacts
# -SkipBuild measures the artefacts that are already there; it must not trigger a release build.
if (-not $SkipBuild -and -not $Quick) {
    # Two invocations on purpose: AGP 8.9+ fails bundleRelease while ABI splits are enabled,
    # so the build script enables splits only when the requested tasks do not contain "bundle".
    $r = Invoke-Gradle ":app:assembleRelease"
    Add-Step "gradle assembleRelease" ($r.exit -eq 0) "exit=$($r.exit)"
    $r = Invoke-Gradle ":app:bundleRelease"
    Add-Step "gradle bundleRelease" ($r.exit -eq 0) "exit=$($r.exit)"
    $r = Invoke-Gradle ":app:lintDebug"
    Add-Step "gradle lintDebug" ($r.exit -eq 0) "exit=$($r.exit)"
}

if (-not $Quick) {
    $relApks = @(Get-ChildItem (Join-Path $root "app/build/outputs/apk/release") -Filter *.apk -ErrorAction SilentlyContinue)
    if ($relApks.Count -gt 0) {
        $summary.facts.releaseApks = @($relApks | ForEach-Object { [ordered]@{ name = $_.Name; sizeBytes = $_.Length; sha256 = (Get-FileHash -Algorithm SHA256 $_.FullName).Hash } })
        Add-Step "release APKs" $true ($relApks.Name -join ", ")
        $apksigner = Find-BuildTool "apksigner.bat"
        if ($apksigner) {
            $unsigned = @()
            foreach ($rel in $relApks) {
                $verify = & $apksigner verify --print-certs $rel.FullName 2>&1
                if ($LASTEXITCODE -ne 0) { $unsigned += $rel.Name }
            }
            Add-Step "release APK signatures" ($unsigned.Count -eq 0) ("unsigned: " + ($unsigned -join ", "))
        }
    } else { Add-Step "release APKs" $false "none produced" }
    $aabs = @(Get-ChildItem (Join-Path $root "app/build/outputs/bundle/release") -Filter *.aab -ErrorAction SilentlyContinue)
    if ($aabs.Count -gt 0) {
        $summary.facts.releaseAab = @($aabs | ForEach-Object { [ordered]@{ name = $_.Name; sizeBytes = $_.Length; sha256 = (Get-FileHash -Algorithm SHA256 $_.FullName).Hash } })
        Add-Step "release AAB" $true ($aabs.Name -join ", ")
    } else { Add-Step "release AAB" $false "none produced" }

    $lintTxt = Join-Path $root "app/build/reports/lint-results-debug.txt"
    if (Test-Path $lintTxt) {
        $text = Get-Content $lintTxt -Raw
        $errors = ([regex]::Matches($text, "(?m)^.*: Error:")).Count
        $warnings = ([regex]::Matches($text, "(?m)^.*: Warning:")).Count
        $summary.facts.lint = [ordered]@{ errors = $errors; warnings = $warnings; report = $lintTxt }
        Add-Step "lint error count" ($errors -eq 0) "errors=$errors warnings=$warnings"
    }
}

# ---------------------------------------------------------------- claim drift check
$stalePatterns = @("72,522,602", "72522602", "8922614F", "8922614f", "32 converter edges", "12 families", "extractNativeLibs", "fully offline", "Coming soon")
$drift = @()
foreach ($doc in @("README.md", "PRODUCT.md", "docs/qa/verification.md")) {
    $p = Join-Path $root $doc
    if (Test-Path $p) {
        $lines = Get-Content $p
        foreach ($pat in $stalePatterns) {
            foreach ($line in $lines) {
                if (-not $line.Contains($pat)) { continue }
                # A line that forbids a claim is not itself the claim.
                if ($line -match "may not say|must not say|never claim|do not claim|not claim|prohibited|avoid") { continue }
                $drift += "$doc contains stale claim '$pat': $($line.Trim())"
            }
        }
    }
}
$summary.facts.claimDrift = $drift
Add-Step "claim drift (no stale numbers in public docs)" ($drift.Count -eq 0) ($drift -join "; ")

# ---------------------------------------------------------------- write evidence
$json = $summary | ConvertTo-Json -Depth 8
$outFile = Join-Path $evidenceDir "verify-$stamp.json"
$json | Set-Content -Path $outFile -Encoding utf8
Write-Host ""
Write-Host "Evidence written: $outFile"
if ($summary.failures.Count -gt 0) {
    Write-Host ("FAILURES: " + ($summary.failures -join " | ")) -ForegroundColor Red
    exit 1
}
Write-Host "ALL CHECKS PASSED" -ForegroundColor Green
exit 0
