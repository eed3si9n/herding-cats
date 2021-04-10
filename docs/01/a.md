---
out: sbt.html
---

  [catsdocs]: http://typelevel.org/cats/api/#package

### sbt

Here's a quick `build.sbt` to play with Cats:

```scala
val catsVersion = "2.4.2"
val catsCore = "org.typelevel" %% "cats-core" % catsVersion
val catsFree = "org.typelevel" %% "cats-free" % catsVersion
val catsLaws = "org.typelevel" %% "cats-laws" % catsVersion
val catsMtl = "org.typelevel" %% "cats-mtl-core" % "0.7.1"

val simulacrum = "org.typelevel" %% "simulacrum" % "1.0.1"
val kindProjector = compilerPlugin("org.typelevel" % "kind-projector" % "0.11.3" cross CrossVersion.full)
val resetAllAttrs = "org.scalamacros" %% "resetallattrs" % "1.0.0"
val munit = "org.scalameta" %% "munit" % "0.7.22"
val disciplineMunit = "org.typelevel" %% "discipline-munit" % "1.0.6"

ThisBuild / scalaVersion := "2.13.5"

lazy val root = (project in file("."))
  .settings(
    organization := "com.example",
    name := "something",
    libraryDependencies ++= Seq(
      catsCore,
      catsFree,
      catsMtl,
      simulacrum,
      kindProjector,
      resetAllAttrs,
      catsLaws % Test,
      munit % Test,
      disciplineMunit % Test,
    ),
    scalacOptions ++= Seq(
      "-deprecation",
      "-encoding", "UTF-8",
      "-feature",
      "-language:_"
    )
  )
```

You can then open the REPL using sbt 1.4.9:

```scala
\$ sbt
> console
[info] Starting scala interpreter...
Welcome to Scala 2.13.5 (OpenJDK 64-Bit Server VM, Java 1.8.0_232).
Type in expressions for evaluation. Or try :help.

scala>
```

There's also [API docs][catsdocs] generated for Cats.
