# Contributing

## Requirements

You will need the following tools:

- [Git](https://git-scm.com/)
- [Java 17](https://adoptium.net/temurin/releases/?version=17) (the JDK CI builds with)
- [sbt](https://www.scala-sbt.org/)

## Workflow

Create your own fork of the repository and work in a local branch based on `master`.

## Code Style

We use [scalafmt](https://scalameta.org/scalafmt/) to format the source code, and recommend you to setup
your editor to “format on save”, as documented [here](https://scalameta.org/scalafmt/docs/installation.html).

## Run Tests

From the sbt shell:

~~~
+test
~~~

## Preview Documentation

From the sbt shell:

~~~
documentation/previewSite
~~~

It will open the generated documentation in your browser.

## Publish a Release

Releases are cut from tags: pushing a `v*` tag runs the `release` workflow, which publishes to
Maven Central and redeploys the documentation site.

Before tagging, state what the release guarantees relative to the previous one by setting
`versionPolicyIntention` in `build.sbt`:

| Intention | Meaning | Version bump |
|-----------|---------|--------------|
| `Compatibility.BinaryAndSourceCompatible` | nothing breaks | patch |
| `Compatibility.BinaryCompatible` | source-breaking, still links | minor |
| `Compatibility.None` | breaking | major |

`sbt versionPolicyCheck` verifies the code actually honours that intention against the previously
released version, and `sbt versionCheck` verifies the tag you are about to push matches it. CI runs
both, so run them locally first.

Then push the tag:

~~~ bash
$ git tag v1.1.0
$ git push origin v1.1.0
~~~

Finally, reset the intention for the next development cycle:

~~~
versionPolicyIntention := Compatibility.BinaryAndSourceCompatible
~~~

Commit and push the change.
