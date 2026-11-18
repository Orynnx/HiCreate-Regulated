#Requires -Version 7.2
<#
.SYNOPSIS
Collect a bounded, sanitized HiCreate compatibility report from an explicitly selected device.
.EXAMPLE
pwsh ./tools/collect-report.ps1 -Serial DEVICE_ID -OutputDir ./reports/device
.NOTES
Read-only on the device. Root may prompt on the device; the attempt times out after eight seconds.
Exit 0: collection completed. Exit 2: some required observations were unavailable (see failures).
#>
[CmdletBinding()]
param(
    [Parameter(Mandatory)][ValidateNotNullOrEmpty()][string]$Serial,
    [Parameter(Mandatory)][ValidateNotNullOrEmpty()][string]$OutputDir,
    [string]$Adb = 'adb'
)
$ErrorActionPreference = 'Stop'
. "$PSScriptRoot/compat-common.ps1"
$report = Get-CompatReport -Serial $Serial -Adb $Adb
$path = Write-CompatJson -Report $report -Path (Join-Path $OutputDir 'compatibility-report.json')
Write-Output $path
if ($report.failures.Count -gt 0) {
    Write-Warning ('Incomplete observations: ' + ($report.failures -join ', '))
    exit 2
}
exit 0
