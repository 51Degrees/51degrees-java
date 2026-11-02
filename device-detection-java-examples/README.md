# 51Degrees Device Detection Engines - Examples

![51Degrees](https://51degrees.com/img/logo.png?utm_source=github&utm_medium=readme&utm_campaign=device-detection-java-examples&utm_content=readme.md&utm_term=51degrees-device-detection-engines-examples "Data rewards the curious") **Java Device Detection**

[Developer Documentation](https://51degrees.com/device-detection-java/index.html?utm_source=github&utm_medium=readme&utm_campaign=device-detection-java-examples&utm_content=readme.md&utm_term=51degrees-device-detection-engines-examples "developer documentation")

## Introduction

These examples are not distributed as maven jars and need to be built by you.

This project contains sub-modules - **console**, giving examples that are intended 
to be run from the command line/console and **web**, illustrating use
of 51Degrees Web/Servlet integration. There is also a **shared** sub-module
containing various helpers for the examples.

Among other things, the examples illustrate:
- use of the fluent builder to configure a pipeline
- use of a configuration options file to configure a pipeline
- use of the cloud device detection service
- use of the on-premise "hash" device detection service
- use of device detection pipeline for off-line processing tasks
- configuring device detection trade-offs between speed and conserving memory

## Cloud resource keys

You will require [resource keys](https://51degrees.com/documentation/_info__resource_keys.html?utm_source=github&utm_medium=readme&utm_campaign=device-detection-java-examples&utm_content=readme.md&utm_term=cloud-resource-keys)
to use the Cloud API, as described on our website. Get resource keys from
our [configurator](https://configure.51degrees.com/?utm_source=github&utm_medium=readme&utm_campaign=device-detection-java-examples&utm_content=readme.md&utm_term=cloud-resource-keys), see our [documentation](https://51degrees.com/documentation/_concepts__configurator.html?utm_source=github&utm_medium=readme&utm_campaign=device-detection-java-examples&utm_content=readme.md&utm_term=cloud-resource-keys) on
how to use this.
 
A resource key configured with the properties needed
to run most of the examples can be obtained [here](https://configure.51degrees.com/jqz435Nc?utm_source=github&utm_medium=readme&utm_campaign=device-detection-java-examples&utm_content=readme.md&utm_term=cloud-resource-keys). 
To use the resource key in the example it can be supplied as a
command line parameter, pasted into the configuration file (where there is one)
or supplied as either an environment variable or a system
property called "TestResourceKey".

Some cloud examples require an enhanced resource key containing a license key. And some
on-premise examples require you to provide a license key. You can find out about 
resource keys and license keys at our [pricing page](https://51degrees.com/pricing?utm_source=github&utm_medium=readme&utm_campaign=device-detection-java-examples&utm_content=readme.md&utm_term=cloud-resource-keys). 

## Running examples with changes to Pipeline packages

A common use case is to make a change to the Pipeline logic in
device-detection-java and then use these examples to observe the results of the
change.

By default, the examples are configured to use the packages from Maven central.
In order to produce and use local packages instead:

- Clone and make your changes to device-detection-java
- Set the version of the device detection packages that we're going to create to 0.0.0:
  `mvn versions:set-property -Dproperty="project.version" "-DnewVersion=0.0.0"`
- Create and install the packages locally (skipping tests is needed):
  `mvn clean install [-DskipTests]`
- Modify the POM for the examples to reference these new local packages. This can
  be done by editing the POM directly or by using the command line:
  `mvn versions:set-property -Dproperty="device-detection.version" "-DnewVersion=0.0.0"`

The same principle can be applied to incorporate changes in pipeline-java if needed.

## Examples

The tables below describe the examples available in this repository.

### Cloud

| Example                                | Description                                                                                                                                        |
|----------------------------------------|----------------------------------------------------------------------------------------------------------------------------------------------------|
| GettingStarted (Console)               | How to use the 51Degrees Cloud service to determine details about a device based on its User-Agent and User-Agent Client Hints HTTP header values. |
| GettingStarted (Web)                   | How to use the 51Degrees Cloud service to determine details about a device as part of a simple Java servlet website.                               |
| Metadata                               | How to access the meta-data that relates to things like the properties populated device detection                                                  |
| TacLookup                              | How to get device details from a TAC (Type Allocation Code) using the 51Degrees cloud service.                                                     |
| NativeModelLookup                      | How to get device details from a native model name using the 51Degrees cloud service.                                                              |

### On-Premise

| Example                  | Description                                                                                                                                                                                                                    |
|--------------------------|--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| GettingStarted (Console) | How to use the 51Degrees on-premise device detection API to determine details about a device based on its User-Agent and User-Agent Client Hints HTTP header values.                                                           |
| GettingStarted (Web)     | How to use the 51Degrees on-premise device detection API to determine details about a device as part of a simple Java servlet website.                                                                                         |
| Metadata                 | How to access the meta-data that relates to things like the properties populated device detection.                                                                                                                             |
| MatchMetrics             | How to view metrics associated with the properties of processing with a Device Detection engine.                                                                                                                               |
| OfflineProcessing        | Example showing how to ingest a file containing data from web requests and perform detection against the entries.                                                                                                              |
| PerformanceBenchmark     | How to configure the various performance options and run some simple performance tests.                                                                                                                                        |
| UpdateOnStartup          | How to configure the Pipeline to automatically update the device detection data file on startup. Also illustrates 'file watcher'. This will refresh the device detection engine if the specified data file is updated on disk. |

## Running built examples from command line

Running

```bash
mvn package
```

will produce "fat" JARs inside `target` subfolders.

Use them with relevant example class entrypoints like:

```bash
java -cp ./console/target/device-detection-java-examples.console-4.4.20-jar-with-dependencies.jar fiftyone.devicedetection.examples.console.OfflineProcessing
```

### Web examples

The web examples are run from the root of this repository, because they find their
web content by a path relative to it. They listen on port 8081 and stop when Enter
is pressed. Set the `PORT` environment variable to listen on another port, in which
case the example runs until the process is stopped:

```bash
PORT=8100 java -jar ./web/getting-started.onprem/target/device-detection-java-examples.web.getting-started.onprem-4.4.20-jar-with-dependencies.jar
```

The on-premise web example takes its data file from the `51DEGREES_DD_PATH`
environment variable, then from `TestDataFile`, and otherwise uses the free Lite file
in `device-detection-data`.

### Native library access

The on-premise examples use a native library, and
[JEP 472](https://openjdk.org/jeps/472) restricts the operations needed to load it.
Java 24 and 25 warn once per calling module, and a later release will refuse the call.

The fat JARs above bundle everything into a single jar on the classpath, so the
permission to use is `ALL-UNNAMED`:

```bash
java -cp ./console/target/device-detection-java-examples.console-4.4.20-jar-with-dependencies.jar --enable-native-access=ALL-UNNAMED fiftyone.devicedetection.examples.console.OfflineProcessing
```

With the Maven exec plugin, use the `exec:exec` goal. `exec:java` runs in the Maven JVM
and its `<arguments>` are passed to `main`, so a JVM flag there has no effect:

```xml
<plugin>
    <groupId>org.codehaus.mojo</groupId>
    <artifactId>exec-maven-plugin</artifactId>
    <configuration>
        <executable>java</executable>
        <arguments>
            <argument>--enable-native-access=ALL-UNNAMED</argument>
            ...
        </arguments>
    </configuration>
</plugin>
```

A fat jar cannot narrow this any further, because everything inside it is part of the
unnamed module. To grant native access to 51Degrees code alone you need the individual
jars, with `pipeline.engines.fiftyone` and `device-detection.hash.engine.on-premise` on
the module path - see the
[device-detection-java README](https://github.com/51Degrees/device-detection-java#native-library-access).
