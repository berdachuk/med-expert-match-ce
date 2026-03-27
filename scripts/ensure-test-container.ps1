# Ensures the test container image exists before integration tests (Windows / PowerShell).
# Builds medexpertmatch-postgres-test:latest if not found locally.
# Called automatically by Maven in pre-integration-test phase on Windows.

$ErrorActionPreference = "Stop"
$ProjectRoot = Split-Path -Parent $PSScriptRoot
$ImageName = "medexpertmatch-postgres-test:latest"

Push-Location $ProjectRoot
try {
    # Use "docker images -q" instead of "inspect": inspect writes to stderr when missing and PowerShell treats that as failure.
    $id = docker images -q $ImageName
    if ($id) {
        Write-Host "Test container image $ImageName exists, skipping build"
        exit 0
    }
    Write-Host "Test container image $ImageName not found, building..."
    & (Join-Path $PSScriptRoot "build-test-container.ps1")
} finally {
    Pop-Location
}
