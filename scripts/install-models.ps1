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

if (-not $SkipInsightFaceLicensePrompt) {
    Write-Host ""
    Write-Host "InsightFace public pretrained model notice:" -ForegroundColor Yellow
    Write-Host "The current InsightFace model documentation states that the pretrained models are for non-commercial research unless separately licensed."
    $answer = Read-Host "Type AGREE to download buffalo_l for research/development use"
    if ($answer -ne "AGREE") {
        throw "InsightFace model download cancelled."
    }
}

$zip = Join-Path $env:TEMP "buffalo_l.zip"
Write-Host "Downloading InsightFace buffalo_l model package (~275 MB)..."
Invoke-WebRequest `
  -Uri "https://github.com/deepinsight/insightface/releases/download/model-zoo/buffalo_l.zip" `
  -OutFile $zip

$tmp = Join-Path $env:TEMP "buffalo_l_extract"
if (Test-Path $tmp) { Remove-Item -Recurse -Force $tmp }
Expand-Archive -Path $zip -DestinationPath $tmp -Force
$found = Get-ChildItem -Path $tmp -Filter "w600k_r50.onnx" -Recurse | Select-Object -First 1
if (-not $found) { throw "w600k_r50.onnx was not found in buffalo_l.zip" }
Copy-Item $found.FullName (Join-Path $recDir "w600k_r50.onnx") -Force

Write-Host "Downloading MiniFASNetV2 ONNX..."
Invoke-WebRequest `
  -Uri "https://raw.githubusercontent.com/QingHeYang/Silent-Face-Anti-Spoofing-onnx/main/onnx/2.7_80x80_MiniFASNetV2.onnx" `
  -OutFile (Join-Path $liveDir "2.7_80x80_MiniFASNetV2.onnx")

Write-Host "Models installed under $root\models"
