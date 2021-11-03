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