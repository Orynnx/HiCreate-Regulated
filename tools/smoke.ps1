#Requires -Version 7.2
<#
.SYNOPSIS
Launch the existing HiCreate app and check bounded startup signals. Never installs or clears data.
.EXAMPLE
pwsh ./tools/smoke.ps1 -Serial DEVICE_ID -OutputDir ./reports/device -WaitSeconds 12
.NOTES
Does not force-stop a running drawing session. Close/save work before a cold-start test.
STARTUP_PASS verifies process, foreground and crash signals only, not drawing or save/export.
Exit 0: STARTUP_PASS. Exit 1: startup failed. Exit 2: checks are incomplete.
#>
[CmdletBinding()]
param(
    [Parameter(Mandatory)][ValidateNotNullOrEmpty()][string]$Serial,
    [Parameter(Mandatory)][ValidateNotNullOrEmpty()][string]$OutputDir,
    [ValidateRange(3, 45)][int]$WaitSeconds = 12,
    [string]$Adb = 'adb'
)
$ErrorActionPreference = 'Stop'
. "$PSScriptRoot/compat-common.ps1"
$state = Invoke-CompatAdb -Serial $Serial -Adb $Adb -Arguments @('get-state')
if (-not $state.ok -or $state.output -ne 'device') {
    $report = Get-CompatReport -Serial $Serial -Adb $Adb
    $report['smoke'] = [ordered]@{ result = 'INCOMPLETE'; reason = 'device-unavailable'; drawingTested = $false }
    Write-Output (Write-CompatJson -Report $report -Path (Join-Path $OutputDir 'smoke-report.json'))
    exit 2
}
$clock = Invoke-CompatAdb -Serial $Serial -Adb $Adb -Arguments @('shell', "date '+%m-%d %H:%M:%S.000'")
$since = ''
if ($clock.ok -and $clock.output -match '^\d\d-\d\d \d\d:\d\d:\d\d\.\d{3}$') { $since = $clock.output }
$launch = Invoke-CompatAdb -Serial $Serial -Adb $Adb -Arguments @('shell', 'am start -W -n com.huawei.hicreate/.app.SplashActivity') -TimeoutSeconds 30
$started = $launch.ok -and $launch.output -notmatch '(?m)^Error|Exception|Permission Denial'
if ($started) { Start-Sleep -Seconds $WaitSeconds }
$report = Get-CompatReport -Serial $Serial -Adb $Adb -CrashSince $since
$processAlive = $report.runtime.Contains('mainPids') -and $report.runtime.mainPids.Count -gt 0
$foreground = $report.runtime.Contains('targetResumed') -and $report.runtime.targetResumed
$result = 'INCOMPLETE'
if (-not $started -or -not $processAlive -or -not $foreground) { $result = 'STARTUP_FAIL' }
elseif ($since -and $report.crashes.Count -gt 0) { $result = 'STARTUP_FAIL' }
elseif ($since -and $report.failures.Count -eq 0) { $result = 'STARTUP_PASS' }
$report['smoke'] = [ordered]@{
    result = $result
    launchCommandSucceeded = $started
    mainProcessAlive = $processAlive
    targetResumed = $foreground
    crashWindowStartDeviceTime = $since
    waitSeconds = $WaitSeconds
    coldStartEnforced = $false
    drawingTested = $false
    saveExportTested = $false
    acceptance = 'Startup signals only; run actual drawing, undo, save/reopen and export acceptance separately.'
}
if (-not $since) { $report.failures += 'crash-window-unavailable' }
Write-Output (Write-CompatJson -Report $report -Path (Join-Path $OutputDir 'smoke-report.json'))
Write-Output $result
if ($result -eq 'STARTUP_PASS') { exit 0 }
if ($result -eq 'STARTUP_FAIL') { exit 1 }
exit 2
