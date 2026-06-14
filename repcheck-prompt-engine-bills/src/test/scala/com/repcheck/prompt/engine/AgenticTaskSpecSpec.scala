package com.repcheck.prompt.engine

import scala.concurrent.duration._

import io.circe.parser.decode

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import repcheck.shared.models.prompt.PromptStage

class AgenticTaskSpecSpec extends AnyFlatSpec with Matchers {

  private val json =
    """{
      |  "name": "cluster-concept-identification",
      |  "chain": [
      |    {"stage":"system","promptFragmentNames":["system-cluster-concept-identification"],"weight":1.0},
      |    {"stage":"custom","promptFragmentNames":["tool-use-follow-up"],"weight":1.0}
      |  ],
      |  "tools": [{"name":"search_taxonomy","descriptionRef":"tools/search-taxonomy"}],
      |  "loopPolicy": {"maxIterations": 3, "perCallTimeoutSeconds": 120, "tokenBudget": null}
      |}""".stripMargin

  "AgenticTaskSpec" should "decode the staged chain, tools, and loop policy" in {
    decode[AgenticTaskSpec](json) match {
      case Right(taskSpec) =>
        taskSpec.name shouldBe "cluster-concept-identification"
        taskSpec.chain.map(_.stage) shouldBe List(PromptStage.System, PromptStage.Custom)
        taskSpec.tools.map(_.name) shouldBe List("search_taxonomy")
        val policy = taskSpec.loopPolicy.toLoopPolicy
        policy.maxIterations shouldBe 3
        policy.perCallTimeout shouldBe 120.seconds
        policy.tokenBudget shouldBe None
      case Left(err) => fail(s"expected a decoded task spec, got $err")
    }
  }

  it should "bridge to the shared PromptProfile via promptProfile (name + chain only)" in {
    decode[AgenticTaskSpec](json).map(_.promptProfile) match {
      case Right(pp) =>
        pp.name shouldBe "cluster-concept-identification"
        pp.chain.flatMap(_.promptFragmentNames) shouldBe
          List("system-cluster-concept-identification", "tool-use-follow-up")
      case Left(err) => fail(s"expected a bridged profile, got $err")
    }
  }

  it should "carry a tokenBudget when present" in {
    val withBudget =
      """{"name":"x","chain":[],"tools":[],"loopPolicy":{"maxIterations":1,"perCallTimeoutSeconds":30,"tokenBudget":4096}}"""
    decode[AgenticTaskSpec](withBudget).map(_.loopPolicy.toLoopPolicy.tokenBudget) shouldBe Right(Some(4096))
  }

}
