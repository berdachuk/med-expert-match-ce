# Build the test container image for integration tests (Windows / PowerShell).
# PostgreSQL 17 with Apache AGE and PgVector extensions.

$ErrorActionPreference = "Stop"
$ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$ProjectRoot = Split-Path -Parent $ScriptDir

Write-Host "Building medexpertmatch-postgres-test Docker image..."
Write-Host "This may take 5-10 minutes on first build..."

Push-Location $ProjectRoot
try {
    docker build -f docker/Dockerfile.test -t medexpertmatch-postgres-test:latest .
} finally {
    Pop-Location
}

Write-Host ""
Write-Host "Test container image built successfully."
Write-Host "Image: medexpertmatch-postgres-test:latest"
