val catsVersion = "0.1.0-SNAPSHOT"
val catsCore    = "org.spire-math" %% "cats-core" % catsVersion
val catsStd     = "org.spire-math" %% "cats-std" % catsVersion
val catsLaws    = "org.spire-math" %% "cats-laws" % catsVersion
val algebraCore = "org.spire-math" %% "algebra" % "0.2.0-SNAPSHOT" from "http://plastic-idolatry.com/jars/algebra_2.11-0.2.0-SNAPSHOT.jar"
val algebraStd  = "org.spire-math" %% "algebra-std" % "0.2.0-SNAPSHOT" from "http://plastic-idolatry.com/jars/algebra-std_2.11-0.2.0-SNAPSHOT.jar"
val algebraLaws = "org.spire-math" %% "algebra-laws" % "0.2.0-SNAPSHOT" from "http://plastic-idolatry.com/jars/algebra-laws_2.11-0.2.0-SNAPSHOT.jar"
val macroParaside = compilerPlugin("org.scalamacros" % "paradise" % "2.1.0-M5" cross CrossVersion.full)
val kindProjector = compilerPlugin("org.spire-math" %% "kind-projector" % "0.5.2")
val resetAllAttrs = "org.scalamacros" %% "resetallattrs" % "1.0.0-M1"

val specs2Version = "2.3.11" // use the version used by discipline
val specs2Core  = "org.specs2" %% "specs2-core" % specs2Version
val specs2Scalacheck = "org.specs2" %% "specs2-scalacheck" % specs2Version
val scalacheck = "org.scalacheck" %% "scalacheck" % "1.11.3"

lazy val packageSite = taskKey[Unit]("package site")
lazy val doPackageSite = taskKey[File]("package site")
lazy val packageSitePath = settingKey[File]("path for the package")

lazy val root = (project in file(".")).
  settings(
    organization := "com.eed3si9n",
    name := "herding-cats",
    scalaVersion := "2.11.5",
    libraryDependencies ++= Seq(
      algebraCore, algebraStd,
      algebraLaws % Test,
      catsCore, catsStd,
      catsLaws % Test, 
      specs2Core % Test, specs2Scalacheck % Test, scalacheck % Test,
      macroParaside, kindProjector, resetAllAttrs
    ),
    scalacOptions ++= Seq(
      "-deprecation",
      "-encoding", "UTF-8",
      "-feature",
      "-language:_"
    )
  ).settings(
    packageSitePath := target.value / "herding-cats.tar.gz",
    doPackageSite := {
      val out = packageSitePath.value
      val siteDir = (target in (Pamflet, pfWrite)).value
      val items = ((siteDir ** "*").get map { _.relativeTo(siteDir) }).flatten
      Process(s"""tar zcf ${ packageSitePath.value.getAbsolutePath } ${ items.mkString(" ") }""", Some(siteDir)).!
      out
    },
    packageSite := Def.sequential(clean, pfWrite, doPackageSite).value
  )
