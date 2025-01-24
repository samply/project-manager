# AES Key Generator Script
param (
    [ValidateSet(16, 24, 32)]
    [int]$KeyLength = 16 # Default key length (16 bytes = 128 bits)
)

# Generate a secure random AES key
$randomBytes = New-Object Byte[] $KeyLength
[Security.Cryptography.RandomNumberGenerator]::Create().GetBytes($randomBytes)

# Convert the key to a Base64 string
$base64Key = [System.Convert]::ToBase64String($randomBytes)

# Output the generated key
Write-Host "Your AES key ($KeyLength bytes, Base64-encoded):" -ForegroundColor Green
Write-Host $base64Key -ForegroundColor Yellow

# Copy the key to the clipboard (Windows only)
if ($PSVersionTable.PSVersion.Major -ge 5) {
    $base64Key | Set-Clipboard
    Write-Host "`nThe key has been copied to your clipboard!" -ForegroundColor Cyan
} else {
    Write-Host "`nCopy the key manually: (Ctrl+C to copy)" -ForegroundColor Cyan
}

## Example: How to use:
# .\GenerateAESKey.ps1
# For AES-128
# .\GenerateAESKey.ps1 -KeyLength 16
# For AES-192
# .\GenerateAESKey.ps1 -KeyLength 24
# For AES-256
# .\GenerateAESKey.ps1 -KeyLength 32
