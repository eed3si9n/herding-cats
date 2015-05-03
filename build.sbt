val catsVersion = "0.1.0-SNAPSHOT"
val catsCore    = "org.spire-math" %% "cats-core" % catsVersion
val catsStd     = "org.spire-math" %% "cats-std" % catsVersion
val algebraCore = "org.spire-math" %% "algebra" % "0.2.0-SNAPSHOT" from "http://plastic-idolatry.com/jars/algebra_2.11-0.2.0-SNAPSHOT.jar"
val algebraStd  = "org.spire-math" %% "algebra-std" % "0.2.0-SNAPSHOT" from "http://plastic-idolatry.com/jars/algebra-std_2.11-0.2.0-SNAPSHOT.jar"

lazy val root = (project in file(".")).
  settings(
    organization := "com.eed3si9n",
    name := "herding-cats",
    scalaVersion := "2.11.5",
    libraryDependencies ++= Seq(
      algebraCore, algebraStd,
      catsCore, catsStd
    )
  )
