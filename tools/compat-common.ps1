#Requires -Version 7.2
# Shared implementation. No command is issued merely by dot-sourcing this file.
Set-StrictMode -Version Latest

$script:TargetPackage = 'com.huawei.hicreate'
$script:ModulePackage = 'com.orynnx.hicreatehook'

function Invoke-CompatAdb {
    param(
        [Parameter(Mandatory)][string]$Serial,
        [Parameter(Mandatory)][string[]]$Arguments,
        [string]$Adb = 'adb',
        [ValidateRange(1, 90)][int]$TimeoutSeconds = 15
    )
    $process = [System.Diagnostics.Process]::new()
    $process.StartInfo.FileName = $Adb
    $process.StartInfo.UseShellExecute = $false
    $process.StartInfo.CreateNoWindow = $true
    $process.StartInfo.RedirectStandardOutput = $true
    $process.StartInfo.RedirectStandardError = $true
    foreach ($arg in (@('-s', $Serial) + $Arguments)) {
        $process.StartInfo.ArgumentList.Add($arg)
    }
    try {
        if (-not $process.Start()) { throw 'Could not start adb.' }
        $stdout = $process.StandardOutput.ReadToEndAsync()
        $stderr = $process.StandardError.ReadToEndAsync()
        if (-not $process.WaitForExit($TimeoutSeconds * 1000)) {
            $process.Kill($true)
            # Termination must not turn a command timeout into an unbounded wait.
            [void]$process.WaitForExit(1000)
            return [pscustomobject]@{ ok = $false; exitCode = $null; output = ''; error = 'timeout' }
        }
        return [pscustomobject]@{
            ok = ($process.ExitCode -eq 0)
            exitCode = $process.ExitCode
            output = $stdout.GetAwaiter().GetResult().Trim()
            error = $stderr.GetAwaiter().GetResult().Trim()
        }
    } catch {
        # Do not place complete command lines or arbitrary tool output in reports.
        return [pscustomobject]@{ ok = $false; exitCode = $null; output = ''; error = $_.Exception.GetType().Name }
    } finally {
        $process.Dispose()
    }
}

function Get-CompatFailure {
    param($Result)
    $text = $Result.output + ' ' + $Result.error
    if ($text -match 'unauthorized') { return 'unauthorized' }
    if ($text -match 'offline') { return 'offline' }
    if ($text -match 'not found|no devices') { return 'device-not-found' }
    if ($text -match 'timeout') { return 'timeout' }
    return 'command-failed'
}

function Get-CompatActivity {
    param([string]$Text)
    $resumed = @()
    foreach ($line in ($Text -split '\r?\n')) {
        if ($line -match '(?:mResumedActivity|topResumedActivity|ResumedActivity).*?\s([\w.$]+/[\w.$]+)(?:\s|\})') {
            $resumed += $Matches[1]
        }
    }
    return @($resumed | Select-Object -Unique)
}

function ConvertTo-CompatCrashSummary {
    param([string]$Text)
    $lines = $Text -split '\r?\n'
    $events = [System.Collections.Generic.List[object]]::new()
    $current = @{}
    foreach ($line in $lines) {
        if ($line -notmatch '^(\d\d-\d\d\s+\d\d:\d\d:\d\d\.\d+)\s+(\d+)\s+\d+\s+\w\s+(\S+)\s*:\s?(.*)$') { continue }
        $stamp = $Matches[1]; $processId = $Matches[2]; $tag = $Matches[3]; $message = $Matches[4]
        if ($tag -eq 'AndroidRuntime' -and $message -match '^FATAL EXCEPTION:') {
            $current[$processId] = [ordered]@{ time = $stamp; pid = [int]$processId; kind = 'java'; target = $false; exceptionTypes = @(); frames = @() }
        }
        if ($tag -eq 'AndroidRuntime' -and $message -match '^Process:\s+com\.huawei\.hicreate(?::[\w.]+)?,') {
            if (-not $current.ContainsKey($processId)) {
                $current[$processId] = [ordered]@{ time = $stamp; pid = [int]$processId; kind = 'java'; target = $false; exceptionTypes = @(); frames = @() }
            }
            $current[$processId].target = $true
            $events.Add($current[$processId])
        }
        if ($tag -eq 'AndroidRuntime' -and $current.ContainsKey($processId)) {
            $entry = $current[$processId]
            # Messages can contain document names, URLs or account data. Keep only types and code locations.
            if ($message -match '^\s*(?:Caused by:\s*)?([\w.$]+(?:Exception|Error))(?::|\s|$)') {
                $entry.exceptionTypes += $Matches[1]
            }
            if ($message -match '^\s*at\s+([\w.$<>]+\([\w.$ :+-]*\))' -and $entry.frames.Count -lt 40) {
                $entry.frames += $Matches[1]
            }
        }
        # Native crash details outside Cmdline can include arbitrary memory. Record only the target marker.
        if ($tag -eq 'DEBUG' -and $message -match '^Cmdline:\s+com\.huawei\.hicreate(?::[\w.]+)?\s*$') {
            $events.Add([ordered]@{ time = $stamp; kind = 'native'; target = $true })
        }
    }
    return @($events.ToArray())
}

