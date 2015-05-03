---
out: sbt.html
---

  [catsdocs]: http://non.github.io/cats/api/

### sbt

Cat は現在実験段階にある。どれぐらい実験段階なのかと言うと、
まだ公開されている JAR が出ていないので、自分で publishLocal する必要がある。

その後、以下のような `build.sbt` で試してみることができる:

```scala
val catsVersion = "0.1.0-SNAPSHOT"
val catsCore    = "org.spire-math" %% "cats-core" % catsVersion
val catsStd     = "org.spire-math" %% "cats-std" % catsVersion
val algebraCore = "org.spire-math" %% "algebra" % "0.2.0-SNAPSHOT" from "http://plastic-idolatry.com/jars/algebra_2.11-0.2.0-SNAPSHOT.jar"
val algebraStd  = "org.spire-math" %% "algebra-std" % "0.2.0-SNAPSHOT" from "http://plastic-idolatry.com/jars/algebra-std_2.11-0.2.0-SNAPSHOT.jar"

lazy val root = (project in file(".")).
  settings(
    organization := "com.example",
    name := "something",
    scalaVersion := "2.11.6",
    libraryDependencies ++= Seq(
      algebraCore, algebraStd,
      catsCore, catsStd
    )
  )
```

sbt 0.13.8 を用いて REPL を開く:

```scala
\$ sbt
> console
Welcome to Scala version 2.11.6 (Java HotSpot(TM) 64-Bit Server VM, Java 1.7.0_79).
Type in expressions to have them evaluated.
Type :help for more information.

scala>
```

Cats の [API ドキュメント][catsdocs] もある。
