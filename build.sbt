name := "StreamingClassifier"

version := "1.0"

scalaVersion := "2.12.1"

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-actor" % "2.5.2",
  "com.typesafe.akka" %% "akka-stream" % "2.5.2",
  "com.typesafe.akka" %% "akka-testkit" % "2.5.2" % Test,
  "com.typesafe.akka" %% "akka-stream-testkit" % "2.5.2" % Test,
  "org.scalactic" %% "scalactic" % "3.0.1",
  "org.scalatest" %% "scalatest" % "3.0.1" % "test"
)