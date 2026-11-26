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

- Git.
- A JDK, 8 or later. CI tests 8, 11, 17 and 21 (`ci/options.json`), and
  builds and runs the examples on the JDK each runner has (see
  [What CI tests](#pitfalls)).
- Maven 3.8 or later, or the `mvnw` wrapper at the root.
- Network access to GitHub, to Maven Central for the packages the examples
  use, and to `distributor.51degrees.com` for the data files.
- For the data files, `curl`, `gzip` and `md5sum` (`md5` on macOS) on Linux
  and macOS, where the examples download with the shell script, and Windows
  PowerShell 5.1 or PowerShell 7 on Windows, where they use the PowerShell
  script.

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

The examples use the free Lite data files, which need no licence key. They are
not stored in this repository. The `cxx` submodule holds the
[data](https://github.com/51Degrees/data) repository as `cxx/data`, which has a
link to each file, and a script downloads them into `cxx/data/device-detection`
and `cxx/data/ip-intelligence`.

On Linux and macOS, run the shell script.

```
cxx/data/get-data-files.sh
```

On Windows, run the PowerShell script. Windows PowerShell can refuse to run a
script file under the policy Windows sets by default, so run it the way the
examples do.

```
powershell -NoProfile -ExecutionPolicy Bypass -File cxx/data/get-data-files.ps1
```

Where PowerShell 7 is installed, `pwsh cxx/data/get-data-files.ps1` does the
same on any system. Either script downloads every free file, and keeps a file
that is already there. To download one file, give its name, as in
`cxx/data/get-data-files.sh 51Degrees-LiteV4.1.hash`. An example whose free
file is missing runs the script for that file itself.

#### Check the files

- `cxx/data/device-detection/51Degrees-LiteV4.1.hash` is more than 10 MB.
- `cxx/data/ip-intelligence/51Degrees-LiteV41.ipi` is more than 100 MB.

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

Run the examples from the repository root, or from a product's examples
folder as its README does. They look for the data repository, `cxx/data`, in
the folder Java runs in and then in each folder above it, and find a data file
or its `.link` there (`findDataFile` in each examples folder's `shared`
module, `DataFileHelper.java`). When a free data file is not there, the
example runs the download script for its `.link`, with Windows PowerShell on
Windows and `sh` on Linux and macOS. They find `logback.xml` with
`FileFinder`, which searches below the folder Java runs in
(`pipeline/pipeline.core/src/main/java/fiftyone/pipeline/util/FileFinder.java`),
and the root and both examples folders have one. The same commands work on
Windows, Linux and macOS.

```
java -cp examples/device-detection/console/target/device-detection-java-examples.console-4.5.127-SNAPSHOT-jar-with-dependencies.jar fiftyone.devicedetection.examples.console.GettingStartedOnPrem
```

The IP Intelligence example uses the free Lite file,
`ip-intelligence/51Degrees-LiteV41.ipi`, when it is given no argument, and
downloads it first when it is not there.

```
java -cp examples/ip-intelligence/console/target/ip-intelligence-java-examples.console-4.5.127-SNAPSHOT-jar-with-dependencies.jar fiftyone.ipintelligence.examples.console.GettingStartedOnPrem
```

An absolute path as the first argument names another data file. Otherwise the
`_51DEGREES_IPI_PATH` environment variable or system property, when it names
a file that is there, comes before the argument or the Lite file.

On Java 24 or later, add `--enable-native-access=ALL-UNNAMED` before the class
name, as
[examples/device-detection/README.md](examples/device-detection/README.md#native-library-access)
explains, or Java warns about the native library.

### Correct output

When an example downloads its data file, a Logback line first reports
`'<file>' is missing, so it is being downloaded by running` and the command,
and the script's `Downloading` and `Saved` lines follow.

The Device Detection example processes five sets of evidence. For each it
prints `Input values:` and the evidence, then `Results:` with
`Mobile Device`, `Platform Name`, `Platform Version`, `Browser Name`,
`Browser Version`, `Hardware Vendor`, `Hardware Name` and `Device Type`. The
Lite file does not have the last three, so each of them reads
`needs the Enterprise data file`.

The IP Intelligence example looks up `116.154.188.222`, `45.236.48.61` and an
IPv6 address. For each it prints `Input values:` with `query.client-ip`, then
`Results:` with a line for each property, from `RegisteredName` to
`TimeZoneOffset`. Each value is followed by its weighting, as in
`RegisteredCountry: CN (weighting 1.00)`. The Lite file has RegisteredName,
RegisteredOwner and RegisteredCountry, so every other line reads `needs the
Enterprise data file`.

Both then end the same way, where `<tier>` is `Lite` for Device Detection and
`IPIV4Lite` for IP Intelligence.

```
Some of these properties need the Enterprise data file. Contact us for a copy at <the tagged contact-us link>
INFO  ... Used a '<tier>' data file, created <date>, <n> days ago, from location '<path>'
INFO  ... All done
```

Then comes the "Find out more" section, between two rules, with the
product's pages on 51degrees.com and the repositories. A warning that the
data file is more than 28 days old may come before `All done`, which is
expected with the Lite file. The exit code is 0.

### Cloud examples

The cloud examples need a resource key, which a person creates on the
configuration page, so ask for one if none is set. A key with the properties
most of the Device Detection examples use can be made
[here](https://51degrees.com/configure/jqz435Nc?utm_source=github&utm_medium=docs&utm_campaign=51degrees-java&utm_content=agents.md&utm_term=cloud-examples).
The Device Detection, IP Intelligence and Location cloud examples take the
key as their first argument, or from the `_51DEGREES_RESOURCE_KEY`
environment variable or system property, and no other name. The TAC and
native model examples need a key with paid properties, and read it from the
same name.

```
java -cp examples/device-detection/console/target/device-detection-java-examples.console-4.5.127-SNAPSHOT-jar-with-dependencies.jar fiftyone.devicedetection.examples.console.GettingStartedCloud <key>
```

It prints the same `Input values:` and `Results:` blocks as the on-premise
example. `examples/ip-intelligence/README.md` gives the keys to create for
the IP Intelligence examples.

With no key, a cloud example does not fail. It says that it needs a resource
key, names the variable to set, gives the page where a key is created, ends
with its "Find out more" section and exits with code 0. The tests named
`WithoutKey` run each one that way.

The TAC and native model examples need a key that includes
HardwarePlatform.Profiles. With one, they list each device as
`<vendor> <name> (<model>)` under the value looked up. With a key that does
not include it, they say so in one line that links to where a key is created
and to the contact page, make no lookup, end with their "Find out more"
section and exit with code 0. A lookup that matches no device prints
`No devices are associated with this TAC.` or `... this native model name.`
under its heading. When the cloud's list of the key's properties cannot be
fetched, the examples end with the cloud service's error and a non-zero exit
code. The published packages up to 4.5.130 cannot build the pipeline for a
key without the lookup, so with those the examples log
`Aspect properties could not be loaded for fiftyone.devicedetection.cloud.flowelements.HardwareProfileCloudEngine`,
end with `java.lang.RuntimeException: Failed to load aspect properties for element 'hardware'`
and exit with code 1. `TacCloudTest` and `NativeModelCloudTest` fail when the
key in `_51DEGREES_RESOURCE_KEY_BESPOKE` lacks the property.
`HardwareLookupStandInTest` runs both examples against a stand-in for the
cloud service, for a key without the lookup, for lookups that match nothing,
and for a list of properties that fails once or every time.

### Pitfalls

- **A data file that is not there.** `cxx/data` holds a `.link` for each file
  rather than the file. An example whose file is missing runs the download
  script for it, so it needs the network and the tools under "Prerequisites".
  Check the file sizes as above.
- **Missing submodules.** The example then logs `Could not find the data file
  'device-detection/51Degrees-LiteV4.1.hash' which must be somewhere in the
  project space to be found.` Run `git submodule update --init --recursive`. A
  zip download of the repository has no submodules, so clone with Git.
- **Running from outside the repository.** An example started in a folder
  outside the repository, such as one the jar was copied to, finds no
  `cxx/data` above it and no `logback.xml` below it. Start it in the
  repository root or a folder inside it. The same applies to `mvn exec:java`,
  which runs in the folder Maven starts in.
- **The version in the jar name.** A jar carries the examples folder's own
  `<version>`, `4.5.127-SNAPSHOT` on `rc` today, not the version of the
  packages the examples use, `4.5.130`. Use the name in `target`.
- **The `dev` profile.** `-Pdev`, which CI uses, builds the examples against
  the libraries here, which needs `mvn install` at the root first and so the
  native build, with CMake 3.10 or later and a C++ toolchain. It is not needed
  to run the examples.
- **Platforms the packages cover.** The published
  `device-detection.hash.engine.on-premise` 4.5.128 jar has native libraries
  for Windows x64 and x86, Linux x64 and aarch64, and macOS x64 and aarch64.
  The `ip-intelligence.engine.on-premise` 4.5.128 jar has the same, without
  Windows x86. On a platform a jar lacks, the example cannot load the engine.
- **What CI tests.** On a pull request CI builds and tests the examples
  with the `dev` profile (`ci/build-project.ps1` and
  `ci/run-unit-tests.ps1`), and the example tests use the paid data files
  when CI has them. Publish tests them against the packages it has just
  built (`ci/run-integration-tests.ps1`).
  [examples-fresh-clone.yml](.github/workflows/examples-fresh-clone.yml)
  does what [Run the examples](#run-the-examples) says, with the JDK and
  Maven each runner has. It
  builds the console examples against the published packages and runs each
  product's `GettingStartedOnPrem` jar from the repository root with no
  settings. On Ubuntu the example downloads the free data file, as from a
  fresh clone, from a stand-in on the runner that serves the copy common-ci's
  fetch-assets step fetched, so the example downloads nothing from the
  Distributor. The Windows and macOS jobs take that file from the run's
  cache. The Ubuntu job then builds every
  example module, the web examples too, with its tests compiled against
  the published packages, so a test that uses a class only this
  repository has fails there. It runs on each push to `main` and
  `rc`, daily, and on a pull request that changes the examples. Its check
  runs by hand from the repository root after the build, with
  `-ExpectDownload` only when the data file is still a link:
  `pwsh -File ./ci/run-fresh-clone-example.ps1 -Product device-detection -Example fiftyone.devicedetection.examples.console.GettingStartedOnPrem -ExpectDownload`.

## Environment variable names

Every name 51Degrees defines for an environment variable or a Java system
property starts `_51DEGREES_`, each name holds one thing, and no name is
filled from another. A name only CI uses, which no customer sets, has no
prefix and says what it holds. A name the platform or a build tool defines
keeps the name its owner gave it.
[`ci/check-environment-variable-names.ps1`](ci/check-environment-variable-names.ps1)
reads every tracked file and fails on a name read or set that is none of
these. It holds the short lists of CI names and of platform and tool
names. Run it, and its tests in
[`ci/check-environment-variable-names.Tests.ps1`](ci/check-environment-variable-names.Tests.ps1),
after a change that reads or sets a name.

## One reactor, six package trees

The root `pom.xml` lists the six package trees as modules but is not their
parent, so nothing from it reaches the published poms. Each tree keeps its own
parent pom. In the repository they all carry the same development version,
and the libraries reference each other through `${project.version}`, so Maven
resolves those dependencies from the reactor. The examples reference published
versions unless the `dev` profile is active. Keep it that way, so that a
reader of the examples sees the package versions a project of their own would
use.

Each published module is released at its own version, tagged
`<package>/<version>`. The publish writes that version into the module's
`pom.xml`, with the versions of its parent and of the packages it depends on,
so leave the versions in the poms as they are. When a module is added, or one
module starts to depend on another, add it to `ci/packages.json` as well, or
the pull request build fails. `ci/README.md` describes how a release is
planned.

## Data files and `_51DEGREES_PROJECT_ROOT`

Tests and examples find data files with `FileFinder`, which searches below
the folder `_51DEGREES_PROJECT_ROOT` names, read as an environment variable
and then as a system property. The surefire configuration sets the system
property to
`maven.multiModuleProjectDirectory`, which Maven takes from the nearest `.mvn`
folder above the working directory. The only `.mvn` folder is at the root, so
the search covers `cxx/data/device-detection` and `cxx/data/ip-intelligence`
from wherever Maven is run. Do not add a `.mvn` folder below the root.

Those folders are the data repository, a submodule of cxx, where each free
data file is a `<file>.link` until `get-data-files.ps1` or `get-data-files.sh`
downloads it. In CI, `ci/fetch-assets.ps1` puts every file there from
common-ci's assets, which each run caches once, so no test job downloads a
data file. A file added to the data repository needs an entry in
common-ci's `steps/fetch-assets.ps1` before CI can use it. The examples'
`DataFileHelper.findDataFile` runs the script for a file that is missing. Look for a
free file by its folder and name, such as
`device-detection/51Degrees-LiteV4.1.hash`, so the search does not stop at a
file of the same name elsewhere, such as a Git LFS pointer in an old
`cxx/device-detection-data` folder.

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

`pipeline/derived-properties` is the
[derived-properties](https://github.com/51Degrees/derived-properties)
repository. `pipeline.derivedproperty` packages the scripts in its `scripts`
folder as resources, so a script added there needs a `BuiltInScript`
constant, which `BuiltInScriptTests` checks. The package's tests run every
case file and every rejection case in its `tests` folder, so its answers and
its faults are checked against the other languages'.

## Releases

Pull requests go to `rc`, not `main`, and stay drafts until they have been
reviewed, because the nightly merges any pull request into `rc` that is not a
draft and has no review requested. When a release is wanted, one pull request
merges `rc` into `main`, and the "Only rc into main" check fails a pull request
into `main` from any other branch.

## Writing

Plain words, no em dashes, sentence case headings. Nothing in this public
repository names a private repository or an issue in one.

## Tests that fail on timing

A test that fails on a time limit, a performance threshold or a timeout is
fixed, not run again until it passes. Either the limit is set to fit the
slowest runner the test runs on, with each runner's measurement behind it, or
the reason it takes too long is found and fixed. A failure that comes and goes
costs everyone who meets it the time to look into it.
