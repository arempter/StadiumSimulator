name := "StadiumSimulator"

version := "0.1"

scalaVersion := "2.13.2"
val zioVersion = "1.0.0-RC18-2"

libraryDependencies ++= Seq(
  "dev.zio" %% "zio"     % zioVersion,
  "dev.zio" %% "zio-logging"  % "0.2.8",
  "dev.zio" %% "zio-test"     %  zioVersion % "test",
  "dev.zio" %% "zio-test-sbt" % zioVersion  % "test"
)

testFrameworks := Seq(new TestFramework("zio.test.sbt.ZTestFramework"))
