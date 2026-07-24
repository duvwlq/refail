param(
    [string]$BaseUrl = "http://localhost:18080/api/v1",
    [string]$EnvFile = ".env.example",
    [string]$Email = "admin@refail.e2e",
    [string]$Password = "e2e-password-123!",
    [string]$Nickname = "E2E관리자"
)

$ErrorActionPreference = "Stop"
$root = Split-Path $PSScriptRoot -Parent
$resolvedEnvFile = [System.IO.Path]::GetFullPath((Join-Path $root $EnvFile))
$envValues = @{}

Get-Content -Encoding utf8 $resolvedEnvFile | ForEach-Object {
    if ($_ -match '^\s*([^#][^=]*)=(.*)$') {
        $envValues[$matches[1].Trim()] = $matches[2].Trim()
    }
}

try {
    $body = @{ email = $Email; password = $Password; nickname = $Nickname } | ConvertTo-Json
    Invoke-RestMethod -Method Post -Uri "$BaseUrl/auth/signup" `
        -ContentType "application/json; charset=utf-8" -Body $body | Out-Null
} catch {
    if ($_.Exception.Response.StatusCode.value__ -ne 409) { throw }
}

$escapedEmail = $Email.Replace("'", "''")
$sql = "UPDATE users SET role = 'ADMIN', status = 'ACTIVE' WHERE email = '$escapedEmail';"
$rootPassword = $envValues["MYSQL_ROOT_PASSWORD"]
$database = $envValues["MYSQL_DATABASE"]
$composeArgs = @(
    "compose",
    "-f", (Join-Path $root "compose.yaml"),
    "-f", (Join-Path $root "compose.e2e.yaml"),
    "--env-file", $resolvedEnvFile,
    "exec", "-T",
    "-e", "MYSQL_PWD=$rootPassword",
    "mysql", "mysql", "-uroot", $database, "-e", $sql
)

& docker @composeArgs | Out-Null
if ($LASTEXITCODE -ne 0) {
    throw "E2E 관리자 역할을 설정하지 못했습니다."
}

Write-Output "E2E admin is ready."
