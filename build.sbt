val catsVersion = "2.4.2"
val catsEffectVersion = "3.0.2"
val http4sVersion = "1.0.0-M21"

val catsCore = "org.typelevel" %% "cats-core" % catsVersion
val catsFree = "org.typelevel" %% "cats-free" % catsVersion
val catsLaws = "org.typelevel" %% "cats-laws" % catsVersion
val catsMtl = "org.typelevel" %% "cats-mtl-core" % "0.7.1"
val catsEffect = "org.typelevel" %% "cats-effect" % catsEffectVersion
val http4sBlazeClient = "org.http4s" %% "http4s-blaze-client" % http4sVersion
val http4sCirce = "org.http4s" %% "http4s-circe" % http4sVersion

val simulacrum = "org.typelevel" %% "simulacrum" % "1.0.1"
val kindProjector = compilerPlugin("org.typelevel" % "kind-projector" % "0.11.3" cross CrossVersion.full)
val resetAllAttrs = "org.scalamacros" %% "resetallattrs" % "1.0.0"
val betterMonadicFor = compilerPlugin("com.olegpy" %% "better-monadic-for" % "0.3.1")

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
      catsEffect,
      simulacrum,
      catsLaws % Test,
      munit % Test,
      disciplineMunit % Test,
      kindProjector,
      resetAllAttrs,
      http4sBlazeClient,
      http4sCirce,
    ),
    testFrameworks += new TestFramework("munit.Framework"),
    scalacOptions ++= Seq(
      "-deprecation",
      "-encoding", "UTF-8",
      "-feature",
      "-language:_"
    ),
    Pamflet / sourceDirectory := target.value / "mdoc",
    assembly / assemblyMergeStrategy := {
      case PathList("META-INF", xs @ _*) =>
        (xs map {_.toLowerCase}) match {
          case ("manifest.mf" :: Nil) | ("index.list" :: Nil) | ("dependencies" :: Nil) =>
            MergeStrategy.discard
          case ps @ (x :: xs) if ps.last.endsWith(".sf") || ps.last.endsWith(".dsa") =>
            MergeStrategy.discard
          case "plexus" :: xs =>
            MergeStrategy.discard
          case "services" :: xs =>
            MergeStrategy.filterDistinctLines
          case ("spring.schemas" :: Nil) | ("spring.handlers" :: Nil) =>
            MergeStrategy.filterDistinctLines
          case _ => MergeStrategy.deduplicate
        }
      case x =>
        MergeStrategy.first
    }
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
