# Getting Started

Add the following dependency to your `build.sbt` file:

@@@vars
```scala
libraryDependencies += "io.github.endless4s" %% "endless-core" % "$project.version$"
```
@@@

This will pull in the core endless module, containing typeclasses and interpreters. You should add this as a dependency to your domain project.

Akka runtime is available in `endless-runtime-akka`, and some extra helpers for circe in `endless-circe-helpers`: add those as dependencies to your infrastructure module.

You can also clone this repository and run the example application with `sbt run`. 

@@@ warning { title="Compatibility" }
Since Akka [does not allow mixed versions](https://doc.akka.io/docs/akka/current/common/binary-compatibility-rules.html#mixed-versioning-is-not-allowed) in a project, Akka dependencies of `endless-runtime-akka` are marked a `Provided`. This means that your application `libraryDependencies` needs to directly include Akka as a dependency. The minimal supported Akka version is $akka.min.version$.  
@@@