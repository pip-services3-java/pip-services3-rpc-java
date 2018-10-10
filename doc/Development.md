# Development and Testing Guide <br/> Pip.Services RPC for Java

This document provides high-level instructions on how to build and test the microservice.

* [Environment Setup](#setup)
* [Installing](#install)
* [Building](#build)
* [Testing](#test)
* [Release](#release)
* [Contributing](#contrib) 

## <a name="setup"></a> Environment Setup

This is a Java project with multiple build targets for Java frameworks. 
To be able to develop and test it you need to install the following components:
- Eclipse Java Photon: https://www.eclipse.org/ 
- Java SE Development Kit 8: https://www.oracle.com/technetwork/java/javase/
- Apache Maven: https://maven.apache.org/download.cgi 

To work with GitHub code repository you need to install Git from: https://git-scm.com/downloads

If you are planning to develop and test using persistent storages other than flat files
you may need to install database servers:
- Download and install MongoDB database from https://www.mongodb.org/downloads

## <a name="install"></a> Installing

After your environment is ready you can check out source code from the Github repository:
```bash
git clone git@github.com:pip-services-java/pip-services-rpc-java.git
```

Then go to the pom.xml file in Maven project and add dependencies:
```bash
<dependency>
  <groupId>org.pipservices</groupId>
  <artifactId>pip-services-rpc</artifactId>
  <version>3.0.0</version>
</dependency>
```

## <a name="build"></a> Building

Build the project from inside the Eclipse using Maven build or command line via command:

```bash
mvn install
```

To generate source code documentation add to pom.xml file <code>maven-javadoc-plugin</code> with configuration and use the command:

```bash
mvn javadoc:javadoc
```
## <a name="test"></a> Testing

Before you execute tests you need to set configuration options in config.json file.
As a starting point you can use example from config.example.json:

```bash
copy config/config.example.yaml config/config.yaml
``` 

The tests can be executed inside the Eclipse via Unit tests. If you prefer to use command line use the following commands:

```bash
mvn test
```

## <a name="release"></a> Release

Detail description of the Maven release publishing procedure 
is described at https://maven.apache.org/repository/guide-central-repository-upload.html or https://central.sonatype.org/pages/ossrh-guide.html.

Another useful release guide: http://kirang89.github.io/blog/2013/01/20/uploading-your-jar-to-maven-central/  

Before publishing a new release you shall register on Sonatype Repository site.

GPG management tool: https://www.gnupg.org/download/index.html.

Then generate gpg key:

```bash
gpg --gen-key
```

In the command output locate KEY_ID:

```bash
gpg: key YOUR_KEY_ID marked as ultimately trusted
```

Upload the gpg public key to servers:

```bash
gpg --keyserver hkp://pgp.mit.edu --send-keys YOUR_KEY_ID
gpg --keyserver hkp://keyserver.ubuntu.com --send-keys YOUR_KEY_ID
```

Save the following settings in ~/.m2/settings.xml file:

```bash
<settings>
  <servers>
      <server>
          <id>ossrh</id>
          <username>USERNAME</username>
          <password>...PASSWORD...</password>
      </server>
      <server>
          <id>sonatype-nexus-snapshots</id>
          <username>USERNAME</username>
          <password>...PASSWORD...</password>
      </server>
      <server>
          <id>nexus-releases</id>
          <username>USERNAME</username>
          <password>...PASSWORD...</password>
      </server>
  </servers>
  <profiles> 
	<profile> 
		<id>ossrh</id>
    		<activation>
       			<activeByDefault>true</activeByDefault>
    		</activation> 
		<properties>
			<gpg.keyname>...KEY_NAME...</gpg.keyname>
			<gpg.executable>gpg</gpg.executable> 
			<gpg.passphrase>...PASSPHRASE...</gpg.passphrase> 
		</properties> 
	</profile> 
  </profiles>
</settings>
```

Update your project pom.xml:

```bash
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <groupId>xxx.xxx</groupId>
    <artifactId>xxx</artifactId>
    <version>0.1</version>

    <parent>
        <groupId>org.sonatype.oss</groupId>
        <artifactId>oss-parent</artifactId>
        <version>7</version>
    </parent>

    <distributionManagement>
        <snapshotRepository>
            <id>ossrh</id>
          <url>https://oss.sonatype.org/content/repositories/snapshots</url>
        </snapshotRepository>
        <repository>
            <id>ossrh</id>
            <url>https://oss.sonatype.org/service/local/staging/deploy/maven2/</url>
        </repository>
    </distributionManagement>
    <build>
        <plugins>
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-source-plugin</artifactId>
                <version>2.2.1</version>
                <executions>
                    <execution>
                        <id>attach-sources</id>
                        <goals>
                            <goal>jar-no-fork</goal>
                        </goals>
                    </execution>
                </executions>
            </plugin>
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-javadoc-plugin</artifactId>
                <version>3.0.0</version>
                <executions>
                    <execution>
                        <id>attach-javadocs</id>
                        <goals>
                            <goal>jar</goal>
                        </goals>
                    </execution>
                </executions>
            </plugin>
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-gpg-plugin</artifactId>
                <version>1.6</version>
                <executions>
                    <execution>
                        <id>sign-artifacts</id>
                        <phase>verify</phase>
                        <goals>
                            <goal>sign</goal>
                        </goals>
                    </execution>
                </executions>
            </plugin>
            <plugin>
                <groupId>org.sonatype.plugins</groupId>
                <artifactId>nexus-staging-maven-plugin</artifactId>
                <version>1.6.7</version>
                <extensions>true</extensions>
                <configuration>
                    <serverId>ossrh</serverId>
                    <nexusUrl>https://oss.sonatype.org/</nexusUrl>
                </configuration>
            </plugin>
        </plugins>
    </build>
</project>
```

Run Maven:

```bash
mvn clean deploy
```

Comment your Jira ticket

This will trigger the synchronization with central for your group id.

## <a name="contrib"></a> Contributing

Developers interested in contributing should read the following instructions:

- [How to Contribute](http://www.pipservices.org/contribute/)
- [Guidelines](http://www.pipservices.org/contribute/guidelines)
- [Styleguide](http://www.pipservices.org/contribute/styleguide)
- [ChangeLog](../CHANGELOG.md)

> Please do **not** ask general questions in an issue. Issues are only to report bugs, request
  enhancements, or request new features. For general questions and discussions, use the
  [Contributors Forum](http://www.pipservices.org/forums/forum/contributors/).

It is important to note that for each release, the [ChangeLog](../CHANGELOG.md) is a resource that will
itemize all:

- Bug Fixes
- New Features
- Breaking Changes