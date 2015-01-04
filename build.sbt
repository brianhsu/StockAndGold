name := "StockAndGold"

version := "0.0.1"

scalaVersion := "2.11.4"

seq(webSettings :_*)

scalacOptions ++= Seq("-unchecked", "-deprecation", "-feature", "-language:implicitConversions")

resolvers += "bone" at "http://brianhsu.moe/ivy"

libraryDependencies ++= Seq(
  "javax.servlet" % "servlet-api" % "2.5" % "provided",
  "org.eclipse.jetty" % "jetty-webapp" % "8.0.1.v20110908" % "container",
  "org.scala-lang.modules" %% "scala-xml" % "1.0.3",
  "org.apache.httpcomponents" % "httpclient" % "4.3.6",
  "org.bone" %% "soplurk" % "0.3.2",
  "org.igniterealtime.smack" % "smack-java7" % "4.0.1",
  "com.plivo" % "plivo-java" % "3.0.1"
)

libraryDependencies ++= Seq(
  "net.liftweb" %% "lift-webkit" % "2.6-RC2" % "compile->default",
  "net.liftweb" %% "lift-mongodb" % "2.6-RC2",
  "net.liftweb" %% "lift-mongodb-record" % "2.6-RC2"
)

port in container.Configuration := 8081

