param(
    [switch]$SkipInsightFaceLicensePrompt
)

$ErrorActionPreference = "Stop"
$root = Split-Path -Parent $PSScriptRoot

$detDir = Join-Path $root "models\detector"
$recDir = Join-Path $root "models\recognition"
$liveDir = Join-Path $root "models\liveness"

New-Item -ItemType Directory -Force -Path $detDir, $recDir, $liveDir | Out-Null

Write-Host "Downloading YuNet detector..."

Invoke-WebRequest `
  -Uri "https://media.githubusercontent.com/media/opencv/opencv_zoo/main/models/face_detection_yunet/face_detection_yunet_2023mar.onnx" `
  -OutFile (Join-Path $detDir "face_detection_yunet_2023mar.onnx")


# ---------------------------------------------------------------------------
# InsightFace pretrained model notice
# ---------------------------------------------------------------------------

if (-not $SkipInsightFaceLicensePrompt) {
    Write-Host ""
    Write-Host "InsightFace public pretrained model notice:" -ForegroundColor Yellow
    Write-Host "The current InsightFace documentation states that the pretrained models"
    Write-Host "are available for non-commercial research purposes unless separately licensed."
    Write-Host ""
    Write-Host "This script will download:" -ForegroundColor Yellow
    Write-Host "  - buffalo_l / w600k_r50.onnx"
    Write-Host "  - buffalo_s / w600k_mbf.onnx"
    Write-Host ""

    $answer = Read-Host "Type AGREE to download InsightFace models for research/development use"

    if ($answer -ne "AGREE") {
        throw "InsightFace model download cancelled."
    }
}


# ---------------------------------------------------------------------------
# Helper function: download and extract one InsightFace model package
# ---------------------------------------------------------------------------

function Install-InsightFaceRecognitionModel {
    param(
        [Parameter(Mandatory = $true)]
        [string]$PackageName,

        [Parameter(Mandatory = $true)]
        [string]$PackageUrl,

        [Parameter(Mandatory = $true)]
        [string]$ModelFileName
    )

    $zip = Join-Path $env:TEMP "$PackageName.zip"
    $tmp = Join-Path $env:TEMP "${PackageName}_extract"

    Write-Host ""
    Write-Host "Downloading InsightFace $PackageName package..."

    Invoke-WebRequest `
      -Uri $PackageUrl `
      -OutFile $zip

    if (Test-Path $tmp) {
        Remove-Item -Recurse -Force $tmp
    }

    New-Item -ItemType Directory -Force -Path $tmp | Out-Null

    Write-Host "Extracting $PackageName..."

    Expand-Archive `
      -Path $zip `
      -DestinationPath $tmp `
      -Force

    $found = Get-ChildItem `
      -Path $tmp `
      -Filter $ModelFileName `
      -Recurse |
            Select-Object -First 1

    if (-not $found) {
        throw "$ModelFileName was not found in $PackageName.zip"
    }

    $destination = Join-Path $recDir $ModelFileName

    Write-Host "Installing $ModelFileName..."
    Copy-Item `
      $found.FullName
            $destination `
      -Force

    Write-Host "Installed: $destination"

    # Clean temporary files
    Remove-Item -Force $zip

    if (Test-Path $tmp) {
        Remove-Item -Recurse -Force $tmp
    }
}

$smallZip = Join-Path $env:TEMP "buffalo_s.zip"
Write-Host "Downloading InsightFace buffalo_s client model package..."
Invoke-WebRequest `
  -Uri "https://github.com/deepinsight/insightface/releases/download/model-zoo/buffalo_s.zip" `
  -OutFile $smallZip
$smallTmp = Join-Path $env:TEMP "buffalo_s_extract"
if (Test-Path $smallTmp) { Remove-Item -Recurse -Force $smallTmp }
Expand-Archive -Path $smallZip -DestinationPath $smallTmp -Force
$smallFound = Get-ChildItem -Path $smallTmp -Filter "w600k_mbf.onnx" -Recurse | Select-Object -First 1
if (-not $smallFound) { throw "w600k_mbf.onnx was not found in buffalo_s.zip" }
Copy-Item $smallFound.FullName (Join-Path $recDir "w600k_mbf.onnx") -Force


# ---------------------------------------------------------------------------
# Download w600k_r50.onnx
# ---------------------------------------------------------------------------

Install-InsightFaceRecognitionModel `
  -PackageName "buffalo_l" `
  -PackageUrl "https://github.com/deepinsight/insightface/releases/download/model-zoo/buffalo_l.zip" `
  -ModelFileName "w600k_r50.onnx"


# ---------------------------------------------------------------------------
# Download w600k_mbf.onnx
# ---------------------------------------------------------------------------

Install-InsightFaceRecognitionModel `
  -PackageName "buffalo_s" `
  -PackageUrl "https://github.com/deepinsight/insightface/releases/download/v0.7/buffalo_s.zip" `
  -ModelFileName "w600k_mbf.onnx"


# ---------------------------------------------------------------------------
# Download MiniFASNetV2 liveness model
# ---------------------------------------------------------------------------

Write-Host ""
Write-Host "Downloading MiniFASNetV2 ONNX..."

Invoke-WebRequest `
  -Uri "https://raw.githubusercontent.com/QingHeYang/Silent-Face-Anti-Spoofing-onnx/main/onnx/2.7_80x80_MiniFASNetV2.onnx" `
  -OutFile (Join-Path $liveDir "2.7_80x80_MiniFASNetV2.onnx")


# ---------------------------------------------------------------------------
# Final verification
# ---------------------------------------------------------------------------

Write-Host ""
Write-Host "Installed models:" -ForegroundColor Green

Get-ChildItem $detDir, $recDir, $liveDir -Filter "*.onnx" -Recurse |
        Select-Object FullName,
        @{Name = "SizeMB"; Expression = {
            [math]::Round($_.Length / 1MB, 2)
        }} |
        Format-Table -AutoSize

Write-Host ""
Write-Host "Models installed under $root\models" -ForegroundColor Green