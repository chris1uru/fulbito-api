if (-not $env:DB_URL -or -not $env:DB_USERNAME -or -not $env:DB_PASSWORD -or -not $env:JWT_SECRET) {
    Write-Error 'Faltan DB_URL, DB_USERNAME, DB_PASSWORD o JWT_SECRET. Consulta README.md.'
    exit 1
}

# Codex dejo un JDK 21 portatil en E:. Si ya tenes JAVA_HOME, se respeta.
if (-not $env:JAVA_HOME -and (Test-Path -LiteralPath 'E:\temurin-21\jdk-21.0.12+8')) {
    $env:JAVA_HOME = 'E:\temurin-21\jdk-21.0.12+8'
    $env:Path = "$env:JAVA_HOME\bin;$env:Path"
}

# Codex dejo un JDK 21 portatil en E:. Si ya tenes JAVA_HOME, se respeta.
if (-not $env:JAVA_HOME -and (Test-Path -LiteralPath 'E:\temurin-21\jdk-21.0.12+8')) {
    $env:JAVA_HOME = 'E:\temurin-21\jdk-21.0.12+8'
    $env:Path = "$env:JAVA_HOME\bin;$env:Path"
}

.\mvnw.cmd spring-boot:run
