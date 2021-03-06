import sbt._
import Keys._

/* To compile with unchecked and deprecation warnings:
$ sbt
> set scalacOptions in ThisBuild ++= Seq("-unchecked", "-deprecation")
> compile
> exit
*/

object BuildSettings {
  val buildSettings = Defaults.coreDefaultSettings ++ Seq(
    organization := "code.winitzki",
    version := "0.0.6",
    scalaVersion := "2.11.8",
    crossScalaVersions := Seq("2.10.2", "2.10.3", "2.10.4", "2.10.5", "2.10.6", "2.11.0", "2.11.1", "2.11.2", "2.11.3", "2.11.4", "2.11.5", "2.11.6", "2.11.7", "2.11.8"),
    resolvers += Resolver.sonatypeRepo("snapshots"),
    resolvers += Resolver.sonatypeRepo("releases"),
    scalacOptions ++= Seq()
  )
}

object MyBuild extends Build {
  import BuildSettings._

  lazy val joinrun: Project = Project(
    "joinrun",
    file("."),
    settings = buildSettings ++ Seq(
      libraryDependencies += "org.scalatest" %% "scalatest" % "3.0.0" % "test",
      run <<= run in Compile in lib)
  ) aggregate(macros, lib)

  // Macros for the JoinRun library - the users will need this too.
  lazy val macros: Project = Project(
    "macros",
    file("macros"),
    settings = buildSettings ++ Seq(
      libraryDependencies <+= scalaVersion("org.scala-lang" % "scala-reflect" % _),
      libraryDependencies := {
        CrossVersion.partialVersion(scalaVersion.value) match {
          // if Scala 2.11+ is used, quasiquotes are available in the standard distribution
          case Some((2, scalaMajor)) if scalaMajor >= 11 =>
            libraryDependencies.value
          // in Scala 2.10, quasiquotes are provided by macro paradise
          case Some((2, 10)) =>
            libraryDependencies.value ++ Seq(
              compilerPlugin("org.scalamacros" % "paradise" % "2.1.0-M5" cross CrossVersion.full),
              "org.scalamacros" %% "quasiquotes" % "2.1.0-M5" cross CrossVersion.binary
            )
        }
      },
      libraryDependencies ++= Seq(
        "org.scalatest" %% "scalatest" % "3.0.0" % "test"
      )
    )
  ) dependsOn lib

  // The JoinRun library itself - this is what users depend on.
  lazy val lib: Project = Project(
    "lib",
    file("lib"),
    settings = buildSettings ++ Seq(
      parallelExecution in Test := false,
      libraryDependencies ++= Seq(
        "com.typesafe.akka" %% "akka-actor" % "2.4.12",
        "org.scalatest" %% "scalatest" % "3.0.0" % "test"
      )
    )
  )

  // Benchmarks - users do not need to depend on this.
  lazy val benchmark: Project = Project(
    "benchmark",
    file("benchmark"),
    settings = buildSettings ++ Seq(
      libraryDependencies ++= Seq(
        "org.scalatest" %% "scalatest" % "3.0.0" % "test"
      )
    )
  ) dependsOn lib

}
