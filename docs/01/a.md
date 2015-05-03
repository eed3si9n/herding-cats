---
out: sbt.html
---

  [catsdocs]: http://non.github.io/cats/api/

### sbt

Cats is currently experimental. How experimental?
There's no published JAR for it yet, so you have to publish it locally.

After that, you can test it using `build.sbt` this:

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

You can then open the REPL using sbt 0.13.8:

```scala
\$ sbt
> console
Welcome to Scala version 2.11.6 (Java HotSpot(TM) 64-Bit Server VM, Java 1.7.0_79).
Type in expressions to have them evaluated.
Type :help for more information.

scala>
```

There's also [API docs][catsdocs] generated for Cats.
