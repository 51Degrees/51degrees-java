# Human Confidence example

Human Confidence says how likely it is that a person is looking at a web
page. This folder holds a small web server and one page that show it working,
the way a publisher's website would use it. It's a web demo rather than a
console program, because the browser tests only run in a browser.

| Example | Description |
| --- | --- |
| `HumanConfidence` | A demo web server that serves `page.html`, asks the cloud for the passive level, and redeems the sealed active level server side. It uses the JDK's own HTTP server and client, and `org.json`, with no 51Degrees package needed. |

## The flow

The page shows three rows, one for each step.

1. **Passive.** The server asks the cloud itself, passing on the visitor's
   request headers and IP address. It sends only the headers the cloud lists
   in its `evidencekeys`, so headers such as `Authorization` stay on the
   server. The request names `HumanConfidence` and `HumanConfidenceSource`,
   so the answer carries both whatever properties the Resource Key was saved
   with. The answer has `HumanConfidenceSource` as `Passive` and
   `HumanConfidence` as a plain level, `High`, `Medium`, or `Low`. The server
   asks for it rather than reading it from the page, because a level sent by
   a browser can be forged.
2. **Active.** The page loads the 51Degrees client script. It runs the
   browser tests, which take a few seconds. When the script completes,
   `HumanConfidenceSource` is `Active` and `HumanConfidence` holds a sealed
   value. The browser can't read it or change it.
3. **Redeem.** The page hands the sealed value to the server, which sends it
   to the cloud's `hc/redeem` with the account's License Key and returns the
   answer as it came. A sealed value redeems once, within ten seconds of the
   tests. The page then offers to redeem the same value again, which answers
   `replayed`.

## What you need

- **A Resource Key** with the properties `HumanConfidence` and
  `HumanConfidenceSource`. At startup the server checks the key. If it
  doesn't include `HumanConfidence`, the console and the page say so, with a
  link to
  [contact us](https://51degrees.com/contact-us?utm_source=github&utm_medium=readme&utm_campaign=51degrees-java&utm_content=web-human-confidence-readme.md&utm_term=what-you-need).
- **Leave the Resource Key's domain list empty,** or include `localhost`.
  The server calls the cloud with the same key, and a call from a server has
  no `Origin` header, so a key limited to domains is refused.
- **A License Key** of the same account. The server uses it to redeem. It
  never goes in the page.
- A JDK, 8 or later, and Maven or the `mvnw` wrapper at the repository root.

## Environment variables

| Variable | Meaning |
| --- | --- |
| `_51DEGREES_RESOURCE_KEY` | The Resource Key. Required. |
| `_51DEGREES_LICENSE_KEY` | A License Key of the same account. Server side only. Needed to redeem. |
| `_51DEGREES_CLOUD_ENDPOINT` | Optional. The API base including the `/api/v4/` segment. It defaults to `https://cloud.51degrees.com/api/v4/`. |
| `_51DEGREES_EXAMPLE_HTTP_PORTS` | The port to listen on. It defaults to `5151`. If it lists several, separated by commas, the example listens on the first. |

## How to run

Build the example from `examples/device-detection`.

```
../../mvnw -pl web/human-confidence -am package
```

Then, in a bash shell:

```
export _51DEGREES_RESOURCE_KEY="your Resource Key"
export _51DEGREES_LICENSE_KEY="your License Key"
java -jar web/human-confidence/target/device-detection-java-examples.web.human-confidence-4.5.127-SNAPSHOT-jar-with-dependencies.jar
```

Or in PowerShell:

```
$env:_51DEGREES_RESOURCE_KEY = "your Resource Key"
$env:_51DEGREES_LICENSE_KEY = "your License Key"
java -jar web/human-confidence/target/device-detection-java-examples.web.human-confidence-4.5.127-SNAPSHOT-jar-with-dependencies.jar
```

Open `http://localhost:5151/` in a browser. The server runs until the
process is stopped. To use another cloud address, also set
`_51DEGREES_CLOUD_ENDPOINT` before you start it.

## What you should see

1. The first row shows `Passive` and a level.
2. After a few seconds, the second row shows `Active` and a sealed value.
3. The third row shows the level the server redeemed.
4. **Redeem the same value again** shows `replayed`.

Results vary by browser and device. If the second row stays `Passive`, the
browser tests didn't run. The cloud you're using may not have the tests yet,
or the Resource Key may not include `HumanConfidenceSource`. A sealed value
redeemed more than ten seconds after the tests answers `expired`.

## Tests

`HumanConfidenceTest` runs the server against a fake cloud on the local
machine, so it needs no keys and no network. It runs with the other example
tests, or by itself from `examples/device-detection`.

```
../../mvnw -pl web/human-confidence -am test
```

`HumanConfidenceLiveTest` starts the server against the cloud service that
`_51DEGREES_CLOUD_ENDPOINT` names, and asks it for the passive result as the
page does. It has two tests, and each gives the server the key it checks.
With the key in `_51DEGREES_RESOURCE_KEY_BESPOKE` the answer must have
`HumanConfidenceSource` set to `Passive` and a plain level. With the key in
`_51DEGREES_RESOURCE_KEY_BASIC`, whose products don't offer the source, the
answer must have no source.

```
../../mvnw -pl web/human-confidence -am test -Dtest=HumanConfidenceLiveTest -Dsurefire.failIfNoSpecifiedTests=false
```

Each test is skipped when its key is not set, and fails when the answer
lacks what it checks. The `Active` source needs the client script to run in
a browser, so the tests don't check it.
[cloud-human-confidence.yml](../../../../.github/workflows/cloud-human-confidence.yml)
runs them with the CI resource keys.

## What a run costs

Every call to the cloud is one use against the subscription behind the keys.
A page load makes several. They are the server's passive call, the client
script's calls, and the redeem.

## Find out more

- [Documentation](https://51degrees.com/documentation/?utm_source=github&utm_medium=readme&utm_campaign=51degrees-java&utm_content=web-human-confidence-readme.md&utm_term=find-out-more)
- [Pricing](https://51degrees.com/pricing?utm_source=github&utm_medium=readme&utm_campaign=51degrees-java&utm_content=web-human-confidence-readme.md&utm_term=find-out-more)
- [Contact us](https://51degrees.com/contact-us?utm_source=github&utm_medium=readme&utm_campaign=51degrees-java&utm_content=web-human-confidence-readme.md&utm_term=find-out-more)
