---
out: sbt.html
---

  [catsdocs]: http://typelevel.org/cats/api/#package

### sbt

> Cats は現在開発段階にある新しいプロジェクトだ。改善のためのフィードバックやコントリビューションを歓迎する。このプロジェクトは速いペースで変化している途中なので、1.0 リリースが出てくるまでは互換性の保証はしない。(現在の予想だと 2016 Q3 あたり)

Cats のリリース版が公開された。

その後、以下のような `build.sbt` で試してみることができる:

```scala
val catsVersion = "1.0.1"
val catsCore = "org.typelevel" %% "cats-core" % catsVersion
val catsFree = "org.typelevel" %% "cats-free" % catsVersion
val catsMtl = "org.typelevel" %% "cats-mtl-core" % "0.2.1"

val simulacrum = "com.github.mpilquist" %% "simulacrum" % "0.11.0"
val macroParadise = compilerPlugin("org.scalamacros" % "paradise" % "2.1.0" cross CrossVersion.full)
val kindProjector = compilerPlugin("org.spire-math" %% "kind-projector" % "0.9.4")
val resetAllAttrs = "org.scalamacros" %% "resetallattrs" % "1.0.0"

val specs2Version = "3.6" // use the version used by discipline
val specs2Core  = "org.specs2" %% "specs2-core" % specs2Version
val specs2Scalacheck = "org.specs2" %% "specs2-scalacheck" % specs2Version
val scalacheck = "org.scalacheck" %% "scalacheck" % "1.12.4"

lazy val root = (project in file(".")).
  settings(
    organization := "com.example",
    name := "something",
    scalaVersion := "2.12.4",
    libraryDependencies ++= Seq(
      catsCore,
      catsFree,
      catsMtl,
      simulacrum,
      specs2Core % Test, specs2Scalacheck % Test, scalacheck % Test,
      macroParadise, kindProjector, resetAllAttrs
    ),
    scalacOptions ++= Seq(
      "-deprecation",
      "-encoding", "UTF-8",
      "-Ypartial-unification",
      "-feature",
      "-language:_"
    )
  )
```

sbt 0.13.16 を用いて REPL を開く:

```scala
\$ sbt
> console
Welcome to Scala 2.12.4 (Java HotSpot(TM) 64-Bit Server VM, Java 1.8.0_144).
Type in expressions for evaluation. Or try :help.

scala>
```

Cats の [API ドキュメント][catsdocs] もある。
