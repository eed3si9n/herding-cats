---
out: sbt.html
---

  [catsdocs]: http://typelevel.org/cats/api/#package

### sbt

<s>Cats is currently experimental. How experimental?
There's no published JAR for it yet, so you have to publish it locally.</s>

A released version of Cats is now available.

After that, you can test it using `build.sbt` this:

```scala
val catsVersion = "0.6.1"
val catsAll = "org.typelevel" %% "cats" % catsVersion
val macroParaside = compilerPlugin("org.scalamacros" % "paradise" % "2.1.0" cross CrossVersion.full)
val kindProjector = compilerPlugin("org.spire-math" %% "kind-projector" % "0.6.3")
val resetAllAttrs = "org.scalamacros" %% "resetallattrs" % "1.0.0-M1"

val specs2Version = "3.6" // use the version used by discipline
val specs2Core  = "org.specs2" %% "specs2-core" % specs2Version
val specs2Scalacheck = "org.specs2" %% "specs2-scalacheck" % specs2Version
val scalacheck = "org.scalacheck" %% "scalacheck" % "1.12.4"

lazy val root = (project in file(".")).
  settings(
    organization := "com.example",
    name := "something",
    scalaVersion := "2.11.6",
    libraryDependencies ++= Seq(
      catsAll,
      specs2Core % Test, specs2Scalacheck % Test, scalacheck % Test,
      macroParadise, kindProjector, resetAllAttrs
    ),
    scalacOptions ++= Seq(
      "-deprecation",
      "-encoding", "UTF-8",
      "-feature",
      "-language:_"
    )
  )
```

You can then open the REPL using sbt 0.13.12:

```scala
\$ sbt
> console
Welcome to Scala version 2.11.6 (Java HotSpot(TM) 64-Bit Server VM, Java 1.7.0_79).
Type in expressions to have them evaluated.
Type :help for more information.

scala>
```

There's also [API docs][catsdocs] generated for Cats.
