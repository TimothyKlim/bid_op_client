import sbt._
import Keys._
import PlayProject._

object Dependencies {
  val scalatest = "org.scalatest" %% "scalatest" % "1.8" % "test"
  val codahale = "com.codahale" %% "jerkson" % "0.5.0"
  val fasterxml = "com.fasterxml.jackson.core" % "jackson-databind" % "2.0.0-RC3"
}

object Resolvers {
  val codahale = "codahale" at "http://repo.codahale.com"
}

object ApplicationBuild extends Build {

  val appName = "bid_op_client"
  val appVersion = "1.0-SNAPSHOT"

  import Dependencies._
  val appDependencies = Seq(
    // Add your project dependencies here
    scalatest, codahale, fasterxml)

  val main = PlayProject(appName, appVersion, appDependencies, mainLang = SCALA).settings(
    // Add your own project settings here
    testOptions in Test := Nil,
    resolvers += Resolvers.codahale)

}