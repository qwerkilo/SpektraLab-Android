<#
.SYNOPSIS
  Build the Android APK on Windows without the Termux toolchain.

.DESCRIPTION
  Compiles the app's Java, dexes it with the R8 jar (build-tools 34's d8
  crashes on MainActivity$1.class), compiles the manifest and theme with
  aapt, reuses the assets and native libraries from a base APK, then
  zipaligns and signs.

  The base APK supplies the ~30 MB of profiles, resources, and lib/*.so.
  It must be a previously built SpektraLab console APK with the same
  package and a compatible native ABI.

.EXAMPLE
  $env:SPK_KEYSTORE = 'G:\path\qwerkilo.jks'
  $env:SPK_KS_PASS = '...'
  $env:SPK_KEY_PASS = '...'
  $env:SPK_KEY_ALIAS = 'qwerkilo'
  ./app/build/build-apk-windows.ps1
#>
[CmdletBinding()]
param(
    [string]$BaseApk = '',
    [string]$Output = '',
    [string]$Sdk = '',
    [string]$Jdk = '',
    [string]$Jdk17 = '',
    [string]$Keystore = $env:SPK_KEYSTORE,
    [string]$KsPass = $env:SPK_KS_PASS,
    [string]$KeyPass = $env:SPK_KEY_PASS,
    [string]$Alias = $env:SPK_KEY_ALIAS,
    [string]$Python = 'python'
)

$ErrorActionPreference = 'Stop'
$repo = (Resolve-Path (Join-Path $PSScriptRoot '..\..')).Path
$app = Join-Path $repo 'app'
$manifest = Join-Path $app 'src\main\AndroidManifest.xml'
$res = Join-Path $app 'src\main\res'
$sourcesDir = Join-Path $app 'src\main\java'
$buildDir = Join-Path $app 'build'

function Find-Sdk {
    if ($Sdk) { return $Sdk }
    if ($env:ANDROID_SDK_ROOT) { return $env:ANDROID_SDK_ROOT }
    if ($env:ANDROID_HOME) { return $env:ANDROID_HOME }
    foreach ($candidate in @('F:\android-sdk', "$HOME\Android\Sdk")) {
        if (Test-Path $candidate) { return $candidate }
    }
    throw 'Android SDK not found. Pass -Sdk or set ANDROID_SDK_ROOT.'
}

function Find-BuildTools([string]$sdkRoot) {
    $dir = Join-Path $sdkRoot 'build-tools'
    if (-not (Test-Path $dir)) { throw "No build-tools under $dir" }
    $v = Get-ChildItem $dir -Directory | Sort-Object { [version]$_.Name } -Descending | Select-Object -First 1
    if (-not $v) { throw "No build-tools under $dir" }
    return $v.FullName
}

function Find-Jdk([string]$explicit, [string]$fallback) {
    if ($explicit) { return $explicit }
    if ($env:JAVA_HOME) { return $env:JAVA_HOME }
    if (Test-Path $fallback) { return $fallback }
    throw 'JDK not found. Pass -Jdk or set JAVA_HOME.'
}

$sdkRoot = Find-Sdk
$buildTools = Find-BuildTools $sdkRoot
$jdkRoot = Find-Jdk $Jdk 'F:\JDK\JDK21'
$jdk17Root = Find-Jdk $Jdk17 'F:\tools\jdk-17.0.20.1+1'

$javac = Join-Path $jdkRoot 'bin\javac.exe'
$androidJar = Join-Path $sdkRoot 'platforms\android-34\android.jar'
$aapt = Join-Path $buildTools 'aapt.exe'
$zipalign = Join-Path $buildTools 'zipalign.exe'
$apksigner = Join-Path $buildTools 'apksigner.bat'
$r8Jar = Join-Path $sdkRoot 'cmdline-tools\latest\lib\r8.jar'
$jdk17Java = Join-Path $jdk17Root 'bin\java.exe'

foreach ($p in @($javac, $androidJar, $aapt, $zipalign, $apksigner, $r8Jar, $jdk17Java)) {
    if (-not (Test-Path $p)) { throw "Missing build tool: $p" }
}
if (-not $Keystore -or -not (Test-Path $Keystore)) {
    throw 'Keystore not found. Pass -Keystore or set SPK_KEYSTORE.'
}
if (-not $KsPass -or -not $KeyPass -or -not $Alias) {
    throw 'Set SPK_KS_PASS, SPK_KEY_PASS, and SPK_KEY_ALIAS (or pass -KsPass/-KeyPass/-Alias).'
}

if (-not $BaseApk) {
    $candidate = Get-ChildItem $buildDir -Filter 'SpektraLab-*-console.apk' -File -ErrorAction SilentlyContinue |
        Sort-Object LastWriteTime -Descending | Select-Object -First 1
    if ($candidate) { $BaseApk = $candidate.FullName }
}
if (-not $BaseApk -or -not (Test-Path $BaseApk)) {
    throw 'Base APK not found. Pass -BaseApk with a previously built SpektraLab console APK.'
}
if (-not $Output) { $Output = Join-Path $buildDir 'SpektraLab-dev.apk' }

$tmp = Join-Path ([IO.Path]::GetTempPath()) ('spektra-build-' + [guid]::NewGuid().ToString('N'))
$classes = Join-Path $tmp 'classes'
$dex = Join-Path $tmp 'dex'
$resStage = Join-Path $tmp 'res'
$baseResources = Join-Path $tmp 'base.apk'
$unsigned = Join-Path $tmp 'unsigned.apk'
$aligned = Join-Path $tmp 'aligned.apk'
New-Item -ItemType Directory -Path $classes, $dex, (Join-Path $resStage 'values'), (Join-Path $resStage 'xml'), (Join-Path $resStage 'mipmap-xxxhdpi') -Force | Out-Null

try {
    $sources = Get-ChildItem $sourcesDir -Recurse -Filter *.java | Select-Object -ExpandProperty FullName
    & $javac -source 17 -target 17 -encoding UTF-8 -Xlint:-options -nowarn `
        -classpath $androidJar -d $classes @sources
    if ($LASTEXITCODE -ne 0) { throw "javac failed with $LASTEXITCODE" }

    & $jdk17Java -cp $r8Jar com.android.tools.r8.D8 --lib $androidJar --min-api 24 `
        --output $dex @(Get-ChildItem $classes -Recurse -Filter *.class | Select-Object -ExpandProperty FullName)
    if ($LASTEXITCODE -ne 0) { throw "d8 failed with $LASTEXITCODE" }

    Copy-Item (Join-Path $res 'values\themes.xml') (Join-Path $resStage 'values\themes.xml') -Force
    Copy-Item (Join-Path $res 'xml\network_security_config.xml') `
        (Join-Path $resStage 'xml\network_security_config.xml') -Force
    Add-Type -AssemblyName System.IO.Compression.FileSystem
    $zip = [IO.Compression.ZipFile]::OpenRead($BaseApk)
    try {
        $icon = $zip.Entries | Where-Object { $_.FullName -match '^res/mipmap-.*/ic_launcher\.png$' } |
            Select-Object -First 1
        if (-not $icon) { throw "No mipmap launcher icon in $BaseApk" }
        [IO.Compression.ZipFileExtensions]::ExtractToFile($icon,
            (Join-Path $resStage 'mipmap-xxxhdpi\ic_launcher.png'), $true)
    } finally { $zip.Dispose() }

    & $aapt package -f -m -M $manifest -S $resStage -I $androidJar -F $baseResources
    if ($LASTEXITCODE -ne 0) { throw "aapt failed with $LASTEXITCODE" }

    $py = @'
