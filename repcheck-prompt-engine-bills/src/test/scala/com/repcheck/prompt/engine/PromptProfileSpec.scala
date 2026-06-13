package com.repcheck.prompt.engine

import scala.concurrent.duration._

import io.circe.parser.decode

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class PromptProfileSpec extends AnyFlatSpec with Matchers {

  "PromptProfile" should "decode the documented shape and lift the loop policy to the F1 type" in {
    val json =
      """{
        |  "systemBlocks": ["system-cluster-concept", "tool-use-follow-up"],
        |  "tools": [{"name":"search_taxonomy","descriptionBlock":"tools/search-taxonomy"}],
        |  "loopPolicy": {"maxIterations": 3, "perCallTimeoutSeconds": 120, "tokenBudget": null}
        |}""".stripMargin
    decode[PromptProfile](json) match {
      case Right(profile) =>
        profile.systemBlocks shouldBe List("system-cluster-concept", "tool-use-follow-up")
        profile.tools.map(_.name) shouldBe List("search_taxonomy")
        val policy = profile.loopPolicy.toLoopPolicy
        policy.maxIterations shouldBe 3
        policy.perCallTimeout shouldBe 120.seconds
        policy.tokenBudget shouldBe None
      case Left(err) => fail(s"expected a decoded profile, got $err")
    }
  }

  it should "carry a tokenBudget when present" in {
    val json =
      """{"systemBlocks":[],"tools":[],"loopPolicy":{"maxIterations":1,"perCallTimeoutSeconds":30,"tokenBudget":4096}}"""
    decode[PromptProfile](json).map(_.loopPolicy.toLoopPolicy.tokenBudget) shouldBe Right(Some(4096))
  }

}
