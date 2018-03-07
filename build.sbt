lazy val Http4sVersion = "0.18.0"
lazy val Specs2Version = "4.0.2"
lazy val LogbackVersion = "1.2.3"
lazy val DoobieVersion = "0.5.1"
lazy val H2Version = "1.4.192"
lazy val FlywayVersion = "4.2.0"
lazy val CirceVersion = "0.9.1"
lazy val PureConfigVersion = "0.9.0"
lazy val ScalaTestVersion = "3.0.4"
lazy val ScalaMockVersion = "4.0.0"

lazy val commonSettings = Seq(
  organization := "com.mattkohl",
  name := "https-nlp-server",
  version := "0.0.1-SNAPSHOT",
  scalaVersion := "2.12.4"
)

lazy val root = (project in file("."))
  .configs(IntegrationTest)
  .settings(
    commonSettings,
    Defaults.itSettings,
    libraryDependencies ++= Seq(
      "org.http4s"            %% "http4s-blaze-server"  % Http4sVersion,
      "org.http4s"            %% "http4s-circe"         % Http4sVersion,
      "org.http4s"            %% "http4s-dsl"           % Http4sVersion,
      "org.http4s"            %% "http4s-blaze-client"  % Http4sVersion     % "it,test",

      "org.tpolecat"          %% "doobie-core"          % DoobieVersion,
      "org.tpolecat"          %% "doobie-h2"            % DoobieVersion,
      "org.tpolecat"          %% "doobie-hikari"        % DoobieVersion,

      "com.h2database"        %  "h2"                   % H2Version,

      "org.flywaydb"          %  "flyway-core"          % FlywayVersion,

      "io.circe"              %% "circe-generic"        % CirceVersion,
      "io.circe"              %% "circe-literal"        % CirceVersion      % "it,test",
      "io.circe"              %% "circe-optics"         % CirceVersion      % "it",

      "com.github.pureconfig" %% "pureconfig"           % PureConfigVersion,

      "ch.qos.logback"        %  "logback-classic"      % LogbackVersion,

      "edu.stanford.nlp"      %  "stanford-corenlp"     % "3.8.0",
      "edu.stanford.nlp"      %  "stanford-corenlp"     % "3.8.0" classifier "models",

      "org.specs2"            %% "specs2-core"          % Specs2Version % "test",

      "org.scalatest"         %% "scalatest"            % ScalaTestVersion  % "it,test",
      "org.scalamock"         %% "scalamock"            % ScalaMockVersion  % "test"
    )
  )