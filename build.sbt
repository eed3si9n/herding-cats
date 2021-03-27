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

lazy val packageSite = taskKey[Unit]("package site")
lazy val doPackageSite = taskKey[File]("package site")
lazy val packageSitePath = settingKey[File]("path for the package")

ThisBuild / scalaVersion := "2.13.5"

lazy val root = (project in file("."))
  .enablePlugins(MdocPlugin)
  .enablePlugins(PamfletPlugin)
  .settings(
    organization := "com.eed3si9n",
    name := "herding-cats",
    libraryDependencies ++= Seq(
      catsCore,
      catsFree,
      catsMtl,
      simulacrum,
      catsLaws % Test,
      munit % Test,
      disciplineMunit % Test,
      kindProjector,
      resetAllAttrs,
    ),
    testFrameworks += new TestFramework("munit.Framework"),
    scalacOptions ++= Seq(
      "-deprecation",
      "-encoding", "UTF-8",
      "-feature",
      "-language:_"
    ),
    Pamflet / sourceDirectory := target.value / "mdoc",
  ).settings(
    packageSitePath := target.value / "herding-cats.tar.gz",
    doPackageSite := {
      val out = packageSitePath.value
      val siteDir = makeSite.value
      val items = ((siteDir ** "*").get map { _.relativeTo(siteDir) }).flatten
      sys.process.Process(s"""tar zcf ${ packageSitePath.value.getAbsolutePath } ${ items.mkString(" ") }""", Some(siteDir)).!
      out
    },
    // packageSite := Def.sequential(/*clean,*/ pfWrite, doPackageSite).value
  )
