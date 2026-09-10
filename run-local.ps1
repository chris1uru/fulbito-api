$envFile = Join-Path $PSScriptRoot '.env'
if (Test-Path -LiteralPath $envFile) {
    Get-Content -LiteralPath $envFile | ForEach-Object {
        $line = $_.Trim()
        if ($line -and -not $line.StartsWith('#')) {
            $entry = $line -split '=', 2
            if ($entry.Count -eq 2) {
                $key = $entry[0].Trim()
                $value = $entry[1].Trim()
                if ($key -and -not (Test-Path -LiteralPath "Env:$key")) {
                    Set-Item -LiteralPath "Env:$key" -Value $value
                }
            }
        }
    }
}

if (-not $env:DB_URL -or -not $env:DB_USERNAME -or -not $env:DB_PASSWORD -or
    -not $env:JWT_SECRET -or -not $env:CLOUDINARY_CLOUD_NAME -or
    -not $env:CLOUDINARY_API_KEY -or -not $env:CLOUDINARY_API_SECRET) {
    Write-Error 'Faltan variables de Neon, JWT o Cloudinary. Consulta .env.example y README.md.'
    exit 1
}

if ($env:JAVA_HOME) {
    $javaExe = Join-Path $env:JAVA_HOME 'bin\java.exe'
    $javacExe = Join-Path $env:JAVA_HOME 'bin\javac.exe'
} else {
    $javaCommand = Get-Command java -CommandType Application -ErrorAction SilentlyContinue
    $javacCommand = Get-Command javac -CommandType Application -ErrorAction SilentlyContinue
    $javaExe = $javaCommand.Source
    $javacExe = $javacCommand.Source
}

if (-not $javaExe -or -not $javacExe -or
    -not (Test-Path -LiteralPath $javaExe) -or -not (Test-Path -LiteralPath $javacExe)) {
    Write-Error 'No se encontro un JDK. Instala JDK 21 o superior y configura JAVA_HOME o agrega java y javac al PATH.'
    exit 1
}

$javaVersion = & $javaExe -version 2>&1 | Out-String
if ($LASTEXITCODE -ne 0 -or $javaVersion -notmatch 'version "(?:1\.)?(?<major>\d+)') {
    Write-Error 'No se pudo determinar la version del JDK instalado.'
    exit 1
}

if ([int]$Matches.major -lt 21) {
    Write-Error "Este proyecto requiere JDK 21 o superior. Version detectada: $($Matches.major)."
    exit 1
}

Push-Location $PSScriptRoot
try {
    & "$PSScriptRoot\mvnw.cmd" spring-boot:run
    $exitCode = $LASTEXITCODE
} finally {
    Pop-Location
}

exit $exitCode