function Get-CompatReport {
    param([string]$Serial, [string]$Adb = 'adb', [string]$CrashSince = '')
    $report = [ordered]@{
        schemaVersion = 1
        collectedAtUtc = [DateTime]::UtcNow.ToString('o')
        serial = $Serial
        deviceStatus = 'unavailable'
        properties = [ordered]@{}
        packages = [ordered]@{}
        runtime = [ordered]@{}
        nativeCache = [ordered]@{ status = 'unverified' }
        crashes = @()
        exitHistory = @()
        failures = @()
        limits = @('No artwork, account data, raw exception messages or full device properties collected.',
            'Cache manifest and memory mapping do not prove drawing, save, export or full native integrity.',
            'Historical crashes are evidence, not necessarily failures of the current module version.')
    }
    $state = Invoke-CompatAdb -Serial $Serial -Adb $Adb -Arguments @('get-state')
    if (-not $state.ok -or $state.output -ne 'device') {
        $report.deviceStatus = Get-CompatFailure $state
        $report.failures += 'device-connection'
        return $report
    }
    $report.deviceStatus = 'device'
    $properties = @('ro.product.manufacturer', 'ro.product.model', 'ro.build.version.release',
        'ro.build.version.sdk', 'ro.product.cpu.abilist', 'ro.dalvik.vm.native.bridge',
        'ro.miui.ui.version.name', 'ro.mi.os.version.name', 'ro.build.version.oneui',
        'ro.build.version.oplusrom', 'ro.vivo.os.version', 'ro.build.version.emui')
    foreach ($key in $properties) {
        $r = Invoke-CompatAdb -Serial $Serial -Adb $Adb -Arguments @('shell', 'getprop', $key)
        if ($r.ok) { $report.properties[$key] = $r.output }
        else { $report.failures += ('property:' + $key) }
    }
    $pages = Invoke-CompatAdb -Serial $Serial -Adb $Adb -Arguments @('shell', 'getconf PAGESIZE')
    if ($pages.ok -and $pages.output -match '^\d+$') { $report.properties['pageSizeBytes'] = [int]$pages.output }
    else { $report.failures += 'page-size' }
    foreach ($package in @($script:TargetPackage, $script:ModulePackage, 'com.huawei.hwid', 'org.lsposed.manager')) {
        $r = Invoke-CompatAdb -Serial $Serial -Adb $Adb -Arguments @('shell', 'dumpsys', 'package', $package)
        $info = [ordered]@{ installed = $false; versionName = $null; versionCode = $null }
        if ($r.ok -and $r.output -match 'versionCode=(\d+)') {
            $info.installed = $true; $info.versionCode = [long]$Matches[1]
            if ($r.output -match 'versionName=([^\r\n]+)') { $info.versionName = $Matches[1].Trim() }
        }
        $report.packages[$package] = $info
        if (-not $info.installed -and $package -ne 'org.lsposed.manager') { $report.failures += ('package:' + $package) }
    }
    $pids = Invoke-CompatAdb -Serial $Serial -Adb $Adb -Arguments @('shell', 'pidof', $script:TargetPackage)
    $report.runtime['mainPids'] = @()
    if ($pids.ok -and $pids.output -match '^\d+(?:\s+\d+)*$') {
        $report.runtime.mainPids = @($pids.output -split '\s+' | ForEach-Object { [int]$_ })
    }
    $activity = Invoke-CompatAdb -Serial $Serial -Adb $Adb -Arguments @('shell', 'dumpsys activity activities')
    $report.runtime['resumedActivities'] = @(Get-CompatActivity $activity.output)
    $report.runtime['targetResumed'] = @($report.runtime.resumedActivities | Where-Object { $_ -like 'com.huawei.hicreate/*' }).Count -gt 0
    if (-not $activity.ok) { $report.failures += 'activity-inspection' }
    $root = Invoke-CompatAdb -Serial $Serial -Adb $Adb -Arguments @('shell', "su -c 'id -u'") -TimeoutSeconds 8
    $report.runtime['rootAvailable'] = $root.ok -and $root.output -eq '0'
    if ($report.runtime.rootAvailable) {
        $manifest = Invoke-CompatAdb -Serial $Serial -Adb $Adb -Arguments @('shell', "su -c 'cat /data/user/0/com.huawei.hicreate/files/native/manifest.json'")
        $library = Invoke-CompatAdb -Serial $Serial -Adb $Adb -Arguments @('shell', "su -c 'stat -c %s /data/user/0/com.huawei.hicreate/files/native/libnativelib.so'")
        if ($manifest.ok -and $library.ok -and $library.output -match '^\d+$') {
            try {
                $json = $manifest.output | ConvertFrom-Json -AsHashtable
                if (-not $json.ContainsKey('patched') -or $json.patched -notmatch '^[a-fA-F0-9]{64}$') { throw 'Invalid manifest' }
                $report.nativeCache = [ordered]@{ status = 'present'; libraryBytes = [long]$library.output; declaredPatchedSha256 = $json.patched; mappedInMainProcess = $false }
                foreach ($targetPid in $report.runtime.mainPids) {
                    $mapped = Invoke-CompatAdb -Serial $Serial -Adb $Adb -Arguments @('shell', "su -c 'grep -q /com.huawei.hicreate/files/native/libnativelib.so /proc/$targetPid/maps'")
                    if ($mapped.ok) { $report.nativeCache.mappedInMainProcess = $true }
                }
                if ($report.runtime.mainPids.Count -gt 0 -and -not $report.nativeCache.mappedInMainProcess) {
                    $report.failures += 'patched-library-mapping-unverified'
                }
            } catch { $report.nativeCache.status = 'invalid-manifest'; $report.failures += 'native-manifest' }
        } else { $report.nativeCache.status = 'missing-or-unreadable'; $report.failures += 'native-cache' }
    } else { $report.nativeCache.status = 'root-unavailable'; $report.failures += 'native-inspection-root-unavailable' }
    $logArgs = @('logcat', '-b', 'crash', '-d', '-v', 'threadtime')
    if ($CrashSince) { $logArgs += @('-T', $CrashSince) } else { $logArgs += @('-t', '1200') }
    $crash = Invoke-CompatAdb -Serial $Serial -Adb $Adb -Arguments $logArgs
    if ($crash.ok) { $report.crashes = @(ConvertTo-CompatCrashSummary $crash.output) }
    else { $report.failures += 'crash-buffer-unavailable' }
    $exits = Invoke-CompatAdb -Serial $Serial -Adb $Adb -Arguments @('shell', 'dumpsys activity exit-info com.huawei.hicreate')
    if ($exits.ok) {
        # Exit records can put a free-text description on the same line as numeric fields.
        # Parse only an explicit allowlist, never copy complete dumpsys lines.
        $exitRecords = [System.Collections.Generic.List[object]]::new()
        $exitEntry = $null
        foreach ($line in ($exits.output -split '\r?\n')) {
            if ($line -match '^\s*ApplicationExitInfo #') {
                $exitEntry = [ordered]@{}
                $exitRecords.Add($exitEntry)
            }
            if ($null -eq $exitEntry) { continue }
            foreach ($key in @('pid', 'reason', 'status', 'importance', 'pss', 'rss')) {
                if ($line -match ('(?:^|\s)' + $key + '=(\d+)')) { $exitEntry[$key] = [long]$Matches[1] }
            }
            if ($line -match '(?:^|\s)timestamp=(\d{4}-\d\d-\d\d\s+\d\d:\d\d:\d\d(?:\.\d+)?)') {
                $exitEntry['timestamp'] = $Matches[1]
            }
        }
        $report.exitHistory = @($exitRecords.ToArray())
    } else { $report.failures += 'exit-history-unavailable' }
    return $report
}

function Write-CompatJson {
    param([Parameter(Mandatory)]$Report, [Parameter(Mandatory)][string]$Path)
    $fullPath = [IO.Path]::GetFullPath($Path)
    [IO.Directory]::CreateDirectory([IO.Path]::GetDirectoryName($fullPath)) | Out-Null
    [IO.File]::WriteAllText($fullPath, ($Report | ConvertTo-Json -Depth 12), [Text.UTF8Encoding]::new($false))
    return $fullPath
}
