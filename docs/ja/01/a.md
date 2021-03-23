---
out: sbt.html
---

  [catsdocs]: http://typelevel.org/cats/api/#package

### sbt

Cats を使ってみるための `build.sbt` はこんな感じになる:

```scala
val catsVersion = "2.4.2"
val catsCore = "org.typelevel" %% "cats-core" % catsVersion
val catsFree = "org.typelevel" %% "cats-free" % catsVersion
val catsMtl = "org.typelevel" %% "cats-mtl-core" % "0.7.1"

val simulacrum = "org.typelevel" %% "simulacrum" % "1.0.1"
val kindProjector = compilerPlugin("org.typelevel" % "kind-projector" % "0.11.3" cross CrossVersion.full)
val resetAllAttrs = "org.scalamacros" %% "resetallattrs" % "1.0.0"

val specs2Version = "4.10.6"
val specs2Core  = "org.specs2" %% "specs2-core" % specs2Version
val specs2Scalacheck = "org.specs2" %% "specs2-scalacheck" % specs2Version
val scalacheck = "org.scalacheck" %% "scalacheck" % "1.15.3"

lazy val root = (project in file("."))
  .settings(
    organization := "com.example",
    name := "something",
    scalaVersion := "2.12.4",
    libraryDependencies ++= Seq(
      catsCore,
      catsFree,
      catsMtl,
      simulacrum,
      specs2Core % Test, specs2Scalacheck % Test, scalacheck % Test,
      kindProjector,
      resetAllAttrs,
    ),
    scalacOptions ++= Seq(
      "-deprecation",
      "-encoding", "UTF-8",
      "-feature",
      "-language:_"
    )
  )
```

sbt 1.4.9 を用いて REPL を開く:

```scala
\$ sbt
> console
[info] Starting scala interpreter...
Welcome to Scala 2.13.5 (OpenJDK 64-Bit Server VM, Java 1.8.0_232).
Type in expressions for evaluation. Or try :help.

scala>
```

Cats の [API ドキュメント][catsdocs] もある。
