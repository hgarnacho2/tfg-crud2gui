param(
    [string]$OutputDirectory
)

$ErrorActionPreference = 'Stop'
$repo = Split-Path $PSScriptRoot -Parent
if (-not $OutputDirectory) {
    $OutputDirectory = Join-Path $repo ('dist\windows-' + (Get-Date -Format 'yyyyMMdd-HHmmss'))
}
$OutputDirectory = [System.IO.Path]::GetFullPath($OutputDirectory)
if (Test-Path -LiteralPath (Join-Path $OutputDirectory 'CRUD2GUI')) {
    throw "Ya existe CRUD2GUI en $OutputDirectory. Elige otro directorio para no sobrescribirlo."
}

$maven = (Get-Command mvn -ErrorAction Stop).Source
$javaHome = $env:JAVA_HOME
if (-not $javaHome) {
    $javaHome = Split-Path (Split-Path (Get-Command java -ErrorAction Stop).Source -Parent) -Parent
}
$jpackage = Join-Path $javaHome 'bin\jpackage.exe'
if (-not (Test-Path -LiteralPath $jpackage)) {
    throw "No se encuentra jpackage en $javaHome. Usa un JDK 21 completo."
}

$module = Join-Path $repo 'modules\desktop-app'
$inputDirectory = Join-Path $module ('target\portable-input-' + [guid]::NewGuid().ToString('N'))
& $maven -f (Join-Path $repo 'pom.xml') -pl modules/desktop-app -am -Pportable-windows `
    "-Ddesktop.package.input=$inputDirectory" '-DskipTests' package
if ($LASTEXITCODE -ne 0) {
    throw 'Ha fallado la compilacion Maven. No se ha generado la distribucion.'
}

$jar = 'crud2gui-desktop-app-0.1.0-SNAPSHOT.jar'
Copy-Item -LiteralPath (Join-Path $module "target\$jar") -Destination $inputDirectory
$configDirectory = Join-Path $inputDirectory 'config'
New-Item -ItemType Directory -Path $configDirectory | Out-Null
Copy-Item -LiteralPath (Join-Path $PSScriptRoot 'portable.env.example') `
    -Destination (Join-Path $configDirectory '.env.public')

& $jpackage --type app-image --name CRUD2GUI --app-version 0.1.0 `
    --input $inputDirectory --dest $OutputDirectory `
    --main-jar $jar --main-class com.crud2gui.desktop.DesktopLauncher `
    --java-options '-Dprism.order=sw' --win-console
if ($LASTEXITCODE -ne 0) {
    throw 'Ha fallado jpackage. Consulta el error anterior.'
}

$executable = Join-Path $OutputDirectory 'CRUD2GUI\CRUD2GUI.exe'
if (-not (Test-Path -LiteralPath $executable)) {
    throw "No se ha encontrado el ejecutable esperado: $executable"
}
Write-Host "Distribucion creada: $executable"
Write-Host 'Configura app\config\.env.public antes de autenticarte. Copia la carpeta CRUD2GUI completa.'
