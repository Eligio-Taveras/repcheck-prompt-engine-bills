package com.repcheck.prompt.engine

import cats.effect.IO
import cats.effect.testing.scalatest.AsyncIOSpec

import org.scalatest.flatspec.AsyncFlatSpec
import org.scalatest.matchers.should.Matchers
import repcheck.shared.models.llm.tool.LlmTool
import repcheck.shared.models.prompt.{PromptStage, StageConfig}

class ToolRegistrySpec extends AsyncFlatSpec with AsyncIOSpec with Matchers {

  private val clusterTaskSpec = AgenticTaskSpec(
    name = "cluster-concept-identification",
    chain = List(StageConfig(PromptStage.System, List("system-cluster-concept-identification"), 1.0)),
    tools = List(ToolBinding("search_taxonomy", "tools/search-taxonomy")),
    loopPolicy = AgenticTaskSpec.LoopPolicyDoc(3, 120, None),
  )

  private val toolDescriptions = Map("tools/search-taxonomy" -> "Searches the active taxonomy by vector similarity.")

  private def loader(
    taskSpecs: Map[String, AgenticTaskSpec] = Map("cluster-concept-identification" -> clusterTaskSpec)
  ): InMemoryPromptLoader =
    new InMemoryPromptLoader(Map.empty, taskSpecs, toolDescriptions)

  private val available: Map[String, LlmTool[IO]] = Map("search_taxonomy" -> new EchoTool("search_taxonomy"))

  "load" should "bind declared tools with their GCS description overriding the code default" in {
    DefaultToolRegistry.load[IO](loader(), available, List("cluster-concept-identification")).asserting { registry =>
      registry.toolsFor("cluster-concept-identification") match {
        case Right(tools) =>
          val _ = tools.map(_.spec.name) shouldBe List("search_taxonomy")
          tools.map(_.spec.description) shouldBe List("Searches the active taxonomy by vector similarity.")
        case Left(err) => fail(s"expected tools, got $err")
      }
    }
  }

  it should "expose the task spec's loop policy" in {
    DefaultToolRegistry.load[IO](loader(), available, List("cluster-concept-identification")).asserting { registry =>
      registry.policyFor("cluster-concept-identification").map(_.maxIterations) shouldBe Right(3)
    }
  }

  it should "keep the code-side schemas/examples while overriding only the description" in {
    DefaultToolRegistry.load[IO](loader(), available, List("cluster-concept-identification")).asserting { registry =>
      val tool =
        registry.toolsFor("cluster-concept-identification").toOption.flatMap(_.headOption).getOrElse(fail("no tool"))
      tool.spec.parametersSchema shouldBe available("search_taxonomy").spec.parametersSchema
    }
  }

  it should "fail loudly at load on a tool name with no code impl" in {
    DefaultToolRegistry.load[IO](loader(), Map.empty, List("cluster-concept-identification")).attempt.asserting {
      case Left(e: UnknownToolBinding) =>
        val _ = e.toolName shouldBe "search_taxonomy"
        e.taskSpec shouldBe "cluster-concept-identification"
      case other => fail(s"expected UnknownToolBinding, got $other")
    }
  }

  it should "fail loudly at load on a missing task-spec object" in {
    DefaultToolRegistry
      .load[IO](loader(Map.empty), available, List("cluster-concept-identification"))
      .attempt
      .asserting {
        case Left(e: PromptObjectNotFound) => e.id shouldBe "cluster-concept-identification"
        case other                         => fail(s"expected PromptObjectNotFound, got $other")
      }
  }

  "lookups" should "return Left(UnknownTaskSpec) for a task spec that was not loaded — total, no throw" in {
    DefaultToolRegistry.load[IO](loader(), available, List("cluster-concept-identification")).asserting { registry =>
      val _ = registry.toolsFor("nope") match {
        case Left(UnknownTaskSpec(taskSpec, known)) =>
          val _ = taskSpec shouldBe "nope"
          known shouldBe Set("cluster-concept-identification")
        case other => fail(s"expected Left(UnknownTaskSpec), got $other")
      }
      registry.policyFor("nope").isLeft shouldBe true
    }
  }

}
