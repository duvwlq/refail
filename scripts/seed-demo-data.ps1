$ErrorActionPreference = "Stop"
$baseUrl = "http://localhost:18080/api/v1"
$utf8 = New-Object System.Text.UTF8Encoding($false)
$dataPath = Join-Path $PSScriptRoot "demo-data.json"
$demo = [System.IO.File]::ReadAllText($dataPath, $utf8) | ConvertFrom-Json
$credentials = $demo.credentials

try {
    Invoke-RestMethod -Method Post -Uri "$baseUrl/auth/signup" -ContentType "application/json; charset=utf-8" -Body ($credentials | ConvertTo-Json)
} catch {
    if ($_.Exception.Response.StatusCode.value__ -ne 409) { throw }
}

$login = Invoke-RestMethod -Method Post -Uri "$baseUrl/auth/login" -ContentType "application/json; charset=utf-8" -Body (@{ email = $credentials.email; password = $credentials.password } | ConvertTo-Json)
$headers = @{ Authorization = "Bearer $($login.accessToken)" }
$mine = Invoke-RestMethod -Method Get -Uri "$baseUrl/posts/me?page=0&size=1" -Headers $headers
if ($mine.totalElements -gt 0) { Write-Output "Demo data already exists."; exit 0 }

$categories = Invoke-RestMethod -Method Get -Uri "$baseUrl/categories"
$categorySlugs = @("study", "diet", "daily", "study")
$index = 0
foreach ($example in $demo.examples) {
    $category = $categories | Where-Object { $_.slug -eq $categorySlugs[$index] } | Select-Object -First 1
    $body = @{ categoryId = $category.categoryId; title = $example.title; content = $example.content; visibilityType = "NICKNAME"; failureSize = $example.size; emotionTag = $example.emotion; advicePreference = "COMFORT"; retryIntention = $true; nextAttemptPlan = $example.plan } | ConvertTo-Json
    $post = Invoke-RestMethod -Method Post -Uri "$baseUrl/posts" -Headers $headers -ContentType "application/json; charset=utf-8" -Body ([System.Text.Encoding]::UTF8.GetBytes($body))
    $updateBody = @{ status = $example.status; content = $example.update } | ConvertTo-Json
    Invoke-RestMethod -Method Post -Uri "$baseUrl/posts/$($post.postId)/updates" -Headers $headers -ContentType "application/json; charset=utf-8" -Body ([System.Text.Encoding]::UTF8.GetBytes($updateBody)) | Out-Null
    $index++
}
Write-Output "Re:Fail demo data created."
