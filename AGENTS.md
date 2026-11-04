# Working in this repository

If you have been asked to get an example running, follow
[Run the examples](#run-the-examples). It gives every step in order, from a
fresh clone to the output. The sections after it are notes on what is not
obvious from the code, for changing it. Keep them short and current. The
[README](README.md) describes the build, the tests and the layout.

## Run the examples

Run each command from the repository root unless the step says otherwise.
Device Detection's on-premise `GettingStartedOnPrem` is the quickest example
to show, because its data file is about 35 MB. IP Intelligence's on-premise
`GettingStartedOnPrem` needs a download of about 230 MB that unpacks to about
460 MB, so run it second.

The examples use the packages published to Maven Central unless the `dev`
profile is active (see
[One reactor, six package trees](#one-reactor-six-package-trees)). Those
packages carry the native engines, so building only an example folder, as
below, needs no CMake and no C++ toolchain. `mvn install` at the root builds
the native engines from `cxx` and does need them, as the README says.

### Prerequisites

- Git, and [Git LFS](https://git-lfs.github.com/) for the Device Detection
  data file.
- A JDK, 8 or later. CI tests 8, 11, 17 and 21 (`ci/options.json`).
- Maven 3.8 or later, or the `mvnw` wrapper at the root.
- Network access to GitHub, and to Maven Central for the packages the examples
  use.
- For the IP Intelligence data file, PowerShell 7 (`pwsh`) on any system, or
  `curl`, `gunzip` and `md5sum` (`md5` on macOS) for the shell script on Linux
  and macOS.

Clone the repository with its submodules.

```
git clone --recurse-submodules https://github.com/51Degrees/51degrees-java.git
cd 51degrees-java
```

In a clone made without `--recurse-submodules`, fetch the submodules.

```
git submodule update --init --recursive
```

### Data files

The examples use the free Lite data files, which need no licence key. They
come from the `cxx` submodule, and two layouts exist. Read the "Getting the
code" section of `README.md` in your checkout to find out which one applies.

- **The README says to run `git lfs pull`.** This is the layout on `rc` and
  `main` today. The files are in `cxx/device-detection-data` and
  `cxx/ip-intelligence-data`. Follow "Without cxx/data".
- **The README says to run `cxx/data/get-data-files.sh`.** The draft pull
  request [#37](https://github.com/51Degrees/51degrees-java/pull/37) moves the
  examples to the [data](https://github.com/51Degrees/data) repository, which
  cxx carries as `cxx/data`. Once it is merged, follow "With cxx/data".

Go by the README rather than by whether `cxx/data` exists, because the nightly
can bring the folder in with a newer `cxx` before the examples here read it.

#### Without cxx/data

The Device Detection Lite file is stored with Git LFS. Install Git LFS, for
example with `sudo apt-get install git-lfs` on Ubuntu, then pull the file in
its submodule.

```
git lfs install
cd cxx/device-detection-data
git lfs pull
cd ../..
```

A script in `cxx/ip-intelligence-data` downloads the IP Intelligence Lite
file. Run it from inside that folder, because it saves the file in the folder
it runs from.

```
cd cxx/ip-intelligence-data
pwsh ./get-lite-file-from-azure.ps1
cd ../..
```

On Linux and macOS, `./get-lite-file-from-azure.sh` in the same folder does
the same. Either script also downloads the Asn file over the copy committed in
the submodule, so `git status` then shows the submodule as modified, which is
expected.

#### With cxx/data

```
cxx/data/get-data-files.sh
```

On Windows, run `cxx\data\get-data-files.ps1` in PowerShell instead. Either
script downloads every free file into `cxx/data/device-detection` and
`cxx/data/ip-intelligence`, and keeps a file that is already there. In this
layout an example whose free file is missing runs the script for that file
itself.

#### Check the files

- `cxx/device-detection-data/51Degrees-LiteV4.1.hash` is more than 10 MB. A
  file of about 130 bytes that starts with
  `version https://git-lfs.github.com/spec/v1` is a Git LFS pointer, which
  means Git LFS has not pulled the file.
- `cxx/ip-intelligence-data/51Degrees-LiteV41.ipi` is more than 100 MB.

With `cxx/data`, the same files are in `cxx/data/device-detection` and
`cxx/data/ip-intelligence`.

### Build

Build the console examples of a product, with the examples' shared module, in
that product's examples folder. Each build makes a jar that holds the example
and everything it needs.

```
cd examples/device-detection
mvn -pl console -am package -DskipTests
cd ../..
cd examples/ip-intelligence
mvn -pl console -am package -DskipTests
cd ../..
```

With the wrapper, run `../../mvnw` in place of `mvn`, or `..\..\mvnw.cmd` on
Windows. The jars are
`examples/device-detection/console/target/device-detection-java-examples.console-<version>-jar-with-dependencies.jar`
and
`examples/ip-intelligence/console/target/ip-intelligence-java-examples.console-<version>-jar-with-dependencies.jar`,
where `<version>` is the `<version>` in that examples folder's `pom.xml`,
`4.5.127-SNAPSHOT` on `rc` today.

### Run

Run the examples from the repository root. They find `logback.xml`, the data
file and their options file with `FileFinder`, which searches only below the
folder Java runs in (`pipeline/pipeline.core/src/main/java/fiftyone/pipeline/util/FileFinder.java`).
The same commands work on Windows, Linux and macOS.

```
java -cp examples/device-detection/console/target/device-detection-java-examples.console-4.5.127-SNAPSHOT-jar-with-dependencies.jar fiftyone.devicedetection.examples.console.GettingStartedOnPrem
```

The IP Intelligence example looks for the paid Enterprise file,
`ip-intelligence-data/51Degrees-EnterpriseIpiV41.ipi`, so give it the Lite
file as its first argument.

```
java -cp examples/ip-intelligence/console/target/ip-intelligence-java-examples.console-4.5.127-SNAPSHOT-jar-with-dependencies.jar fiftyone.ipintelligence.examples.console.GettingStartedOnPrem cxx/ip-intelligence-data/51Degrees-LiteV41.ipi
```

With `cxx/data`, the argument is `cxx/data/ip-intelligence/51Degrees-LiteV41.ipi`.
The `51DEGREES_IPI_PATH` environment variable or system property, set to an
absolute path, does the same. On Java 24 or later, add
`--enable-native-access=ALL-UNNAMED` before the class name, as
[examples/device-detection/README.md](examples/device-detection/README.md#native-library-access)
explains, or Java warns about the native library.

### Correct output

The Device Detection example processes five sets of evidence. For each it
prints `Input values:` and the evidence, then `Results:` with
`Mobile Device`, `Platform Name`, `Platform Version`, `Browser Name` and
`Browser Version`. Logback lines come before and after. The last ones report
the data file and end the run, with these messages.

```
Used a 'Lite' data file, created <date>, <n> days ago, from location '<path>'
This example is using the 'Lite ' data file. This is used for illustration, ...
All done
```

A warning that the data file is more than 28 days old may come too, which is
expected with the Lite file. The exit code is 0.

The IP Intelligence example looks up `116.154.188.222`, `45.236.48.61` and an
IPv6 address. For each it prints `Input values:` with `query.client-ip`, then
`Results:` with a line for each property, from `RegisteredName` to
`TimeZoneOffset`. The Lite file has RegisteredCountry, RegisteredName and
RegisteredOwner, so a line for a property it does not have reads
`Unknown. The property '<name>' is not available in this data file.` followed
by a link, and the first such line also logs a warning that some properties
need a paid subscription. It then logs the data file in the same way, and
`All done`.

The paid data files have many more properties, as the
[pricing page](https://51degrees.com/pricing?utm_source=github&utm_medium=docs&utm_campaign=51degrees-java&utm_content=agents.md&utm_term=correct-output)
describes.

### Cloud examples

The cloud examples need a resource key, which a person creates in the
configurator, so ask for one if none is set. A key with the properties most of
the Device Detection examples use can be made
[here](https://configure.51degrees.com/jqz435Nc?utm_source=github&utm_medium=docs&utm_campaign=51degrees-java&utm_content=agents.md&utm_term=cloud-examples).
The Device Detection cloud example takes the key as its first argument, or
from a `TestResourceKey` environment variable or system property.

```
java -cp examples/device-detection/console/target/device-detection-java-examples.console-4.5.127-SNAPSHOT-jar-with-dependencies.jar fiftyone.devicedetection.examples.console.GettingStartedCloud <key>
```

It prints the same `Input values:` and `Results:` blocks as the on-premise
example. The IP Intelligence cloud examples read their key from
`51DEGREES_RESOURCE_KEY` first and `TestResourceKey` second, and
`examples/ip-intelligence/README.md` gives the keys to create for them.

### Pitfalls

- **A Git LFS pointer instead of the data file.** The engine cannot load it,
  so the example fails before it prints any results. Check the file sizes as
  above.
- **Missing submodules.** The example then logs `Could not find the data file
  '51Degrees-LiteV4.1.hash' which must be somewhere in the project space to be
  found.` Run `git submodule update --init --recursive`, and pull the Git LFS
  file.
- **Running from another folder.** `FileFinder` looks only below the folder
  Java runs in, so `java` started in `examples/device-detection`, as the
  examples' README shows, cannot find the data file in `cxx`. Start it from
  the repository root. The same applies to `mvn exec:java`, which runs in the
  folder Maven starts in.
- **The version in the jar name.** The examples' READMEs show jar names with
  `4.4.20` and `4.4.19`, which are out of date. Use the name in `target`.
- **The IP Intelligence example without an argument.** It searches for the
  Enterprise file, logs `Failed to find IP Intelligence data file` and stops,
  even when the Lite file is in place.
- **The `dev` profile.** `-Pdev`, which CI uses, builds the examples against
  the libraries here, which needs `mvn install` at the root first and so the
  native build, with CMake 3.10 or later and a C++ toolchain. It is not needed
  to run the examples.
- **Platforms the packages cover.** The published
  `device-detection.hash.engine.on-premise` 4.5.128 jar has native libraries
  for Windows x64 and x86, Linux x64 and aarch64, and macOS x64 and aarch64.
  The `ip-intelligence.engine.on-premise` 4.5.128 jar has the same, without
  Windows x86. On a platform a jar lacks, the example cannot load the engine.
- **Windows line endings.** Git for Windows usually checks files out with
  `core.autocrlf=true`, and `cxx/ip-intelligence-data` has no
  `.gitattributes`, so its shell script gets Windows line endings and the
  shell cannot run it. Use the PowerShell script on Windows.
- **What CI does not test.** On a pull request CI builds and tests the
  examples with the `dev` profile (`ci/build-project.ps1` and
  `ci/run-unit-tests.ps1`), and against the published packages only when it
  publishes (`ci/run-integration-tests.ps1`). It never runs the console
  example jars as above, and the example tests use the paid data files when
  CI has them.

## One reactor, six package trees

The root `pom.xml` lists the six package trees as modules but is not their
parent, so nothing from it reaches the published poms. Each tree keeps its own
parent pom. They all carry the same version, and the libraries reference each
other through `${project.version}`, so Maven resolves those dependencies from
the reactor. The examples reference published versions unless the `dev`
profile is active. Keep it that way: a reader of the examples should see the
package versions a project of their own would use.

Because the root is not a parent, `versions:set` has to run in each tree. CI
does that. When changing versions by hand, run it in each of the six folders.

## Data files and `project.root`

Tests and examples find data files with `FileFinder`, which searches below
`project.root`. The surefire configuration sets that to
`maven.multiModuleProjectDirectory`, which Maven takes from the nearest `.mvn`
folder above the working directory. The only `.mvn` folder is at the root, so
the search covers `cxx/device-detection-data` and `cxx/ip-intelligence-data`
from wherever Maven is run. Do not add a `.mvn` folder below the root.

## Native builds

Each on-premise engine compiles its own copy of the common C code from `cxx`,
because Device Detection and IP Intelligence need it built with different
options. The engine's `src/main/cxx/CMakeLists.txt` includes the root
`cxx/CMakeLists.txt` with the other product and the C tests switched off. The
SWIG interface files and the generated wrappers include through `cxx/`, so the
folder that holds `cxx/` is on the include path and SWIG runs with `-I` set to
the repository root.

The Windows pre-build scripts take a toolset and SDK version from the pom.
`auto`, the default, means CMake picks what is installed.

## Submodules

`cxx` is 51degrees-cxx, moved forward by the nightly submodule update. The
JavaScript templates and the OWID library are submodules inside `pipeline`,
where the Pipeline API expects them.

## Releases

Pull requests go to `rc`, not `main`, and stay drafts until they have been
reviewed, because the nightly merges any pull request into `rc` that is not a
draft and has no review requested. When a release is wanted, one pull request
merges `rc` into `main`, and the "Only rc into main" check fails a pull request
into `main` from any other branch.

## Writing

Plain words, no em dashes, sentence case headings. Nothing in this public
repository names a private repository or an issue in one.
