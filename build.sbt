val catsVersion = "0.7.2"
val catsAll = "org.typelevel" %% "cats" % catsVersion
val macroParadise = compilerPlugin("org.scalamacros" % "paradise" % "2.1.0" cross CrossVersion.full)
val kindProjector = compilerPlugin("org.spire-math" %% "kind-projector" % "0.6.3")
val resetAllAttrs = "org.scalamacros" %% "resetallattrs" % "1.0.0-M1"

val specs2Version = "3.6" // use the version used by discipline
val specs2Core  = "org.specs2" %% "specs2-core" % specs2Version
val specs2Scalacheck = "org.specs2" %% "specs2-scalacheck" % specs2Version
val scalacheck = "org.scalacheck" %% "scalacheck" % "1.12.4"

lazy val packageSite = taskKey[Unit]("package site")
lazy val doPackageSite = taskKey[File]("package site")
lazy val packageSitePath = settingKey[File]("path for the package")

lazy val root = (project in file(".")).
  settings(
    organization := "com.eed3si9n",
    name := "herding-cats",
    scalaVersion := "2.11.8",
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
    ),
    resolvers ++= Seq(
      Resolver.sonatypeRepo("releases"),
      Resolver.sonatypeRepo("snapshots")
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
