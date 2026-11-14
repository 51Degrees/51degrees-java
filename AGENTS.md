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

Run the examples from the repository root. They find `logback.xml`, the data
file and their options file with `FileFinder`, which searches only below the
folder Java runs in (`pipeline/pipeline.core/src/main/java/fiftyone/pipeline/util/FileFinder.java`).
When a free data file is not there, the example finds its `.link` the same way
and runs the download script for it (`findDataFile` in each examples folder's
`shared` module, `DataFileHelper.java`), with Windows PowerShell on Windows and
`sh` on Linux and macOS. The same commands work on Windows, Linux and macOS.

```
java -cp examples/device-detection/console/target/device-detection-java-examples.console-4.5.127-SNAPSHOT-jar-with-dependencies.jar fiftyone.devicedetection.examples.console.GettingStartedOnPrem
```

The IP Intelligence example looks for the paid Enterprise file,
`ip-intelligence/51Degrees-EnterpriseIpiV41.ipi`, so give it the Lite file as
its first argument.

```
java -cp examples/ip-intelligence/console/target/ip-intelligence-java-examples.console-4.5.127-SNAPSHOT-jar-with-dependencies.jar fiftyone.ipintelligence.examples.console.GettingStartedOnPrem cxx/data/ip-intelligence/51Degrees-LiteV41.ipi
```

The example downloads the Lite file first when it is not there. In place of
the argument, the `_51DEGREES_IPI_PATH` environment variable or system property
can give the absolute path of a data file that is already there.

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

The cloud examples need a resource key, which a person creates on the
configuration page, so ask for one if none is set. A key with the properties
most of the Device Detection examples use can be made
[here](https://51degrees.com/configure/jqz435Nc?utm_source=github&utm_medium=docs&utm_campaign=51degrees-java&utm_content=agents.md&utm_term=cloud-examples).
The Device Detection cloud example takes the key as its first argument, or
from a `TestResourceKey` environment variable or system property.

```
java -cp examples/device-detection/console/target/device-detection-java-examples.console-4.5.127-SNAPSHOT-jar-with-dependencies.jar fiftyone.devicedetection.examples.console.GettingStartedCloud <key>
```

It prints the same `Input values:` and `Results:` blocks as the on-premise
example. The IP Intelligence cloud examples read their key from
`_51DEGREES_RESOURCE_KEY` first and `TestResourceKey` second, and
`examples/ip-intelligence/README.md` gives the keys to create for them.

### Pitfalls

- **A data file that is not there.** `cxx/data` holds a `.link` for each file
  rather than the file. An example whose file is missing runs the download
  script for it, so it needs the network and the tools under "Prerequisites".
  Check the file sizes as above.
- **Missing submodules.** The example then logs `Could not find the data file
  'device-detection/51Degrees-LiteV4.1.hash' which must be somewhere in the
  project space to be found.` Run `git submodule update --init --recursive`. A
  zip download of the repository has no submodules, so clone with Git.
- **Running from another folder.** `FileFinder` looks only below the folder
  Java runs in, so `java` started in `examples/device-detection`, as the
  examples' README shows, cannot find the data file or its link in `cxx`.
  Start it from the repository root. The same applies to `mvn exec:java`,
  which runs in the folder Maven starts in.
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
  settings, so the free data file is downloaded. It runs on each push to
  `main` and `rc`, daily, and on a pull request that changes the examples.

## Environment variable names

[common-ci's list of environment variable names](https://github.com/51Degrees/common-ci/blob/d8a9c596eabf69dd5581a08e9360cdd1c9b3d9e8/environment-variables.md)
holds every environment variable name that the packages, tests, examples and
CI scripts of every language read, with what each name holds and the
deprecated names each replaced. The list decides. Use the names it gives, and
when a change needs a new name, or reads an old name again, change
[`environment-variables.json`](https://github.com/51Degrees/common-ci/blob/d8a9c596eabf69dd5581a08e9360cdd1c9b3d9e8/environment-variables.json)
in common-ci in the same piece of work. This repository's own name
check, [`ci/check-environment-variable-names.ps1`](ci/check-environment-variable-names.ps1), still keeps a
list of its own. Where the two differ, common-ci's list is the one to
follow.

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

## Data files and `project.root`

Tests and examples find data files with `FileFinder`, which searches below
`project.root`. The surefire configuration sets that to
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

## Releases

Pull requests go to `rc`, not `main`, and stay drafts until they have been
reviewed, because the nightly merges any pull request into `rc` that is not a
draft and has no review requested. When a release is wanted, one pull request
merges `rc` into `main`, and the "Only rc into main" check fails a pull request
into `main` from any other branch.

## Writing

Plain words, no em dashes, sentence case headings. Nothing in this public
repository names a private repository or an issue in one.
