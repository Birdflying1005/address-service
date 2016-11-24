organization := "address-service"

name := "address-service"

version := "0.1-SNAPSHOT"

scalaVersion := "2.11.8"

scalacOptions := Seq("-unchecked", "-deprecation", "-encoding", "utf8")

resolvers += "Sonatype Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots/"

libraryDependencies ++= {
  val akkaV = "2.4.11"
  val reactiveMongoV = "0.12.0"

  Seq(
    "io.netty"                      % "netty-all"                         % "4.1.6.Final",
    "com.typesafe"                  % "config"                            % "1.2.1",
    "com.typesafe.akka"            %% "akka-actor"                        % akkaV,
    "com.typesafe.akka"            %% "akka-http-experimental"            % akkaV,
    "com.typesafe.akka"            %% "akka-http-core"                    % akkaV,
    "com.typesafe.akka"            %% "akka-http-testkit"                 % akkaV,
    "com.typesafe.akka"            %% "akka-http-spray-json-experimental" % akkaV,
    "org.reactivemongo"            %% "reactivemongo"                     % reactiveMongoV,
    "org.reactivemongo"            %% "reactivemongo-akkastream"          % reactiveMongoV,
    "org.scodec"                   %% "scodec-bits"                       % "1.1.2"
  )
}