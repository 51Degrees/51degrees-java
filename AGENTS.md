# Working in this repository

Notes on what is not obvious from the code. Keep them short and current.
Building, testing and the layout are described in the [README](README.md).

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
the search covers `cxx/data/device-detection` and `cxx/data/ip-intelligence`
from wherever Maven is run. Do not add a `.mvn` folder below the root.

Those folders are the data repository, a submodule of cxx, where each free
data file is a `<file>.link` until `get-data-files.ps1` or `get-data-files.sh`
downloads it. `ci/fetch-assets.ps1` runs the script, and the examples'
`DataFileHelper.findDataFile` runs it for a file that is missing. Look for a
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

## Writing

Plain words, no em dashes, sentence case headings. Nothing in this public
repository names a private repository or an issue in one.
