# 51Degrees Java

![51Degrees](https://51degrees.com/img/logo.png?utm_source=github&utm_medium=readme&utm_campaign=51degrees-java&utm_content=readme.md&utm_term=51degrees-java "Data rewards the curious") **Java packages for the Pipeline API, Device Detection, IP Intelligence and Location**

This repository holds the 51Degrees Java packages. Each package is still
published to Maven Central under its own coordinates, and the examples show
how a project of your own uses them.

| Product | Read first | Documentation |
| --- | --- | --- |
| Pipeline API | [pipeline/README.md](pipeline/README.md) | [Java](https://51degrees.com/documentation/apis/pipeline-java/index.html?utm_source=github&utm_medium=readme&utm_campaign=51degrees-java&utm_content=readme.md&utm_term=pipeline-java) |
| Device Detection | [device-detection/README.md](device-detection/README.md) | [Java](https://51degrees.com/documentation/apis/device-detection-java/index.html?utm_source=github&utm_medium=readme&utm_campaign=51degrees-java&utm_content=readme.md&utm_term=device-detection-java) |
| IP Intelligence | [ip-intelligence/README.md](ip-intelligence/README.md) | [Java](https://51degrees.com/documentation/apis/ip-intelligence-java/index.html?utm_source=github&utm_medium=readme&utm_campaign=51degrees-java&utm_content=readme.md&utm_term=ip-intelligence-java) |
| Location | [location/README.md](location/README.md) | [Java](https://51degrees.com/documentation/apis/location-java/index.html?utm_source=github&utm_medium=readme&utm_campaign=51degrees-java&utm_content=readme.md&utm_term=location-java) |

## What is where

| Path | Contents |
| --- | --- |
| `pipeline` | The Pipeline API packages, `com.51degrees:pipeline.*` |
| `device-detection` | The Device Detection packages, `com.51degrees:device-detection*` |
| `ip-intelligence` | The IP Intelligence packages, `com.51degrees:ip-intelligence*` |
| `location` | The Location packages, `com.51degrees:geo-location*` |
| `examples/device-detection` | Console and web examples for Device Detection |
| `examples/ip-intelligence` | Console and web examples for IP Intelligence, including the mixed examples that run both products |
| `cxx` | Submodule of [51degrees-cxx](https://github.com/51Degrees/51degrees-cxx), with the C and C++ code the on-premise engines are built from and the data file submodules |
| `pom.xml` | The reactor that builds everything here. It is never published |
| `ci` | The scripts the shared 51Degrees CI workflows call |

## Getting the code

```
git clone --recurse-submodules https://github.com/51Degrees/51degrees-java.git
cd 51degrees-java
```

The free Device Detection Lite data file in `cxx/device-detection-data` is
stored with [Git LFS](https://git-lfs.github.com/). Run `git lfs pull` in that
folder to fetch it. The IP Intelligence ASN file in `cxx/ip-intelligence-data`
is plain git.

## Building

You need a JDK (8 or later) and either Maven 3.8 or later or the `mvnw`
wrapper in this folder. The two on-premise engines compile their native
library during the build, which needs CMake 3.10 or later and a C++ toolchain:
Visual Studio with the C++ workload on Windows, GCC or Clang elsewhere.

From this folder:

```
mvn install -DskipTests
```

That builds every package in dependency order, so a change to `pipeline` is
seen by `device-detection` in the same build. To skip the native libraries,
for example on a machine without a C++ toolchain, add `-DskipNativeBuild=true`.
To build one product and what it depends on, name its folder:

```
mvn install -DskipTests -pl device-detection -am
```

## Tests

```
mvn surefire:test
```

The cloud tests need a resource key, passed as `-DTestResourceKey=<key>`. The
on-premise IP Intelligence tests need the Lite or Enterprise data file, which
is not in the repository; see `ip-intelligence/README.md` for how to get it.
Tests find data files by searching from the root of this repository, so run
Maven from here rather than from inside a package folder.

## Examples

The examples reference the packages published to Maven Central, which is how
a project of your own uses them. To build and run them against the libraries
in this repository instead, activate the `dev` profile:

```
mvn install -DskipTests -Pdev
```

## Regenerating the SWIG wrappers

The C++ wrappers `Java_Hash_Engine.cpp` and `Java_Ipi_Engine.cpp` are
generated and must not be edited by hand. They include the C++ headers through
`cxx/`, so SWIG needs the root of this repository on its include path. With
SWIG 4.0 or later, from this folder:

```
cd device-detection/device-detection.hash.engine.on-premise/src/main/cxx
swig -c++ -java -I../../../../.. -package fiftyone.devicedetection.hash.engine.onpremise.interop.swig -outdir ../java/fiftyone/devicedetection/hash/engine/onpremise/interop/swig -o Java_Hash_Engine.cpp hash_java.i
```

The IP Intelligence engine has `RegenSwig.ps1` beside its interface file that
does the same.

## Versions

The repository has one version, and every package is released with it. CI
sets the version on each package tree before a release and tags the commit
with the plain version, such as `4.5.127`.

## History

The folders `pipeline`, `device-detection`, `ip-intelligence`, `location` and
the two example folders were brought in from the separate repositories
[pipeline-java](https://github.com/51Degrees/pipeline-java),
[device-detection-java](https://github.com/51Degrees/device-detection-java),
[ip-intelligence-java](https://github.com/51Degrees/ip-intelligence-java),
[location-java](https://github.com/51Degrees/location-java),
[device-detection-java-examples](https://github.com/51Degrees/device-detection-java-examples)
and
[ip-intelligence-java-examples](https://github.com/51Degrees/ip-intelligence-java-examples)
with `git subtree add`, without `--squash`, so every commit keeps its original
SHA. The history of a file before the import is under its old path, so read it
from the imported commit, for example `git log aff7c544 -- pom.xml` for what is
now `pipeline/pom.xml`.

## Licence

[European Union Public Licence v1.2](LICENSE)
