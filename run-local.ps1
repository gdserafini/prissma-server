param(
    [switch]$Offline
)
Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'

$projectRoot = Split-Path -Parent $MyInvocation.MyCommand.Path
$logsDir = Join-Path $projectRoot 'var\logs'
New-Item -ItemType Directory -Force -Path $logsDir | Out-Null

$javaCandidates = @(
    'C:\Program Files\JetBrains\IntelliJ IDEA Community Edition 2025.2\jbr',
    'C:\Program Files\JetBrains\IntelliJ IDEA 2025.2\jbr',
    $env:JAVA_HOME
) | Where-Object { $_ -and (Test-Path (Join-Path $_ 'bin\java.exe')) } | Select-Object -Unique

if (-not $javaCandidates) {
    throw 'Nenhum Java 21+ encontrado. Ajuste o JAVA_HOME ou instale uma JDK 21.'
}

$javaHome = $javaCandidates | Select-Object -First 1
$env:JAVA_HOME = $javaHome
$env:Path = (Join-Path $javaHome 'bin') + ';' + $env:Path

$javaExe = Join-Path $javaHome 'bin\java.exe'
$javaVersionOutput = cmd.exe /c "\"$javaExe\" -version 2>&1"
if ($javaVersionOutput -notmatch 'version "2[1-9]') {
    throw "O Java encontrado em '$javaHome' não é 21+. Saída: $($javaVersionOutput -join ' ')"
}

$settingsPath = 'C:\Users\GFURQUI\.m2\settings.xml'
$mavenWrapper = Join-Path $projectRoot 'mvnw.cmd'
$cachedMaven = Get-ChildItem 'C:\Users\GFURQUI\.m2\wrapper\dists\apache-maven-*' -Recurse -Filter mvn.cmd -ErrorAction SilentlyContinue |
    Sort-Object FullName -Descending |
    Select-Object -First 1 -ExpandProperty FullName

if ($cachedMaven) {
    $mavenCommand = $cachedMaven
} elseif (Test-Path $mavenWrapper) {
    $mavenCommand = $mavenWrapper
} else {
    throw 'Nenhum Maven disponível. Nem o wrapper do projeto nem uma distribuição cacheada foram encontrados.'
}

$stdoutLog = Join-Path $logsDir 'run-local.log'
$stderrLog = Join-Path $logsDir 'run-local-err.log'

$mavenArgs = @()
if (Test-Path $settingsPath) {
    $mavenArgs += @('-s', $settingsPath)
}
if ($Offline) {
    $mavenArgs += '-o'
}
$mavenArgs += @(
    '-Dmaven.test.skip=true',
    '-Dtomcat.version=10.1.59',
    '-Dthymeleaf.version=3.1.5.RELEASE',
    'spring-boot:run',
    '-Dspring-boot.run.profiles=local'
)

Push-Location $projectRoot
try {
    Write-Host "Projeto: $projectRoot"
    Write-Host "JAVA_HOME: $javaHome"
    Write-Host "Maven: $mavenCommand"
    Write-Host "Logs: $stdoutLog"
    Write-Host "Perfil Spring: local"
    if ($Offline) {
        Write-Host 'Modo Maven: offline'
    }

    & $mavenCommand @mavenArgs 1>> $stdoutLog 2>> $stderrLog
    exit $LASTEXITCODE
}
finally {
    Pop-Location
}
