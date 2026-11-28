# Human Confidence example

Human Confidence says how likely it is that a person is looking at a web
page. This folder holds a small web server and one page that show it working,
the way a publisher's website would use it. It's a web demo rather than a
console program, because the browser tests only run in a browser.

| Example | Description |
| --- | --- |
| `HumanConfidence` | A demo web server that serves `page.html` and reads `HumanConfidence` and `HumanConfidenceSource` from a 51Degrees Pipeline, for the passive step and again for the sealed active value. It uses the JDK's own HTTP server, the Cloud Request Engine and the Human Confidence element from `pipeline.cloudrequestengine`, and `org.json`. |

## The flow

The page shows three rows, one for each step.

1. **Passive.** The server processes the visitor's request headers and IP
   address with the Pipeline, and reads the two properties. It adds only the
   headers the cloud lists in its `evidencekeys`, so headers such as
   `Authorization` stay on the server. The Cloud Request Engine names
   `HumanConfidence` and `HumanConfidenceSource`, so the answer carries both
   whatever properties the Resource Key was saved with.
   `HumanConfidenceSource` is `Passive` and `HumanConfidence` is a plain
   level, `High`, `Medium`, or `Low`. The server reads it rather than
   reading it from the page, because a level sent by a browser can be
   forged.
2. **Active.** The page loads the 51Degrees client script. It runs the
   browser tests, which take a few seconds. When the script completes,
   `HumanConfidenceSource` is `Active` and `HumanConfidence` holds a sealed
   value. The browser can't read it or change it.
3. **Active, read by the server.** The page sends the sealed value to the
   server as `humanconfidence`. The server adds it to the Evidence as
   `query.humanconfidence` and processes the request with the Pipeline,
   then reads the same two properties. The example never redeems the
   sealed value itself. The Pipeline redeems it through its normal call to
   the cloud, with the account's License Key, and never caches that call.
   `HumanConfidenceSource` is `Active`, and `HumanConfidence` is the level
   the sealed value redeemed to. A sealed value redeems once, within ten
   seconds of the tests. Where it does not redeem, `HumanConfidence` is
   `Medium`. The page then offers to send the same value again, which
   gives `Medium`.

## What you need

- **A Resource Key** with the properties `HumanConfidence` and
  `HumanConfidenceSource`. At startup the server checks the key. If it
  doesn't include `HumanConfidence`, the console and the page say so, with a
  link to
  [contact us](https://51degrees.com/contact-us?utm_source=github&utm_medium=readme&utm_campaign=51degrees-java&utm_content=web-human-confidence-readme.md&utm_term=what-you-need).
- **Leave the Resource Key's domain list empty,** or include `localhost`.
  The server calls the cloud with the same key, and a call from a server has
  no `Origin` header, so a key limited to domains is refused.
- **A License Key** of the same account. The Pipeline uses it to redeem
  the sealed value. It never goes in the page. Without it the third row
  shows `Medium`.
- A JDK, 8 or later, and Maven or the `mvnw` wrapper at the repository root.

## Environment variables

| Variable | Meaning |
| --- | --- |
| `_51DEGREES_RESOURCE_KEY` | The Resource Key. Required. |
| `_51DEGREES_LICENSE_KEY` | A License Key of the same account. Server side only. Needed for the Pipeline to redeem the sealed value. |
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
3. The third row shows `Active` and the level the sealed value redeemed to.
4. **Send the same value again** shows `Medium`, because a sealed value
   redeems once.

Results vary by browser and device. If the second row stays `Passive`, the
browser tests didn't run. The cloud you're using may not have the tests yet,
or the Resource Key may not include `HumanConfidenceSource`. A sealed value
sent more than ten seconds after the tests gives `Medium`.

## Tests

`HumanConfidenceTest` runs the server against a fake cloud on the local
machine, so it needs no keys and no network. It runs with the other example
tests, or by itself from `examples/device-detection`.

```
../../mvnw -pl web/human-confidence -am test
```

`HumanConfidenceLiveTest` starts the server with a key and asks it for the
passive result as the page does. The production cloud does not serve
`HumanConfidenceSource` yet, so CI runs these tests against the staging
cloud, with two names that only CI sets. `CLOUD_STAGING_ENDPOINT` names the
cloud, the API base including `/api/v4/`, and `RESOURCE_KEY_BESPOKE_STAGING`
holds a Bespoke key that cloud resolves. With that key the answer must have
`HumanConfidenceSource` set to `Passive` and a plain level. The test first
asks the cloud which properties the key includes, and fails, naming the
variable and the property, when `HumanConfidence` is not one of them. It
then asks the cloud for `HumanConfidenceSource` with the key, and fails,
naming the variable, when the answer is not `Passive`. With a key whose
products don't offer the source, in `_51DEGREES_RESOURCE_KEY_BASIC`, the
cloud must answer the source as null, with the reason, and the example's
answer must have no source.

```
../../mvnw -pl web/human-confidence -am test -Dtest=HumanConfidenceLiveTest -Dsurefire.failIfNoSpecifiedTests=false
```

Each test is skipped when its key or `CLOUD_STAGING_ENDPOINT` is not set.
The `Active` source needs the client script to run in a browser, so the
tests don't check it.
[cloud-human-confidence.yml](../../../../.github/workflows/cloud-human-confidence.yml)
runs both, and fails if either is skipped.

## What a run costs

Every call to the cloud is one use against the subscription behind the keys.
A page load makes several. They are the server's passive call, the client
script's calls, and the server's call with the sealed value.

## Find out more

- [Documentation](https://51degrees.com/documentation/?utm_source=github&utm_medium=readme&utm_campaign=51degrees-java&utm_content=web-human-confidence-readme.md&utm_term=find-out-more)
- [Pricing](https://51degrees.com/pricing?utm_source=github&utm_medium=readme&utm_campaign=51degrees-java&utm_content=web-human-confidence-readme.md&utm_term=find-out-more)
- [Contact us](https://51degrees.com/contact-us?utm_source=github&utm_medium=readme&utm_campaign=51degrees-java&utm_content=web-human-confidence-readme.md&utm_term=find-out-more)
