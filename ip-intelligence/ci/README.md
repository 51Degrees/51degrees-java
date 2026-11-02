# API Specific CI/CD Approach
This API complies with the `common-ci` approach.

The following secrets are required:
* `ACCESS_TOKEN` - GitHub [access token](https://docs.github.com/en/authentication/keeping-your-account-and-data-secure/managing-your-personal-access-tokens#about-personal-access-tokens) for cloning repos, creating PRs, etc.
    * Example: `github_pat_l0ng_r4nd0m_s7r1ng`
  
The following secrets are required to run on-premise tests:
* `DEVICE_DETECTION_KEY` - [license key](https://51degrees.com/pricing?utm_source=github&utm_medium=readme&utm_campaign=ip-intelligence-java&utm_content=ci-readme.md&utm_term=api-specific-ci-cd-approach) for downloading assets (enterprise IPI data file)
    * Example: `V3RYL0NGR4ND0M57R1NG`
 
The following secrets are requred to run cloud tests:
* `SUPER_RESOURCE_KEY` - [resource key](https://51degrees.com/documentation/_info__resource_keys.html?utm_source=github&utm_medium=readme&utm_campaign=ip-intelligence-java&utm_content=ci-readme.md&utm_term=api-specific-ci-cd-approach) for integration tests
    * Example: `R4nd0m-S7r1ng`

The following secrets are required for publishing releases (this should only be needed by 51Degrees):
* `JAVA_STAGING_SETTINGS_BASE64` - Base64 encoded settings.xml file which points to the Sonatype staging repo.
* `JAVA_GPG_KEY_PASSPHRASE` - Passphrase string for the 51Degrees GPG key used to sign releases
* `CODE_SIGNING_CERT` - String containing the 51Degrees code signing certificate in PFX format
* `JAVA_KEY_PGP_FILE` - String containing the 51Degrees PGP key
* `CODE_SIGNING_CERT_ALIAS` - Name of the 51Degrees code signing certificate alias
* `CODE_SIGNING_CERT_PASSWORD` - Password for the `CODE_SIGNING_CERT`

The following secrets are optional:
* `IPI_DATA_FILE_URL` - URL for downloading the enterprise IPI data file
    * Default: the 51Degrees Distributor URL using the `DEVICE_DETECTION_KEY` license key

## Environment

On Windows, [setup-environment.ps1](setup-environment.ps1) installs WMIC if the runner does not have it. Maven Surefire needs it to check that Maven is still alive, and without it can end a long running test that had not failed. The reasoning is in [install-wmic.ps1](install-wmic.ps1).

## Integration Tests

The integration testing approach differs from the 'general' inversion of control approach outlined in the [Design.md](https://github.com/51Degrees/common-ci/blob/gh-refact/design.md) as the it cannot be generic. 


The integration tests are conducted to verify the proper functioning of the ip-intelligence-java-examples with the newly built packages. In order to do this, the integration tests use a locally built and installed version of the ip-intelligence-java dependency created in the preceding stages of the pipeline. 

The below chart shows the process of running tests for the ip-intelligence-java-examples. 

```mermaid
graph TD
    B[Clone Examples Repo]
    B --> C[Set up test files]
    C --> F[Enter ip-intelligence-examples directory]
    F --> G[Set package dependency version]
    G --> H[Test Examples]
    H --> I[Copy test results]
    I --> J[Leave RepoPath]
```

It performs the following steps:

1. Clone Examples Repo: Clone the "ip-intelligence-java-examples" repository into the common-ci directory. The branch with the same name as the one under test is used if it exists there, otherwise `main`.
3. Set up test files: Move the 51Degrees-EnterpriseIpiV41.ipi file to the ip-intelligence-java-examples/ip-intelligence-data directory and download Evidence files into ip-intelligence-data directory.
4. Enter ip-intelligence-examples directory: Changes the current working directory to the ip-intelligence-java-examples folder.
5. Set package dependency version: Sets the version of the ip-intelligence package dependency for the examples to the specified Version parameter. This will be the version installed in the local repository found in the .m2 folder.
6. Test Examples: Runs the Maven clean test command, passing the TestResourceKey, SuperResourceKey, and LicenseKey as parameters.
7. Copy test results: Copies the test results from the surefire-reports directory to the ip-intelligence-java/test-results/integration directory.
Leave RepoPath: Changes the current working directory back to the initial location.

## Performance Tests

The performance tests are located in the examples project, therefore, all of the above applies to performance tests.

The output of the Performance Tests is written to the console. To enable the parsing of this output, the "redirectTestOutputToFile" option must be enabled in the examples project. This ensures that the test output is saved to a file, making it accessible for further processing.

The performance test results are then parsed and organized using a hashtable data structure. This approach facilitates the extraction of relevant metrics, such as detections per second and average milliseconds per detection that are then written to a file following the convention outlined in the [Design.md](https://github.com/51Degrees/common-ci/blob/gh-refact/design.md)
