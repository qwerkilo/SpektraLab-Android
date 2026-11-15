<#
.SYNOPSIS
Taps an Android device only while the expected app owns the foreground.

.DESCRIPTION
Scripted tap sequences keep landing in another app after a notification or a
manual app switch. This wrapper reads mCurrentFocus first and refuses to tap
when the foreground is not the expected package, so a stale sequence cannot
type into a shopping app or a settings screen.

.EXAMPLE
.\scripts\device-tap.ps1 -Serial 10.110.67.69:37713 -X 624 -Y 1748
#>
param(
    [Parameter(Mandatory = $true)][string]$Serial,
    [Parameter(Mandatory = $true)][int]$X,
    [Parameter(Mandatory = $true)][int]$Y,
    [string]$Package = 'app.spektralab',
    [string]$Adb = 'F:\android-sdk\platform-tools\adb.exe'
)

$ErrorActionPreference = 'Stop'

$windowDump = & $Adb -s $Serial shell dumpsys window
$focusLine = $windowDump |
    Select-String -Pattern 'mFocusedApp' |
    Select-Object -First 1
if (-not $focusLine) {
    $focusLine = $windowDump |
        Select-String -Pattern 'mCurrentFocus' |
        Select-Object -First 1
}

if (-not $focusLine) {
    Write-Error "could not read the foreground app from $Serial"
    exit 1
}

# mFocusedApp names the owning package even when a dialog window is focused,
# so an in-app BottomSheet does not read as a foreign app.
if ($focusLine.Line -notmatch [regex]::Escape($Package)) {
    Write-Error "foreground is not ${Package}: $($focusLine.Line.Trim())"
    exit 1
}

& $Adb -s $Serial shell input tap $X $Y
exit $LASTEXITCODE
