# PowerShell script to resolve remaining merge conflicts by choosing HEAD version
# This is safe for Controller files where HEAD version is preferred

$filesWithConflicts = Get-ChildItem -Path "src" -Filter "*.java" -Recurse | 
    Select-String -Pattern "^<<<<<<<" | 
    Select-Object -ExpandProperty Path -Unique

Write-Host "Files with conflicts: $($filesWithConflicts.Count)" -ForegroundColor Yellow

foreach ($file in $filesWithConflicts) {
    Write-Host "Processing: $file" -ForegroundColor Cyan
    
    $content = Get-Content $file -Raw
    
    # Replace conflict blocks with HEAD version
    # Pattern: <<<<<<< HEAD\n(content)\n=======\n(other content)\n>>>>>>> branch
    $pattern = '<<<<<<< HEAD\r?\n([\s\S]*?)\r?\n=======\r?\n[\s\S]*?\r?\n>>>>>>> [^\r\n]+'
    $replacement = '$1'
    
    $newContent = $content -replace $pattern, $replacement
    
    # Handle nested conflicts
    $iterations = 0
    while ($newContent -match '<<<<<<<' -and $iterations -lt 10) {
        $newContent = $newContent -replace $pattern, $replacement
        $iterations++
    }
    
    Set-Content -Path $file -Value $newContent -NoNewline
    Write-Host "  Fixed conflicts in $file" -ForegroundColor Green
}

Write-Host "`nDone! Verifying..." -ForegroundColor Green

$remaining = (Get-ChildItem -Path "src" -Filter "*.java" -Recurse | Select-String -Pattern "^<<<<<<<").Count
if ($remaining -eq 0) {
    Write-Host "SUCCESS: All conflicts resolved!" -ForegroundColor Green
} else {
    Write-Host "WARNING: $remaining conflict markers still remain" -ForegroundColor Yellow
}
