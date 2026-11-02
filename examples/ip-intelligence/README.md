# 51Degrees IP Intelligence Engines - Examples

![51Degrees](https://51degrees.com/img/logo.png?utm_source=github&utm_medium=readme&utm_campaign=ip-intelligence-java-examples&utm_content=readme.md&utm_term=51degrees-ip-intelligence-engines-examples "Data rewards the curious") **Java IP Intelligence**

[Developer Documentation](https://51degrees.com/ip-intelligence-java/index.html?utm_source=github&utm_medium=readme&utm_campaign=ip-intelligence-java-examples&utm_content=readme.md&utm_term=51degrees-ip-intelligence-engines-examples "developer documentation")

## Introduction

These examples are not distributed as maven jars and need to be built by you.

## Required Files

See [ip-intelligence-data](https://github.com/51Degrees/ip-intelligence-data/)
repository for instructions on obtaining the necessary data files for on-premise IP intelligence.

**IMPORTANT**: The enterprise IP Intelligence data file must be placed in the `ip-intelligence-data`
directory at the root of this repository. The expected filename is `51Degrees-EnterpriseIpiV41.ipi`.

Two free data files are also usable for many of the examples. The ASN file
(`51Degrees-IPIV4AsnIpiV41.ipi`) is included in the `ip-intelligence-data` submodule, and the
Lite file (`51Degrees-LiteV41.ipi`) can be downloaded by running
`ip-intelligence-data/get-lite-file-from-azure.ps1` (or `.sh`). When the enterprise file is
absent, the tests for examples that work with any data file fall back to the ASN file and
then the Lite file. The OfflineProcessing and PerformanceBenchmark examples need the
RegisteredName property, which the ASN file does not contain, so their tests fall back to
the Lite file only. Tests for examples needing enterprise-only properties (Metrics, Compare
and Suspicious) are skipped instead.

```
ip-intelligence-java-examples/
├── ip-intelligence-data/
│   └── 51Degrees-EnterpriseIpiV41.ipi
├── console/
├── web/
└── shared/
```

The examples locate the data file in the following order:

1. The `51DEGREES_IPI_PATH` environment variable or system property, which can
   be set to an explicit path to the data file.
2. A search of the project folder hierarchy for the expected file name.
3. The expected location `ip-intelligence-data/51Degrees-EnterpriseIpiV41.ipi`
   (the configuration files for the getting started examples fall back to the
   free Lite file `ip-intelligence-data/51Degrees-LiteV41.ipi`).

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
https://configure.51degrees.com/Wkqxf3Bs?utm_source=github&utm_medium=readme&utm_campaign=ip-intelligence-java-examples&utm_content=readme.md&utm_term=cloud-coming-soon. A resource key that also includes
the paid properties used by the examples can be created at
https://configure.51degrees.com/hYzn3TV3?utm_source=github&utm_medium=readme&utm_campaign=ip-intelligence-java-examples&utm_content=readme.md&utm_term=cloud-coming-soon. See https://51degrees.com/pricing?utm_source=github&utm_medium=readme&utm_campaign=ip-intelligence-java-examples&utm_content=readme.md&utm_term=cloud-coming-soon
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
| OfflineProcessing        | Example showing how to ingest a file containing IP addresses and perform IP intelligence processing against the entries. It is intentionally minimal and outputs only a small subset of properties; see the [Property Dictionary](https://51degrees.com/developers/property-dictionary?utm_source=github&utm_medium=readme&utm_campaign=ip-intelligence-java-examples&utm_content=readme.md&utm_term=offline-processing) for the full set you can enable. |
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

**IMPORTANT**: All examples and tests must be run from the repository root directory (`ip-intelligence-java-examples/`).

### Running Tests

To run all tests:

```bash
# From the repository root directory
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
# From the repository root directory
mvn compile exec:java -pl console -Dexec.mainClass="fiftyone.ipintelligence.examples.console.GettingStartedOnPrem"
```

Web examples:

```bash
# From the repository root directory
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
