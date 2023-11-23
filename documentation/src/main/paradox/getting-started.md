# Getting Started

Add the following dependency to your `build.sbt` file:

@@@vars
```scala
libraryDependencies += "io.github.endless4s" %% "endless-core" % "$project.version$"
```
@@@

This will pull in the core endless module, containing typeclasses and interpreters. You should add this dependency to the project that contains your business domain logic (typically "domain").

Pekko runtime is available in `endless-runtime-pekko` (for Akka, use `endless-runtime-akka`). There are also helpers for defining protobuf protocols in `endless-protobuf-helpers`, scodec in `endless-scodec-helpers` and circe in `endless-circe-helpers`. Add those dependencies to the project where your application wiring code resides (typically "infrastructure").

You can also clone this repository and run the example application with `sbt run`. 

@@@ warning { title="Compatibility" }
Since Akka/Pekko [does not allow mixed versions](https://doc.akka.io/docs/akka/current/common/binary-compatibility-rules.html#mixed-versioning-is-not-allowed) in a project, Akka/Pekko dependencies of `endless-runtime-akka` and `endless-runtime-pekko` respectively are marked a `Provided`. This means that your application `libraryDependencies` needs to directly include Akka or Pekko as a dependency. The minimal supported Akka version is $akka.min.version$, and Pekko version is $pekko.min.version$.  
@@@