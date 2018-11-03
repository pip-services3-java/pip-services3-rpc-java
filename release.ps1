#!/usr/bin/env pwsh

Set-StrictMode -Version latest
$ErrorActionPreference = "Stop"

$component = Get-Content -Path "component.json" | ConvertFrom-Json
$version = ([xml](Get-Content -Path "pom.xml")).project.version

if ($component.version -ne $version) {
    throw "Versions in component.json and pom.xml do not match"
}

# Make sure that:
# 1. GPG has been installed, key was generated and uploaded to a key server;
# 2. Maven has been installed (use v3.3.9 instead of v3.5.4 - otherwise upload will be VERY slow);
# 3. Maven's global settings file (~/.m2/settings.xml) contains all necessary credentials and your key
# 4. ~/.gnupg contains 2 files, each of which contain the lines in double quotation marks:
#   4a. ~/.gnupg/gpg-agent.conf
#   "allow-loopback-pinentry"
#   4b. ~/.gnupg/gpg.conf
#   "pinentry-mode loopback"
# 
# See "Development.md" in the "doc" folder for detailed instructions.
mvn clean deploy