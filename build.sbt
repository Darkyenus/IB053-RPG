
name := "IB053 RPG"

autoScalaLibrary := false

crossPaths := false

javacOptions += "-g"

javaOptions += "-ea"

resolvers += "jitpack" at "https://jitpack.io"

libraryDependencies += "com.github.EsotericSoftware" % "jsonbeans" % "7306654ed3"

libraryDependencies += "org.slf4j" % "slf4j-api" % "1.7.25"
libraryDependencies += "com.github.Darkyenus" % "tproll" % "v1.2.2"

libraryDependencies += "com.koloboke" % "koloboke-api-jdk8" % "1.0.0"
libraryDependencies += "com.koloboke" % "koloboke-impl-jdk8" % "1.0.0"