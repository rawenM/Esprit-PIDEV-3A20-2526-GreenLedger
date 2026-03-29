$path = "C:\Users\Mega-PC\Desktop\Pi_Dev\src\main\java\Controllers\CarbonAuditController.java"
$lines = Get-Content $path
$depth = 0
$started = $false
for ($i = 0; $i -lt $lines.Count; $i++) {
  $line = $lines[$i]
  if ($line -match 'class CarbonAuditController') { $started = $true }
  if ($started) {
    $depth += ([regex]::Matches($line,'\{').Count)
    $depth -= ([regex]::Matches($line,'\}').Count)
  }
}
Write-Output ("final depth=" + $depth)
