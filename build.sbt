lazy val akkaHttpVersion = "10.0.10"
lazy val akkaVersion    = "2.5.6"

lazy val root = (project in file(".")).
  settings(
    inThisBuild(List(
      organization    := "zhongl",
      scalaVersion    := "2.12.3"
    )),
    name := "di",
    libraryDependencies ++= Seq(
      "com.typesafe.akka" %% "akka-http"            % akkaHttpVersion,
      "com.typesafe.akka" %% "akka-http-spray-json" % akkaHttpVersion,
      "com.typesafe.akka" %% "akka-stream"          % akkaVersion,
      "com.auth0"         %  "java-jwt"             % "3.3.0",
      "org.bouncycastle"  %  "bcpkix-jdk15on"       % "1.58",
      "com.typesafe.akka" %% "akka-http-testkit"    % akkaHttpVersion % Test,
      "com.typesafe.akka" %% "akka-testkit"         % akkaVersion     % Test,
      "com.typesafe.akka" %% "akka-stream-testkit"  % akkaVersion     % Test,
      "org.scalatest"     %% "scalatest"            % "3.0.1"         % Test
    )
  )
