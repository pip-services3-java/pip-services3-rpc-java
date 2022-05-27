#!/usr/bin/env pwsh

Set-StrictMode -Version latest
$ErrorActionPreference = "Stop"

$component = Get-Content -Path "component.json" | ConvertFrom-Json
$version = ([xml](Get-Content -Path "pom.xml")).project.version

# Verify versions in component.json and pom.xml
if ($component.version -ne $version) {
    throw "Versions in component.json and pom.xml do not match"
}

# Create ~/.m2/settings.xml if not exists
if (!(Test-Path "~/.m2/settings.xml")) {
   # Generate new gpg key
   $genKey = @"
Key-Type: 1
Key-Length: 2048
Subkey-Type: 1
Subkey-Length: 2048
Name-Real: $($env:GPG_USERNAME)
Name-Email: $($env:GPG_EMAIL)
Passphrase: $($env:GPG_PASSPHRASE)
Expire-Date: 0
"@

   Set-Content -Path "genKey" -Value $genKey
   
   $gpgOut = gpg --batch --gen-key genKey

   # Get gpg keyname
   $gpgKeyname = Read-Host "Enter gpg key id, you should see it above (gpg: key YOUR_KEY_ID marked as ultimately trusted)"

    $m2SetingsContent = @"
<?xml version="1.0" encoding="UTF-8"?>
<settings>
   <servers>
      <server>
         <id>ossrh</id>
         <username>$($env:M2_USER)</username>
         <password>$($env:M2_PASS)</password>
      </server>
      <server>
         <id>sonatype-nexus-snapshots</id>
         <username>$($env:M2_USER)</username>
         <password>$($env:M2_PASS)</password>
      </server>
      <server>
         <id>nexus-releases</id>
         <username>$($env:M2_USER)</username>
         <password>$($env:M2_PASS)</password>
      </server>
   </servers>
   <profiles>
      <profile>
         <id>ossrh</id>
         <activation>
            <activeByDefault>true</activeByDefault>
         </activation>
         <properties>
            <gpg.keyname>$gpgKeyname</gpg.keyname>
            <gpg.executable>gpg</gpg.executable>
            <gpg.passphrase>$($env:GPG_PASSPHRASE)</gpg.passphrase>
         </properties>
      </profile>
   </profiles>
</settings>
"@

    if (!(Test-Path "~/.m2")) {
        $null = New-Item -Path "~/.m2" -ItemType "directory"
    }

    Set-Content -Path "~/.m2/settings.xml" -Value $m2SetingsContent
}

# Make sure that:
# 1. GPG has been installed, key was generated and uploaded to a key server;
# 2. Maven has been installed (use v3.3.9 instead of v3.5.4 - otherwise upload will be VERY slow [https://issues.sonatype.org/browse/OSSRH-43371]);
# 3. Maven's global settings file (~/.m2/settings.xml) contains all necessary credentials and your key
# 4. ~/.gnupg contains 2 files, each of which contain the lines in double quotation marks:
#   4a. ~/.gnupg/gpg-agent.conf
#   "allow-loopback-pinentry"
#   4b. ~/.gnupg/gpg.conf
#   "pinentry-mode loopback"
# 

# Release package
mvn clean deploy

# Verify release result
if ($LastExitCode -ne 0) {
    Write-Error "Release failed. Watch logs above. If you run script from local machine - try to remove ~/.m2/settings.xml and rerun a script."
}
