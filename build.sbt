import sbt._
import sbtassembly.AssemblyKeys
import scala.language.postfixOps

enablePlugins(GitVersioning)
git.useGitDescribe := true

name := "sqs-kafka-connect"
organization := "com.hivehome"

scalaVersion in ThisBuild := "2.11.8"
crossScalaVersions := Seq("2.11.8", "2.12.0")

ivyScala := ivyScala.value map {
  _.copy(overrideScalaVersion = true)
}

scalacOptions ++= Seq("-unchecked", "-deprecation", "-feature")
updateOptions := updateOptions.value.withCachedResolution(true)

val artifactoryRepository = "Artifactory" at "https://bgchops.jfrog.io/bgchops/dataplatform-maven-releases/"
resolvers in ThisBuild ++= Seq(
  artifactoryRepository,
  "Confluent" at "http://packages.confluent.io/maven/"
)

publishTo := Some(artifactoryRepository)

lazy val dependencies = Seq(
  "com.amazonaws" % "aws-java-sdk" % Versions.AwsSdk,
  "com.amazonaws" % "amazon-sqs-java-messaging-lib" % Versions.AwsSqsJms,
  "org.apache.avro" % "avro" % Versions.Avro,
  "org.apache.kafka" % "kafka-clients" % Versions.Kafka,
  "org.apache.kafka" % "connect-api" % Versions.Kafka,
  "io.confluent" % "kafka-avro-serializer" % Versions.Confluent,
  "io.confluent" % "kafka-schema-registry-client" % Versions.Confluent,
  "org.slf4j" % "slf4j-api" % Versions.Slf4j,
  "org.slf4j" % "jcl-over-slf4j" % Versions.Slf4j  ,
  "org.scalatest" %% "scalatest" % Versions.ScalaTest % "test,it",
  "org.scalacheck" %% "scalacheck" % Versions.ScalaCheck % "test,it"
) map {
  _.excludeAll(
    ExclusionRule(name = "javax.activation"),
    ExclusionRule(name = "javax.mail.glassfish"),
    ExclusionRule(name = "javax.transaction"),
    ExclusionRule(name = "org.apache.geronimo.specs"),
    ExclusionRule(name = "servlet-api"),
    ExclusionRule(name = "jsr305"),
    ExclusionRule(name = "commons-logging"),
    ExclusionRule(organization = "commons-beanutils"),
    ExclusionRule(organization = "org.ow2.asm", name = "asm"),
    ExclusionRule(organization = "org.jboss.netty"),
    ExclusionRule(organization = "org.apache.spark.unused"),
    ExclusionRule(organization = "com.esotericsoftware")
  )
}

libraryDependencies ++= dependencies

val dpIntegrationTest: Configuration = config("it") extend Test
lazy val root = project.in(file("."))
  .enablePlugins(BuildInfoPlugin)
  .settings(
    buildInfoKeys ++= Seq[BuildInfoKey](
      "latestGitTagVersion" -> "git describe --abbrev=0 --tags".!!.trim
    )
  )
  .configs(dpIntegrationTest)
  .settings(Defaults.itSettings: _*)

// This allows the fat jar to be published to artifactory as part of the release process.
artifact in (Compile, assembly) := {
  val art = (artifact in (Compile, assembly)).value
  art.copy(`classifier` = Some("assembly"))
}
addArtifact(artifact in (Compile, assembly), assembly)

// Must for for javaOptions to be passed properly from outside sbt.
fork := true

// Standard assembly
assemblyJarName in assembly := s"${name.value}.jar"

assemblyOption in assembly := (assemblyOption in assembly).value.copy(includeScala = true)

assemblyMergeStrategy in assembly := {
  case PathList("META-INF", "*") => MergeStrategy.discard
  case PathList("META-INF", "io.netty.versions.properties") => MergeStrategy.last
  case PathList("org", "apache", "kafka", xs@_*) => MergeStrategy.discard
  case PathList("org", "apache", "zookeeper", xs@_*) => MergeStrategy.discard
  case PathList("io", "confluent", xs@_*) => MergeStrategy.discard
  case PathList("org", "apache", xs@_*) => MergeStrategy.last
  case PathList("com", "datastax", "driver", "core", "Driver.properties") => MergeStrategy.first
  case PathList("com", "esotericsoftware", xs@_*) => MergeStrategy.last
  case PathList("com", "google", "common", xs@_*) => MergeStrategy.last
  case PathList("org.apache.geronimo.specs", xs@_*) => MergeStrategy.discard
  case PathList("org", "apache", "geronimo", "specs", xs@_*) => MergeStrategy.discard
  case PathList("javax", "xml", xs@_*) => MergeStrategy.last
  case PathList("org", "joda", "time", xs@_*) => MergeStrategy.first
  case PathList("plugin.properties") => MergeStrategy.discard

  case x =>
    val oldStrategy = (assemblyMergeStrategy in assembly).value
    oldStrategy(x)
}

test in assembly := {}
test in(dpIntegrationTest, assembly) := {}
