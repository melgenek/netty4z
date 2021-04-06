import sbt.Keys.{libraryDependencies, name}

inThisBuild(List(
  scalaVersion := "2.13.5",
  version := "0.1"
))

lazy val root = project
  .in(file("."))
  .settings(
    name := "netty4z",
    skip in publish := true
  )
  .aggregate(core, benchmarketing)

lazy val core = project
  .in(file("core"))
  .settings(
    name := "netty4z-core",
    libraryDependencies ++= Seq(
      "io.netty" % "netty-all" % "4.1.60.Final",
      "dev.zio" %% "zio" % "1.0.5",
      "dev.zio" %% "zio-streams" % "1.0.5",
      "dev.zio" %% "zio-test" % "1.0.5" % Test,
      "dev.zio" %% "zio-test-sbt" % "1.0.5" % Test
    )
  )

lazy val benchmarketing = project
  .in(file("benchmarketing"))
  .settings(
    skip in publish := true,
    libraryDependencies ++= Seq(
      "dev.zio" %% "zio-nio" % "1.0.0-RC10"
    )
  )
  .dependsOn(core)
