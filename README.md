# Epirus Gradle Plugin

Gradle Plugin for the [Epirus Platform](http://web3labs.com/) enabling the automation of smart contract lifecycle
within your development environment.

## Plugin configuration

The minimum Gradle version to run the plugin is `5.+`.

### Using the `buildscript` convention

To install the Epirus Plugin using the old Gradle `buildscript` convention, you should add 
the following to the first line of your build file:

```groovy
buildscript {
    repositories {
        mavenCentral()
    }
    dependencies {
        classpath 'io.epirus:epirus-gradle-plugin:2.0.0'
    }
}

apply plugin: 'io.epirus'
```

### Using the plugins DSL

Alternatively, if you are using the more modern plugins DSL, add the following line to your 
build file:

```groovy
plugins {
    id 'io.epirus' version '2.0.0'
}
```

## Plugin tasks

The plugin depends on the [web3j](https://github.com/web3j/web3j-gradle-plugin) and the 
[Solidity](https://github.com/web3j/solidity-gradle-plugin) plugins so they will add tasks
for compilation and generation of web3j contract wrappers.

In addition, this plugin registers the task `uploadMetadata`, which will upload automatically
the Solidity contract metadata files to your Epirus node.

To customize the plugin with your node URL you can use the plugin extension DSL:
```groovy
epirus {
    url = 'http://user:password@your.epirus.node:port'
}
```

To obtain a list and description of all added tasks, run the command:
```bash
./gradlew tasks --all
```
