param(
    [Parameter(Mandatory)] [string] $Version,
    [Parameter(Mandatory)] [string] $SdkSha256,
    [Parameter(Mandatory)] [string] $LoaderSha256
)

$ErrorActionPreference = 'Stop'
$ProgressPreference = 'SilentlyContinue'

function Save-PinnedDownload([string] $FileName, [string] $Sha256) {
    $target = Join-Path $env:RUNNER_TEMP $FileName
    Invoke-WebRequest -Uri "https://sdk.lunarg.com/sdk/download/$Version/windows/$FileName" -OutFile $target `
        -MaximumRetryCount 3 -RetryIntervalSec 5
    $digest = (Get-FileHash -LiteralPath $target -Algorithm SHA256).Hash
    if ($digest -ne $Sha256) {
        throw "$FileName has the SHA-256 $digest instead of the pinned $Sha256"
    }
    $target
}

$sdkRoot = Join-Path $env:RUNNER_TEMP "vulkan-sdk-$Version"
$installer = Save-PinnedDownload "vulkansdk-windows-X64-$Version.exe" $SdkSha256
$installation = Start-Process -FilePath $installer -Wait -PassThru -ArgumentList @(
    '--root', $sdkRoot, '--accept-licenses', '--default-answer', '--confirm-command', 'install')
if ($installation.ExitCode -ne 0) {
    throw "The Vulkan SDK installer exited with code $($installation.ExitCode)"
}

$loaderRoot = Join-Path $env:RUNNER_TEMP "vulkan-loader-$Version"
$loaderArchive = Save-PinnedDownload "VulkanRT-X64-$Version-Components.zip" $LoaderSha256
Expand-Archive -LiteralPath $loaderArchive -DestinationPath $loaderRoot

Add-Content -LiteralPath $env:GITHUB_ENV -Value "VULKAN_SDK=$sdkRoot"
Add-Content -LiteralPath $env:GITHUB_PATH -Value (Join-Path $loaderRoot "VulkanRT-X64-$Version-Components" 'x64')
