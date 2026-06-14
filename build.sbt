import org.typelevel.scalacoptions.ScalacOption
import sbt.Keys.libraryDependencies
import sbt.Def
import Dependencies.*
import com.repcheck.sbt.ExceptionUniquenessPlugin.autoImport.exceptionUniquenessRootPackages

val isScala212: Def.Initialize[Boolean] = Def.setting {
  VersionNumber(scalaVersion.value).matchesSemVer(SemanticSelector("2.12.x"))
}

ThisBuild / dynverSonatypeSnapshots := true

lazy val commonSettings = Seq(
  organization := "com.repcheck",
  scalaVersion := "3.7.3",
  publishTo := Some(
    "GitHub Packages" at s"https://maven.pkg.github.com/Eligio-Taveras/repcheck-prompt-engine-bills"
  ),
  publishMavenStyle := true,
  credentials ++= {
    val envCreds = for {
      user  <- sys.env.get("GITHUB_ACTOR")
      token <- sys.env.get("GITHUB_TOKEN")
    } yield Credentials("GitHub Package Registry", "maven.pkg.github.com", user, token)

    val fileCreds = {
      val f = Path.userHome / ".sbt" / ".github-packages-credentials"
      if (f.exists) Some(Credentials(f)) else None
    }

    envCreds.orElse(fileCreds).toSeq
  },
  resolvers ++= Seq(
    "GitHub Packages - shared-models" at "https://maven.pkg.github.com/Eligio-Taveras/repcheck-shared-models",
    "GitHub Packages - repcheck-utils" at "https://maven.pkg.github.com/Eligio-Taveras/repcheck-utils",
  ),
  libraryDependencies ++= Seq(
    "org.scalatest" %% "scalatest" % "3.2.18" % Test
  ),
  semanticdbEnabled := true,
  tpolecatScalacOptions ++= ScalaCConfig.scalaCOptions,
  tpolecatScalacOptions ++= {
    if (isScala212.value) ScalaCConfig.scalaCOption2_12
    else Set.empty[ScalacOption]
  },

  // WartRemover — enforces FP discipline at compile time
  wartremoverErrors ++= Seq(
    Wart.AsInstanceOf,          // No unsafe casts
    Wart.EitherProjectionPartial, // No .get on Either projections
    Wart.IsInstanceOf,          // No runtime type checks — use pattern matching
    Wart.MutableDataStructures, // No mutable collections
    Wart.Null,                  // No null — use Option
    Wart.OptionPartial,         // No Option.get — use fold/map/getOrElse
    Wart.Return,                // No return statements
    Wart.StringPlusAny,         // No string + any — use interpolation
    Wart.IterableOps,           // No .head/.tail on collections — use headOption
    Wart.TryPartial,            // No Try.get — use fold/recover
    Wart.Var                    // No mutable vars
  ),
  wartremoverWarnings ++= Seq(
    Wart.Throw                  // Warn on bare throw — prefer F.raiseError
  )
)

lazy val root = (project in file("."))
  .aggregate(repcheckpromptenginebills, docGenerator)
  .settings(
    commonSettings,
    name := "repcheck-prompt-engine-bills-root",
    publish / skip := true
  )

lazy val repcheckpromptenginebills = (project in file("repcheck-prompt-engine-bills"))
  .enablePlugins(com.repcheck.sbt.ExceptionUniquenessPlugin)
  .settings(
    commonSettings,
    name := "repcheck-prompt-engine-bills",
    // F4 library: GCS prompt-fragment loader + assembler + tool registry. No HTTP, no streaming.
    libraryDependencies ++= circe ++ pureConfig ++ catsEffect ++ testDeps,
    libraryDependencies += "com.google.cloud" % "google-cloud-storage" % "2.43.2", // GCS Java SDK, Sync-wrapped
    libraryDependencies += "com.google.cloud" % "google-cloud-nio" % "0.127.28" % Test, // LocalStorageHelper in-memory GCS
    libraryDependencies += "com.repcheck" %% "repchecksharedmodels" % "0.1.58", // F1 contracts + §1.7 PromptFragment/chain
    libraryDependencies += "com.repcheck" %% "repcheck-utils" % "0.1.5", // RetryWrapper + DockerRequired tag
    // DockerRequired specs need a fake-gcs-server container; excluded from `sbt test` (run them explicitly, see README)
    Test / testOptions += Tests.Argument(TestFrameworks.ScalaTest, "-l", "DockerRequired"),
    Test / scalacOptions += "-Wconf:msg=unused value of type:s",
    Test / scalacOptions += "-Wconf:msg=is not declared infix:s",
    coverageMinimumStmtPerFile   := 95,
    coverageMinimumBranchPerFile := 95,
    coverageFailOnMinimum         := true,
    // Circe semi-auto derivation for large case classes
    scalacOptions += "-Xmax-inlines:64",
    exceptionUniquenessRootPackages := Seq("com.repcheck")
  )

lazy val docGenerator = (project in file("doc-generator"))
  .settings(
    commonSettings,
    libraryDependencies ++= Seq(
      "com.anthropic" % "anthropic-java" % "2.18.0",
      "org.typelevel" %% "cats-effect" % "3.5.4",
      "ch.qos.logback" % "logback-classic" % "1.5.6"
    ),
    // Exclude WartRemover for this utility project — uses Java SDK patterns
    wartremoverErrors := Seq.empty,
    wartremoverWarnings := Seq.empty,
    // Exclude from coverage — utility project with no unit tests
    coverageEnabled := false
  )
