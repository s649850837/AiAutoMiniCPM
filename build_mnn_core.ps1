$CMAKE_EXE = "D:\Sdk\cmake\3.22.1\bin\cmake.exe"
$NINJA_EXE = "D:\Sdk\cmake\3.22.1\bin\ninja.exe"
$TOOLCHAIN = "D:\Sdk\ndk\25.1.8937393\build\cmake\android.toolchain.cmake"
$SOURCE = "D:\AIWorkSpace\AiAutoMiniCPM\MNN_Repo"
$BUILD = "D:\AIWorkSpace\AiAutoMiniCPM\MNN_Repo\build_android_arm64"

Write-Host "Paths Configured:"
Write-Host "CMAKE: $CMAKE_EXE"
Write-Host "NINJA: $NINJA_EXE"
Write-Host "TOOLCHAIN: $TOOLCHAIN"

# Ensure build directory exists
if (-not (Test-Path $BUILD)) {
    New-Item -ItemType Directory -Path $BUILD | Out-Null
}

# Configure
Write-Host "`n---------- CONFIGURING ----------"
& $CMAKE_EXE -G "Ninja" -S $SOURCE -B $BUILD `
    -DCMAKE_TOOLCHAIN_FILE="$TOOLCHAIN" `
    -DCMAKE_MAKE_PROGRAM="$NINJA_EXE" `
    -DANDROID_ABI="arm64-v8a" `
    -DANDROID_PLATFORM=android-21 `
    -DANDROID_STL=c++_shared `
    -DMNN_BUILD_LLM=ON `
    -DMNN_SUPPORT_TRANSFORMER_FUSE=ON `
    -DMNN_ARM82=ON `
    -DMNN_LOW_MEMORY=ON `
    -DMNN_USE_LOGCAT=ON `
    -DMNN_OPENCL=ON `
    -DMNN_BUILD_OPENCV=ON `
    -DMNN_IMGCODECS=ON `
    -DMNN_SEP_BUILD=OFF `
    -DCMAKE_BUILD_TYPE=Release

if ($LASTEXITCODE -eq 0) {
    Write-Host "`n---------- BUILDING ----------"
    & $CMAKE_EXE --build $BUILD --parallel 8
} else {
    Write-Host "`n---------- CONFIGURATION FAILED ----------"
}
