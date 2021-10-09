lazy val cache = (project in file("."))
  .settings(
    name := "Cache",
    exportJars := true,
    resolvers += "Sonatype OSS Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots",
    libraryDependencies += "org.scalatest" %% "scalatest" % "3.2.10" % Test,
    libraryDependencies += "org.scalatestplus" %% "scalacheck-1-15" % "3.2.9.0" % "test",
    testFrameworks += new TestFramework("org.scalameter.ScalaMeterFramework"),
  ).enablePlugins(JmhPlugin)
