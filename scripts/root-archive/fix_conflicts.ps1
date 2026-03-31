# Advanced conflict resolver - removes markers and keeps HEAD version
param(
    [string]$FilePath
)

$content = Get-Content $FilePath -Raw -Encoding UTF8

# Count initial conflicts
$initialCount = ([regex]::Matches($content, '<<<<<<< HEAD')).Count
Write-Host "Processing $FilePath - Found $initialCount conflicts" -ForegroundColor Cyan

# Remove conflict blocks iteratively
$maxIterations = 20
for ($i = 0; $i -lt $maxIterations; $i++) {
    $before = $content
    
    # Pattern: <<<<<<< HEAD\n...\n=======\n...\n>>>>>>> branch
    # Keep only the HEAD section (between <<<<<<< HEAD and =======)
    $content = $content -replace '(?s)<<<<<<< HEAD\r?\n(.*?)\r?\n=======\r?\n.*?\r?\n>>>>>>> [^\r\n]+\r?\n?', '$1'
    
    # If no more changes, we're done
    if ($content -eq $before) { break }
}

# Final cleanup - remove any stray markers
$content = $content -replace '<<<<<<< HEAD\r?\n', ''
$content = $content -replace '=======\r?\n', ''
$content = $content -replace '>>>>>>> [^\r\n]+\r?\n?', ''

# Count remaining conflicts
$remaining = ([regex]::Matches($content, '<<<<<<<')).Count

# Write back
[System.IO.File]::WriteAllText($FilePath, $content, [System.Text.Encoding]::UTF8)

if ($remaining -eq 0) {
    Write-Host "  ✓ SUCCESS - All conflicts resolved" -ForegroundColor Green
} else {
    Write-Host "  ⚠ WARNING - $remaining markers remain" -ForegroundColor Yellow
}

return $remaining
