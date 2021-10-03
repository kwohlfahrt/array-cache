lazy val cache = (project in file("."))
  .settings(
    name := "Cache",
    exportJars := true,
    libraryDependencies += "org.scalatest" %% "scalatest" % "3.2.10" % Test,
    libraryDependencies += "org.scalatestplus" %% "scalacheck-1-15" % "3.2.9.0" % "test"
  )
