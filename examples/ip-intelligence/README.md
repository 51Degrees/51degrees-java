# 51Degrees IP Intelligence Engines - Examples

![51Degrees](https://51degrees.com/img/logo.png?utm_source=github&utm_medium=readme&utm_campaign=51degrees-java&utm_content=readme.md&utm_term=51degrees-ip-intelligence-engines-examples "Data rewards the curious") **Java IP Intelligence**

[Developer Documentation](https://51degrees.com/ip-intelligence-java/index.html?utm_source=github&utm_medium=readme&utm_campaign=51degrees-java&utm_content=readme.md&utm_term=51degrees-ip-intelligence-engines-examples "developer documentation")

## Introduction

These examples are not distributed as maven jars and need to be built by you.

## Required Files

The data files for on-premise IP intelligence are kept in `cxx/data/ip-intelligence`, a
folder of the [data](https://github.com/51Degrees/data) repository, which is a submodule of
cxx.

The examples use the free data files by default, so they run from a fresh clone with no
settings. The examples that show location and network properties use the Lite file
(`51Degrees-LiteV41.ipi`), which has the registration properties, and the performance and
update examples use the much smaller ASN file (`51Degrees-IPIV4AsnIpiV41.ipi`). An example
that cannot find its file runs the data repository's script to download it, and logs the
command it runs. To download both first, run `cxx/data/get-data-files.sh ip-intelligence`
from the root of this repository, or `cxx\data\get-data-files.ps1 ip-intelligence` in
PowerShell on Windows.

Each example shows the properties that only the Enterprise data file has as needing it, and
gives a link to ask for a copy. The Compare, Metrics and Suspicious examples need those
properties, so with a free data file they say which they need and stop.
[Contact us](https://51degrees.com/contact-us?utm_source=github&utm_medium=readme&utm_campaign=51degrees-java&utm_content=readme.md&utm_term=enterprise-data-file) for a copy of
the Enterprise data file. To use it, place it in `cxx/data/ip-intelligence` and pass its path
as the first argument, or set the `51DEGREES_IPI_PATH` environment variable or system
property to its path.

The Enterprise data file is over 6 GB, and the MaxPerformance profile loads all of it into
memory, so the examples built for it use the LowMemory profile, which reads the file from disk
as it is needed. Those are the Compare, Metrics, Suspicious and UpdateDataFile examples. The
Metrics and Compare examples take a profile as their fourth argument, and `MaxPerformance`
there makes them faster on a machine with memory to hold the whole file.

The `ExamplesWithoutSettingsTest` tests run each console example as a fresh clone does. The
other tests use the Enterprise data file, `51Degrees-EnterpriseIpiV41.ipi`, when it is in
`cxx/data/ip-intelligence`. When it is absent, the tests for examples that work with any data
file fall back to the ASN file and then the Lite file. The OfflineProcessing and
PerformanceBenchmark tests fall back to the Lite file only, and the tests of the Metrics,
Compare and Suspicious examples with the Enterprise data file are skipped.

```
51degrees-java/
├── cxx/
│   └── data/
│       └── ip-intelligence/
│           ├── 51Degrees-LiteV41.ipi
│           └── 51Degrees-IPIV4AsnIpiV41.ipi
└── examples/
    └── ip-intelligence/
        ├── console/
        ├── web/
        └── shared/
```

The examples locate the data file in the following order:

1. The `51DEGREES_IPI_PATH` environment variable or system property, which can
   be set to an explicit path to the data file. It is used in place of any
   file named without a full path.
2. The path given as the first argument, or the free file the example uses
   by default, searched for below the folder the example is run from and then
   below `cxx/data`, so that the examples find it whether they are run from
   the root of this repository or from this folder.
3. A free file that is still missing is downloaded by the data repository's
   script.

This project contains sub-modules - **console**, giving examples that are intended
to be run from the command line/console and **web**, illustrating use
of 51Degrees Web/Servlet integration. There is also a **shared** sub-module
containing various helpers for the examples.

Among other things, the examples illustrate:
- use of the fluent builder to configure a pipeline
- use of a configuration options file to configure a pipeline
- use of the cloud IP intelligence service
- use of the on-premise IP intelligence service
- use of IP intelligence pipeline for offline processing tasks
- configuring IP intelligence trade-offs between speed and conserving memory

## Examples

The tables below describe the examples available in this repository.

### Cloud

Cloud examples require a resource key, supplied via the `TestResourceKey`
environment variable, system property, or command line argument. Resource keys
for IP Intelligence are not yet generally available, so these examples can also
be pointed at a custom hosted cloud endpoint using the `TestCloudEndPoint`
environment variable or system property.

| Example                  | Description                                                                                                                                 |
|--------------------------|---------------------------------------------------------------------------------------------------------------------------------------------|
| GettingStartedCloud (Console) | How to use the 51Degrees cloud IP intelligence service to determine details about IP addresses.                                        |
| GettingStartedWebCloud (Web)  | How to use the 51Degrees cloud IP intelligence service to determine details about IP addresses as part of a simple Java servlet website. |
| GetAllPropertiesCloud    | How to iterate over all properties returned by the cloud IP intelligence service for a given IP address.                                     |
| MetadataCloud            | How to access the meta-data that relates to the properties available from the cloud IP intelligence service.                                 |

Cloud examples need a resource key. The key is read from the
`51DEGREES_RESOURCE_KEY` environment variable or system property first. The
legacy `TestResourceKey` name is still supported and is checked second.

The cloud property tiers changed in May 2026. The examples and this
documentation now reflect what is free and what needs a paid subscription.

- Free tier IP properties are Country, LocationConfidence, Ip and IpV6.
- Paid IP properties used by the examples are CountryCode, CountryCode3,
  Region, State, Town, TimeZoneOffset, RegisteredName, RegisteredOwner,
  RegisteredCountry, IpRangeStart, IpRangeEnd, Latitude, Longitude, Areas
  and AccuracyRadiusMin.

A free resource key selecting the free tier properties can be created at
https://configure.51degrees.com/Wkqxf3Bs?utm_source=github&utm_medium=readme&utm_campaign=51degrees-java&utm_content=readme.md&utm_term=cloud-coming-soon. A resource key that also includes
the paid properties used by the examples can be created at
https://configure.51degrees.com/hYzn3TV3?utm_source=github&utm_medium=readme&utm_campaign=51degrees-java&utm_content=readme.md&utm_term=cloud-coming-soon. See https://51degrees.com/pricing?utm_source=github&utm_medium=readme&utm_campaign=51degrees-java&utm_content=readme.md&utm_term=cloud-coming-soon
to get a paid subscription with more properties.

### On-Premise

| Example                  | Description                                                                                                                                                                                                                    |
|--------------------------|--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| GettingStartedOnPrem (Console) | How to use the 51Degrees on-premise IP intelligence API to determine details about IP addresses.                                                                                                                       |
| GettingStartedWebOnPrem (Web)   | How to use the 51Degrees on-premise IP intelligence service to determine details about IP addresses as part of a simple Java servlet website.                                                                         |
| MetadataOnPrem           | How to access the meta-data that relates to the properties available in IP intelligence.                                                                                                                             |
| MetricsOnPrem            | How to access the metrics that relate to an IP address lookup, such as the area covered by the location returned.                                                                                                             |
| CompareOnPrem            | Compares the results of IP intelligence lookups across multiple data files or configurations for the same set of IP addresses.                                                                                                |
| SuspiciousOnPrem         | How to use IP intelligence results to identify potentially suspicious traffic, using geometric calculations on the location areas returned.                                                                                   |
| GettingStartedApi (Web)  | Hosts a local HTTP API mirroring the endpoints of the 51Degrees cloud service, backed by the on-premise engine. Cloud examples can be pointed at it as a custom endpoint.                                                      |
| OfflineProcessing        | Example showing how to ingest a file containing IP addresses and perform IP intelligence processing against the entries. It is intentionally minimal and outputs only a small subset of properties; see the [Property Dictionary](https://51degrees.com/developers/property-dictionary?utm_source=github&utm_medium=readme&utm_campaign=51degrees-java&utm_content=readme.md&utm_term=offline-processing) for the full set you can enable. |
| PerformanceBenchmark     | How to configure the various performance options and run some simple performance tests for IP intelligence.                                                                                                                    |
| UpdateDataFile           | How to configure the Pipeline to automatically update the IP intelligence data file on startup. Also illustrates 'file watcher'. This will refresh the IP intelligence engine if the specified data file is updated on disk. |

### Mixed

| Example                  | Description                                                                                                                                 |
|--------------------------|---------------------------------------------------------------------------------------------------------------------------------------------|
| GettingStartedMixed (Console) | How to configure a pipeline from an XML options file that can target either the on-premise engine or the cloud service.               |
| GettingStartedCloudMixed (Console) | How to configure a pipeline from an XML options file targeting the cloud service.                                                 |
| GettingStartedWebMixed (Web)  | How to use an XML options file to configure IP intelligence in a Java servlet website.                                                  |
| GettingStartedWebCloudMixed (Web) | How to use an XML options file targeting the cloud service in a Java servlet website.                                               |

## Running Examples and Tests

**IMPORTANT**: All examples and tests must be run from this folder, `examples/ip-intelligence`.

### Running Tests

To run all tests:

```bash
# From examples/ip-intelligence
mvn test
```

To run tests for a specific module:

```bash
# Console tests
mvn test -pl console

# Web tests
mvn test -pl web/getting-started.onprem
```

### Running Examples with Maven

Console examples can be run directly using Maven exec plugin:

```bash
# From examples/ip-intelligence
mvn compile exec:java -pl console -Dexec.mainClass="fiftyone.ipintelligence.examples.console.GettingStartedOnPrem"
```

Web examples:

```bash
# From examples/ip-intelligence
mvn compile exec:java -pl web/getting-started.onprem -Dexec.mainClass="fiftyone.ipintelligence.examples.web.GettingStartedWebOnPrem"
```

The web example will start a Jetty server on port 8081. Access it at: `http://localhost:8081`

### Running Examples from Packaged JARs

Alternatively, you can build "fat" JARs and run examples from the command line.

First, build the packages:

```bash
mvn package
```

This will produce JARs with dependencies inside `target` subfolders.

Then run examples using the JAR:

```bash
java -cp .\console\target\ip-intelligence-java-examples.console-4.4.19-jar-with-dependencies.jar fiftyone.ipintelligence.examples.console.OfflineProcessing
```
