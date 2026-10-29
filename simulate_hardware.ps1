param (
    [Parameter(Mandatory=$false)]
    [string]$DatabaseUrl = "",

    [Parameter(Mandatory=$false)]
    [string]$DeviceId = "washroom_01",

    [Parameter(Mandatory=$false)]
    [ValidateSet("normal", "emergency_nobuzzer", "emergency_buzzer", "offline")]
    [string]$Action = "emergency_nobuzzer"
)

Write-Host "==========================================================" -ForegroundColor Cyan
Write-Host " Smart Washroom - Hardware Firebase Simulation Script" -ForegroundColor Yellow
Write-Host "==========================================================" -ForegroundColor Cyan

if ([string]::IsNullOrWhiteSpace($DatabaseUrl)) {
    Write-Host "Usage:" -ForegroundColor Yellow
    Write-Host "  .\simulate_hardware.ps1 -DatabaseUrl 'https://YOUR_DB.firebaseio.com' -Action emergency_nobuzzer"
    Write-Host ""
    Write-Host "Actions:" -ForegroundColor Green
    Write-Host "  normal              : Sends normal heartbeat (online=true, buzzer=OFF, emergency=false)"
    Write-Host "  emergency_nobuzzer  : PRIMARY TEST - Emergency active & Buzzer OFF (Fires notification)"
    Write-Host "  emergency_buzzer    : Emergency active & Buzzer ON (Local buzzer only; no push alert)"
    Write-Host "  offline             : Marks device offline"
    exit
}

# Ensure URL ends without trailing slash
$baseUrl = $DatabaseUrl.TrimEnd('/')
$endpoint = "$baseUrl/devices/$DeviceId.json"

$timestamp = [DateTimeOffset]::UtcNow.ToUnixTimeMilliseconds()

switch ($Action) {
    "normal" {
        $body = @{
            deviceId = $DeviceId
            online = $true
            lastSeen = $timestamp
            emergency = $false
            buzzer = $false
            motion = $true
            occupancy = "OCCUPIED"
            status = "NORMAL"
        } | ConvertTo-Json
        Write-Host "Sending NORMAL heartbeat to $endpoint..." -ForegroundColor Green
    }
    "emergency_nobuzzer" {
        $body = @{
            deviceId = $DeviceId
            online = $true
            lastSeen = $timestamp
            emergency = $true
            buzzer = $false
            motion = $true
            occupancy = "OCCUPIED"
            status = "EMERGENCY"
            alert_type = "EMERGENCY_BUTTON"
        } | ConvertTo-Json
        Write-Host "Sending EMERGENCY WITH BUZZER OFF to $endpoint..." -ForegroundColor Red
        Write-Host "Expected result: Android App receives emergency push notification!" -ForegroundColor Yellow
    }
    "emergency_buzzer" {
        $body = @{
            deviceId = $DeviceId
            online = $true
            lastSeen = $timestamp
            emergency = $true
            buzzer = $true
            motion = $true
            occupancy = "OCCUPIED"
            status = "EMERGENCY"
            alert_type = "NO_MOTION"
        } | ConvertTo-Json
        Write-Host "Sending EMERGENCY WITH BUZZER ON to $endpoint..." -ForegroundColor DarkYellow
        Write-Host "Expected result: Buzzer rings locally; buzzer-off notification is skipped." -ForegroundColor Yellow
    }
    "offline" {
        $body = @{
            deviceId = $DeviceId
            online = $false
            lastSeen = ($timestamp - 120000) # 2 minutes ago
            status = "OFFLINE"
        } | ConvertTo-Json
        Write-Host "Sending OFFLINE state to $endpoint..." -ForegroundColor Gray
    }
}

try {
    $response = Invoke-RestMethod -Uri $endpoint -Method Patch -Body $body -ContentType "application/json"
    Write-Host "Firebase updated successfully!" -ForegroundColor Green
    Write-Host "Response: $($response | ConvertTo-Json -Compress)" -ForegroundColor Cyan
} catch {
    Write-Host "Failed to update Firebase: $($_.Exception.Message)" -ForegroundColor Red
    Write-Host "Make sure your Realtime Database rules allow read/write or publish rules from backend/database.rules.json" -ForegroundColor Yellow
}
