param(
    [ValidateSet(10000, 50000)]
    [int]$PostCount = 10000
)

$ErrorActionPreference = "Stop"
$projectRoot = Split-Path -Parent $PSScriptRoot

Push-Location $projectRoot
try {
    & .\gradlew.bat searchBenchmark "-PsearchBenchmarkPostCount=$PostCount"
    if ($LASTEXITCODE -ne 0) {
        throw "검색 벤치마크가 실패했습니다."
    }

    $report = Join-Path $projectRoot "build\reports\search-benchmark-$PostCount.md"
    Write-Host "검색 벤치마크 보고서: $report"
} finally {
    Pop-Location
}
