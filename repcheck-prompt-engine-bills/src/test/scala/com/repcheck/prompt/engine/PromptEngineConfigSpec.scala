package com.repcheck.prompt.engine

import pureconfig.ConfigSource

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class PromptEngineConfigSpec extends AnyFlatSpec with Matchers {

  "PromptEngineConfig" should "load bucket, prefix, and version" in {
    val source = ConfigSource.string("""{ bucket = "repcheck-prompts-dev", prefix = "bills", version = "v1.0.0" }""")
    source.load[PromptEngineConfig] match {
      case Right(config) =>
        config.bucket shouldBe "repcheck-prompts-dev"
        config.prefix shouldBe "bills"
        config.version shouldBe "v1.0.0"
      case Left(failures) => fail(s"expected a loaded config, got $failures")
    }
  }

}
