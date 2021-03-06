import sbt._
import Keys._
import play.Project._

object Dependencies {
  val scalatest = "org.scalatest" %% "scalatest" % "1.9" % "test"
  //val codahale = "com.codahale" %% "jerkson" % "0.5.0"
  val fasterxml = "com.fasterxml.jackson.core" % "jackson-databind" % "2.0.0-RC3"
  val squeryl_orm = "org.squeryl" %% "squeryl" % "0.9.5-6"
  val postgresDriver = "postgresql" % "postgresql" % "9.1-901-1.jdbc4"
}

object Resolvers {
  //val codahale = "codahale" at "http://repo.codahale.com"
}

object ApplicationBuild extends Build {

  val appName = "bid_op_client"
  val appVersion = "1.0-SNAPSHOT"

  import Dependencies._
  val appDependencies = Seq(
    // Add your project dependencies here
    jdbc, anorm, scalatest, fasterxml, squeryl_orm, postgresDriver) //, codahale)

  val main = play.Project(appName, appVersion, appDependencies).settings(
    // Add your own project settings here
    testOptions in Test := Nil) /*,
    resolvers += Resolvers.codahale)*/

}
