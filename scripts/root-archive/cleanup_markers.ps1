# Fix malformed conflict markers (markers stuck to code)
param(
    [string]$FilePath
)

$content = Get-Content $FilePath -Raw -Encoding UTF8

# Count initial markers
$initialCount = ([regex]::Matches($content, '<<<<<<<')).Count
Write-Host "Processing $FilePath - Found $initialCount stray markers" -ForegroundColor Cyan

# Remove malformed markers that are stuck to code (no newline after marker)
$content = $content -replace '<<<<<<< HEAD', ''
$content = $content -replace '>>>>>>> [^\r\n]+', ''
$content = $content -replace '=======\r?\n?', ''

# Count remaining
$remaining = ([regex]::Matches($content, '<<<<<<<')).Count

# Write back
[System.IO.File]::WriteAllText($FilePath, $content, [System.Text.Encoding]::UTF8)

if ($remaining -eq 0) {
    Write-Host "  ✓ SUCCESS - All markers removed" -ForegroundColor Green
} else {
    Write-Host "  ⚠ WARNING - $remaining markers remain" -ForegroundColor Yellow
}

return $remaining
