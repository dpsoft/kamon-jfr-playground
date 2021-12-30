val scala3Version = "3.1.0"

lazy val root = project
  .in(file("."))
  .settings(
    name := "kamon-jfr-playground",
    version := "0.1.0",

    scalaVersion := scala3Version,

    libraryDependencies += "io.kamon" %% "kamon-core" % "2.4.2",
    libraryDependencies += "io.kamon" %% "kamon-prometheus" % "2.4.2",
    libraryDependencies += "com.novocode" % "junit-interface" % "0.11" % "test",
    libraryDependencies += "ch.qos.logback" % "logback-classic" % "1.2.3",
    libraryDependencies += "org.typelevel" %% "cats-effect" % "3.1.1",
    libraryDependencies += "org.scala-lang.modules" %% "scala-parser-combinators" % "2.1.0",
    libraryDependencies += "org.moditect.jfrunit" % "jfrunit" % "1.0.0.Alpha1" % "test",
    libraryDependencies += "org.scalatest" %% "scalatest" % "3.2.10" % "test"
  )
