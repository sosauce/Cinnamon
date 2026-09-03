# Generate Cinnamon release keystore + base64 for GitHub Secrets
# Usage: powershell -ExecutionPolicy Bypass -File scripts/generate-keystore.ps1

param(
    [string]$Alias = "cinnamon",
    [string]$Keystore = "cinnamon-release.jks",
    [string]$DName = "CN=Cinnamon, OU=Mobile, O=sosauce, L=Cairo, S=Cairo, C=EG"
)

$storePass = Read-Host "Enter KEYSTORE_PASSWORD (storepass) - will be hidden" -AsSecureString
$keyPass   = Read-Host "Enter KEY_PASSWORD (keypass, often same)" -AsSecureString
$storePlain = [Runtime.InteropServices.Marshal]::PtrToStringAuto([Runtime.InteropServices.Marshal]::SecureStringToBSTR($storePass))
$keyPlain   = [Runtime.InteropServices.Marshal]::PtrToStringAuto([Runtime.InteropServices.Marshal]::SecureStringToBSTR($keyPass))

if ([string]::IsNullOrWhiteSpace($storePlain) -or [string]::IsNullOrWhiteSpace($keyPlain)) {
    Write-Error "Passwords cannot be empty"
    exit 1
}

Write-Host "`nGenerating $Keystore with alias '$Alias'..." -ForegroundColor Cyan
keytool -genkey -v -keystore $Keystore -keyalg RSA -keysize 2048 -validity 10000 -alias $Alias -storepass $storePlain -keypass $keyPlain -dname $DName
if ($LASTEXITCODE -ne 0) { Write-Error "keytool failed"; exit $LASTEXITCODE }

Write-Host "`nEncoding to base64..." -ForegroundColor Cyan
$b64 = [Convert]::ToBase64String([IO.File]::ReadAllBytes((Resolve-Path $Keystore)))
$b64 | Set-Content -NoNewline -Path "keystore.b64"
Write-Host "Wrote keystore.b64 (single line, ready to paste as KEYSTORE_FILE_B64)" -ForegroundColor Green
Write-Host "Length: $($b64.Length) chars"

# verify
Write-Host "`nVerifying..." -ForegroundColor Cyan
keytool -list -v -keystore $Keystore -alias $Alias -storepass $storePlain | Select-Object -First 20

Write-Host "`n=== GitHub Secrets to create ===" -ForegroundColor Yellow
Write-Host "KEYSTORE_FILE_B64  -> paste entire content of keystore.b64"
Write-Host "KEYSTORE_PASSWORD  -> $storePlain"
Write-Host "KEY_ALIAS          -> $Alias"
Write-Host "KEY_PASSWORD       -> $keyPlain"
Write-Host "`nAdd at: https://github.com/MoHamed-B-M/Cinnamon/settings/secrets/actions" -ForegroundColor Yellow
Write-Host "`nDO NOT commit $Keystore or keystore.b64 to git!" -ForegroundColor Red

# clear plaintext from memory
$storePlain = $null; $keyPlain = $null
