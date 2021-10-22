# Contributing

## Requirements

You will need the following tools:

- [Git](https://git-scm.com/)
- [Java 8](http://www.oracle.com/technetwork/java/javase/downloads/jdk8-downloads-2133151.html)
- [SBT](http://www.scala-sbt.org/)

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

Push a Git tag:

~~~ bash
$ git tag v2.0.0
$ git push origin v2.0.0
~~~

Then, reset the compatibility guarantees in `build.sbt`:

~~~
versionPolicyIntention := Compatibility.BinaryAndSourceCompatible
~~~

Commit and push the changes.
