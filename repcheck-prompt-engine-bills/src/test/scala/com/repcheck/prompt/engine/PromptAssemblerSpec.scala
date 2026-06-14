package com.repcheck.prompt.engine

import cats.effect.IO
import cats.effect.testing.scalatest.AsyncIOSpec

import org.scalatest.flatspec.AsyncFlatSpec
import org.scalatest.matchers.should.Matchers
import repcheck.shared.models.prompt.{PromptFragment, PromptStage, StageConfig}

class PromptAssemblerSpec extends AsyncFlatSpec with AsyncIOSpec with Matchers {

  private def fragment(name: String, stage: PromptStage, weight: Double, content: String): PromptFragment =
    PromptFragment(name, stage, weight, "v1.0.0", content)

  private val sys  = fragment("system-cluster-concept-identification", PromptStage.System, 1.0, "Identify the concept.")
  private val loop = fragment("tool-use-follow-up", PromptStage.Custom, 1.0, "Gather, then submit.")

  private val clusterTaskSpec = AgenticTaskSpec(
    name = "cluster-concept-identification",
    chain = List(
      StageConfig(PromptStage.System, List("system-cluster-concept-identification"), 1.0),
      StageConfig(PromptStage.Custom, List("tool-use-follow-up"), 1.0),
    ),
    tools = List(ToolBinding("search_taxonomy", "tools/search-taxonomy")),
    loopPolicy = AgenticTaskSpec.LoopPolicyDoc(3, 120, None),
  )

  private def assembler(fragments: PromptFragment*): IO[PromptAssembler[IO]] = {
    val loader = new InMemoryPromptLoader(
      fragments.map(f => f.name -> f).toMap,
      Map("cluster-concept-identification" -> clusterTaskSpec),
    )
    DefaultPromptAssembler.load[IO](loader, List("cluster-concept-identification"))
  }

  "assemble" should "compose the chain in stage order, weight-translated, with messages empty" in {
    assembler(sys, loop).flatMap(_.assemble("cluster-concept-identification", Map.empty)).asserting { prompt =>
      prompt.messages shouldBe Nil
      prompt.system shouldBe "You MUST: Identify the concept.\n\nYou MUST: Gather, then submit."
    }
  }

  it should "inject {{placeholders}} into Context-stage fragments" in {
    val ctxFragment = fragment("cluster-sections", PromptStage.Context, 0.8, "Sections:\n{{cluster_sections}}")
    val taskSpec = clusterTaskSpec.copy(chain =
      clusterTaskSpec.chain :+ StageConfig(PromptStage.Context, List("cluster-sections"), 0.8)
    )
    val loader = new InMemoryPromptLoader(
      List(sys, loop, ctxFragment).map(f => f.name -> f).toMap,
      Map("cluster-concept-identification" -> taskSpec),
    )
    DefaultPromptAssembler
      .load[IO](loader, List("cluster-concept-identification"))
      .flatMap(_.assemble("cluster-concept-identification", Map("cluster_sections" -> "§101, §102")))
      .asserting { prompt =>
        prompt.system should include("§101, §102")
        prompt.system should not include "{{cluster_sections}}"
      }
  }

  it should "raise UnknownTaskSpec for a task spec that was not loaded" in {
    assembler(sys, loop).flatMap(_.assemble("nope", Map.empty)).attempt.asserting {
      case Left(e: UnknownTaskSpec) => e.taskSpec shouldBe "nope"
      case other                    => fail(s"expected UnknownTaskSpec, got $other")
    }
  }

  it should "raise PromptObjectNotFound when a chain fragment is missing at load" in {
    assembler(sys).attempt.asserting { // 'loop' fragment omitted
      case Left(e: PromptObjectNotFound) => e.id shouldBe "tool-use-follow-up"
      case other                         => fail(s"expected PromptObjectNotFound, got $other")
    }
  }

  "assembleOne" should "raise PromptAssemblyFailed when a chain fragment is absent from the map" in {
    DefaultPromptAssembler.assembleOne(clusterTaskSpec, Map.empty, Map.empty) match {
      case Left(e: PromptAssemblyFailed) => e.taskSpec shouldBe "cluster-concept-identification"
      case other                         => fail(s"expected PromptAssemblyFailed, got $other")
    }
  }

}
