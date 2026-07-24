param(
    [string]$ProjectName = "refail-production-smoke",
    [string]$EnvFile = ".env.production.smoke.example",
    [switch]$KeepRunningOnFailure
)

$ErrorActionPreference = "Stop"
$root = Split-Path $PSScriptRoot -Parent
$resolvedEnvFile = [System.IO.Path]::GetFullPath((Join-Path $root $EnvFile))
$composeArgs = @(
    "compose",
    "-p", $ProjectName,
    "--env-file", $resolvedEnvFile,
    "-f", (Join-Path $root "compose.yaml"),
    "-f", (Join-Path $root "compose.production.yaml")
)
$succeeded = $false

function Get-EnvValue([string]$Name) {
    $line = Get-Content -Encoding utf8 $resolvedEnvFile |
        Where-Object { $_ -match "^\s*$([regex]::Escape($Name))=(.*)$" } |
        Select-Object -First 1
    if (-not $line) {
        throw "Missing $Name in the environment file."
    }
    return ($line -split "=", 2)[1].Trim()
}

function Invoke-Compose([string[]]$Arguments) {
    & docker @composeArgs @Arguments
    if ($LASTEXITCODE -ne 0) {
        throw "docker compose $($Arguments -join ' ') failed."
    }
}

function Assert-PortBoundary([int]$HttpPort, [int]$HttpsPort) {
    $jsonLines = & docker @composeArgs ps --format json
    if ($LASTEXITCODE -ne 0) {
        throw "Failed to inspect Compose ports."
    }

    $services = @($jsonLines | ForEach-Object { $_ | ConvertFrom-Json })
    $expectedServices = @("backend", "caddy", "frontend", "mysql")
    foreach ($serviceName in $expectedServices) {
        $service = $services | Where-Object Service -eq $serviceName
        if (-not $service) {
            throw "$serviceName is not running."
        }
        if ($service.State -ne "running" -or $service.Health -ne "healthy") {
            throw "$serviceName is unhealthy: $($service.Status)"
        }
    }

    foreach ($service in $services) {
        $published = @($service.Publishers | Where-Object { $_.PublishedPort -gt 0 })
        if ($service.Service -ne "caddy" -and $published.Count -gt 0) {
            throw "$($service.Service) publishes a host port."
        }
    }

    $gateway = $services | Where-Object Service -eq "caddy"
    $gatewayPorts = @(
        $gateway.Publishers |
            Where-Object { $_.PublishedPort -gt 0 } |
            Select-Object -ExpandProperty PublishedPort -Unique |
            Sort-Object
    )
    $expectedPorts = @($HttpPort, $HttpsPort) | Sort-Object
    if (($gatewayPorts -join ",") -ne ($expectedPorts -join ",")) {
        throw "Caddy published ports do not match the expected gateway ports."
    }

    Write-Output "[ports] Only Caddy publishes HTTP $HttpPort and HTTPS $HttpsPort."
}

try {
    $httpPort = [int](Get-EnvValue "HTTP_PORT")
    $httpsPort = [int](Get-EnvValue "HTTPS_PORT")

    Invoke-Compose @("config", "--quiet")
    Invoke-Compose @("up", "-d", "--build", "--wait", "--wait-timeout", "240")
    Assert-PortBoundary $httpPort $httpsPort

    & node (Join-Path $PSScriptRoot "smoke-production.mjs") `
        "--http-port" $httpPort `
        "--https-port" $httpsPort
    if ($LASTEXITCODE -ne 0) {
        throw "The HTTPS application flow failed."
    }

    $succeeded = $true
    Write-Output "Production deployment smoke test passed."
} finally {
    if ($succeeded -or -not $KeepRunningOnFailure) {
        & docker @composeArgs down -v --remove-orphans | Out-Null
    } else {
        Write-Output "Keeping $ProjectName running for failure diagnostics."
    }
}
