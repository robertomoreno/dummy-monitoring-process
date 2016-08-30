name := "dummy-monitoring-process"

version := "1.0"

scalaVersion := "2.11.8"

libraryDependencies += "com.typesafe.akka" %% "akka-stream-kafka" % "0.11-RC1"
libraryDependencies += "com.datastax.cassandra" % "cassandra-driver-core" % "3.1.0"
libraryDependencies += "org.json4s" % "json4s-jackson_2.11" % "3.4.0"
libraryDependencies += "org.slf4j" % "slf4j-simple" % "1.7.21"



    