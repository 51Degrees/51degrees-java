# CI for this repository

The workflows in `.github/workflows` call the shared 51Degrees workflows in
[common-ci](https://github.com/51Degrees/common-ci), which call the scripts in
this folder. Each script does once, at the root, what the separate
repositories used to do for one package tree each.

## How the scripts treat the repository

- [packages.ps1](packages.ps1) lists the six package trees, which of them
  publish, the two on-premise engines that build a native library, and the
  generated accessors. The other scripts loop over it rather than naming
  folders themselves.
- The root `pom.xml` builds every tree in one reactor but is not their parent,
  so [build-package.ps1](build-package.ps1) sets the version in each tree
  before it deploys from the root. The examples are built and tested but left
  out of the deploy.
- [build-project.ps1](build-project.ps1) and
  [run-unit-tests.ps1](run-unit-tests.ps1) use the `dev` profile, so a pull
  request tests the examples against the libraries it changes. The publish
  workflow instead tests the examples against the version it built, through
  [run-integration-tests.ps1](run-integration-tests.ps1).
- [fetch-assets.ps1](fetch-assets.ps1) links every data file it downloads into
  `cxx/data/device-detection` or `cxx/data/ip-intelligence`, where the tests
  and examples find them. Those are the folders of the
  [data](https://github.com/51Degrees/data) repository, which is a submodule
  of cxx. The script then runs the data repository's `get-data-files.ps1`,
  which keeps the linked files and downloads the free files that are not
  assets, which is the Asn file.
- [run-performance-tests.ps1](run-performance-tests.ps1) measures the Pipeline
  API, Device Detection and IP Intelligence and writes one results file per
  configuration. The Device Detection and IP Intelligence metrics are prefixed
  with the product, as both use the same names.
- The repository has one version, tagged as a plain version such as `4.5.127`.
  [get-next-package-version.ps1](get-next-package-version.ps1) moves only the
  patch number.

## Secrets

Required:

- `ACCESS_TOKEN`: GitHub
  [access token](https://docs.github.com/en/authentication/keeping-your-account-and-data-secure/managing-your-personal-access-tokens#about-personal-access-tokens)
  for cloning repositories and creating pull requests.

To run the on-premise tests:

- `DEVICE_DETECTION_KEY`: [license key](https://51degrees.com/pricing?utm_source=github&utm_medium=readme&utm_campaign=51degrees-java&utm_content=ci-readme.md&utm_term=secrets)
  for the Device Detection TAC data file and the data file update example.
- `IP_INTELLIGENCE_KEY`: license key for the IP Intelligence Enterprise data
  file.
- `IPI_DATA_FILE_URL`: URL of the IP Intelligence Enterprise data file. The
  IP Intelligence examples and benchmark are skipped without it.
- `DEVICE_DETECTION_URL` (optional): URL of the Device Detection TAC data file.

To run the cloud tests:

- `_51DEGREES_RESOURCE_KEY_BESPOKE`: [resource key](https://51degrees.com/documentation/_info__resource_keys.html?utm_source=github&utm_medium=readme&utm_campaign=51degrees-java&utm_content=ci-readme.md&utm_term=secrets)
  passed to the tests as `TestResourceKey` and `SuperResourceKey`.
- `_51DEGREES_LICENSE_KEY_51DID`: license key for the 51Did cloud tests,
  together with the `_51DEGREES_RESOURCE_KEY_BASIC` and
  `_51DEGREES_RESOURCE_KEY_51DID` variables.

To publish to Maven Central (51Degrees only):

- `MAVEN_SETTINGS`: Central Portal token.
- `JAVA_GPG_KEY_PASSPHRASE` and `JAVA_KEY_PGP_FILE`: the key the packages are
  signed with.
- `CODE_SIGNING_KEY_VAULT_NAME`, `CODE_SIGNING_KEY_VAULT_URL`,
  `CODE_SIGNING_KEY_VAULT_CLIENT_ID`, `CODE_SIGNING_KEY_VAULT_TENANT_ID`,
  `CODE_SIGNING_KEY_VAULT_CLIENT_SECRET`,
  `CODE_SIGNING_KEY_VAULT_CERTIFICATE_NAME` and
  `CODE_SIGNING_KEY_VAULT_CERTIFICATE_DATA`: the key vault that signs the
  native libraries.

## Environment

On Windows, [setup-environment.ps1](setup-environment.ps1) installs WMIC if
the runner does not have it. Maven Surefire needs it to check that Maven is
still alive, and without it can end a long running test that had not failed.
The reasoning is in [install-wmic.ps1](install-wmic.ps1).
