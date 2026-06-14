package com.repcheck.prompt.engine

import cats.effect.IO
import cats.effect.testing.scalatest.AsyncIOSpec

import org.scalatest.flatspec.AsyncFlatSpec
import org.scalatest.matchers.should.Matchers
import repcheck.shared.models.prompt.{InstructionBlock, PromptStage, StageConfig}

class PromptAssemblerSpec extends AsyncFlatSpec with AsyncIOSpec with Matchers {

  private def block(name: String, stage: PromptStage, weight: Double, content: String): InstructionBlock =
    InstructionBlock(name, stage, weight, "v1.0.0", content)

  private val sys  = block("system-cluster-concept-identification", PromptStage.System, 1.0, "Identify the concept.")
  private val loop = block("tool-use-follow-up", PromptStage.Custom, 1.0, "Gather, then submit.")

  private val clusterProfile = AgenticProfile(
    name = "cluster-concept-identification",
    chain = List(
      StageConfig(PromptStage.System, List("system-cluster-concept-identification"), 1.0),
      StageConfig(PromptStage.Custom, List("tool-use-follow-up"), 1.0),
    ),
    tools = List(ToolBinding("search_taxonomy", "tools/search-taxonomy")),
    loopPolicy = AgenticProfile.LoopPolicyDoc(3, 120, None),
  )

  private def assembler(blocks: InstructionBlock*): IO[PromptAssembler[IO]] = {
    val loader = new InMemoryBlockLoader(
      blocks.map(b => b.name -> b).toMap,
      Map("cluster-concept-identification" -> clusterProfile),
    )
    DefaultPromptAssembler.load[IO](loader, List("cluster-concept-identification"))
  }

  "assemble" should "compose the chain in stage order, weight-translated, with messages empty" in {
    assembler(sys, loop).flatMap(_.assemble("cluster-concept-identification", Map.empty)).asserting { prompt =>
      prompt.messages shouldBe Nil
      prompt.system shouldBe "You MUST: Identify the concept.\n\nYou MUST: Gather, then submit."
    }
  }

  it should "inject {{placeholders}} into Context-stage blocks" in {
    val ctxBlock = block("cluster-sections", PromptStage.Context, 0.8, "Sections:\n{{cluster_sections}}")
    val profile = clusterProfile.copy(chain =
      clusterProfile.chain :+ StageConfig(PromptStage.Context, List("cluster-sections"), 0.8)
    )
    val loader = new InMemoryBlockLoader(
      List(sys, loop, ctxBlock).map(b => b.name -> b).toMap,
      Map("cluster-concept-identification" -> profile),
    )
    DefaultPromptAssembler
      .load[IO](loader, List("cluster-concept-identification"))
      .flatMap(_.assemble("cluster-concept-identification", Map("cluster_sections" -> "§101, §102")))
      .asserting { prompt =>
        prompt.system should include("§101, §102")
        prompt.system should not include "{{cluster_sections}}"
      }
  }

  it should "raise UnknownProfile for a profile that was not loaded" in {
    assembler(sys, loop).flatMap(_.assemble("nope", Map.empty)).attempt.asserting {
      case Left(e: UnknownProfile) => e.profile shouldBe "nope"
      case other                   => fail(s"expected UnknownProfile, got $other")
    }
  }

  it should "raise PromptBlockNotFound when a chain block is missing at load" in {
    assembler(sys).attempt.asserting { // 'loop' block omitted
      case Left(e: PromptBlockNotFound) => e.blockId shouldBe "tool-use-follow-up"
      case other                        => fail(s"expected PromptBlockNotFound, got $other")
    }
  }

  "assembleOne" should "raise PromptAssemblyFailed when a chain block is absent from the blocks map" in {
    DefaultPromptAssembler.assembleOne(clusterProfile, Map.empty, Map.empty) match {
      case Left(e: PromptAssemblyFailed) => e.profile shouldBe "cluster-concept-identification"
      case other                         => fail(s"expected PromptAssemblyFailed, got $other")
    }
  }

}
