#!/usr/bin/env pwsh

$component = Get-Content -Path "component.json" | ConvertFrom-Json
$buildImage="$($component.registry)/$($component.name):$($component.version)-build"
$testImage="$($component.registry)/$($component.name):$($component.version)-test"
$rcImage="$($component.registry)/$($component.name):$($component.version)-$($component.build)-rc"

# Remove build files
if (Test-Path "obj") {
    Remove-Item -Recurse -Force -Path "obj"
}
if (Test-Path "lib") {
    Remove-Item -Recurse -Force -Path "lib"
}

# Remove docker images
docker rmi $buildImage --force
docker rmi $testImage --force
docker rmi $rcImage --force
docker image prune --force

# Remove existed containers
docker ps -a | Select-String -Pattern "Exit" | foreach($_) { docker rm $_.ToString().Split(" ")[0] }
