
name := "IB053 RPG"

autoScalaLibrary := false

crossPaths := false

javacOptions += "-g"

resolvers += "jitpack" at "https://jitpack.io"

libraryDependencies += "com.github.EsotericSoftware" % "jsonbeans" % "7306654ed3"

libraryDependencies += "com.koloboke" % "koloboke-api-jdk8" % "1.0.0"
libraryDependencies += "com.koloboke" % "koloboke-impl-jdk8" % "1.0.0"