import sys, zipfile
base, resources, dex, out = sys.argv[1:5]
skip = {"AndroidManifest.xml", "resources.arsc", "classes.dex"}
with zipfile.ZipFile(base) as old, zipfile.ZipFile(resources) as res, zipfile.ZipFile(out, "w") as z:
    for i in res.infolist():
        z.writestr(i, res.read(i.filename))
    for i in old.infolist():
        if i.filename in skip or i.filename.startswith("res/") or i.filename.startswith("META-INF/"):
            continue
        z.writestr(i, old.read(i.filename))
    z.write(dex, "classes.dex")
'@
    $pyPath = Join-Path $tmp 'assemble.py'
    Set-Content -Path $pyPath -Value $py -Encoding UTF8
    & $Python $pyPath $BaseApk $baseResources (Join-Path $dex 'classes.dex') $unsigned
    if ($LASTEXITCODE -ne 0) { throw "package assembly failed with $LASTEXITCODE" }

    & $zipalign -f -p 4 $unsigned $aligned
    if ($LASTEXITCODE -ne 0) { throw "zipalign failed with $LASTEXITCODE" }

    & $apksigner sign --ks $Keystore --ks-pass "pass:$KsPass" --key-pass "pass:$KeyPass" `
        --ks-key-alias $Alias --v1-signing-enabled true --out $Output $aligned
    if ($LASTEXITCODE -ne 0) { throw "apksigner failed with $LASTEXITCODE" }

    $hash = (Get-FileHash $Output -Algorithm SHA256).Hash
    $size = (Get-Item $Output).Length
    Write-Host "APK: $Output"
    Write-Host "Size: $size bytes"
    Write-Host "SHA-256: $hash"
} finally {
    Remove-Item $tmp -Recurse -Force -ErrorAction SilentlyContinue
}
