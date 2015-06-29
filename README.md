[![License](https://img.shields.io/github/license/LordAkkarin/bukkit-plugin-annotations.svg?style=flat-square)](https://www.apache.org/licenses/LICENSE-2.0.txt)
[![Latest Tag](https://img.shields.io/github/tag/LordAkkarin/bukkit-plugin-annotations.svg?style=flat-square&label=Latest Tag)](https://github.com/LordAkkarin/bukkit-plugin-annotations/tags)
[![Latest Release](https://img.shields.io/github/release/LordAkkarin/bukkit-plugin-annotations.svg?style=flat-square&label=Latest Release)](https://github.com/LordAkkarin/bukkit-plugin-annotations/releases)

Plugin Annotations
==================

Table of Contents
-----------------
* [About](#about)
* [Contacts](#contacts)
* [Issues](#issues)
* [Building](#building)
* [Contributing](#contributing)

About
-----

A set of annotations for automagically generating Bukkit plugin descriptors.

Contacts
--------

* [IRC #Akkarin on irc.spi.gt](http://irc.spi.gt/iris/?nick=Guest....&channels=Akkarin&prompt=1) (alternatively #Akkarin on esper.net)
* [GitHub](https://github.com/LordAkkarin/bukkit-plugin-annotations)

Using
-----

When running maven you may simply add a new dependency along with our repository to your ```pom.xml```:

```xml
<repository>
        <id>torchmind</id>
        <url>https://maven.torchmind.com/snapshot/</url>
</repository>

<dependencies>
        <dependency>
                <groupId>com.torchmind.minecraft</groupId>
                <artifactId>plugin-annotations</artifactId>
                <version>1.0-SNAPSHOT</version>
                <scope>compile</scope>
        </dependency>
</dependencies>
```

For a detailed example of the annotations refer to [ExamplePlugin.java](src/test/java/com/torchmind/minecraft/annotation/test/ExamplePlugin.java).
Keep in mind that you will need to enable annotation processing in your compiler configuration to be able to use this library.

Issues
------

You encountered problems with the mod or have a suggestion? Create an issue!

1. Make sure your issue has not been fixed in a newer version (check the list of [closed issues](https://github.com/LordAkkarin/bukkit-plugin-annotations/issues?q=is%3Aissue+is%3Aclosed)
1. Create [a new issue](https://github.com/LordAkkarin/bukkit-plugin-annotations/issues/new) from the [issues page](https://github.com/LordAkkarin/bukkit-plugin-annotations/issues)
1. Enter your issue's title (something that summarizes your issue) and create a detailed description containing:
   - What is the expected result?
   - What problem occurs?
   - How to reproduce the problem?
   - Crash Log (Please use a [Pastebin](http://www.pastebin.com) service)
1. Click "Submit" and wait for further instructions

Building
--------

1. Clone this repository via ```git clone https://github.com/LordAkkarin/bukkit-plugin-annotations.git``` or download a [zip](https://github.com/LordAkkarin/bukkit-plugin-annotations/archive/master.zip)
1. Build the modification by running ```./gradlew build``` (or ```./gradlew.bat build``` on Windows)
1. The resulting jars can be found in ```build/libs```

To prepare a development environment you will need to run these additional commands:
1. ```./gradlew setupDecompWorkspace``` (or ```./gradlew.bat setupDecompWorkspace``` on Windows)
1. ```./gradlew idea``` (or ```./gradlew.bat idea```) for IntelliJ users and ```./gradlew eclipse``` (or ```./gradlew.bat eclipse``` on Windows) for Eclipse users

Contributing
------------

Before you add any major changes to the library you may want to discuss them with us (see [Contact](#contact)) as
we may choose to reject your changes for various reasons. All contributions are applied via [Pull-Requests](https://help.github.com/articles/creating-a-pull-request).
Patches will not be accepted. Also be aware that all of your contributions are made available under the terms of the
[Apache License 2.0](https://www.apache.org/licenses/LICENSE-2.0.txt). Please read the [Contribution Guidelines](CONTRIBUTING.md)
for more information.
