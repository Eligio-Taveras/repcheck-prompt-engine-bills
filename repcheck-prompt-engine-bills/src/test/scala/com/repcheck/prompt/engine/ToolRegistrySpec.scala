package com.repcheck.prompt.engine

import cats.effect.IO
import cats.effect.testing.scalatest.AsyncIOSpec

import org.scalatest.flatspec.AsyncFlatSpec
import org.scalatest.matchers.should.Matchers
import repcheck.shared.models.llm.tool.LlmTool

class ToolRegistrySpec extends AsyncFlatSpec with AsyncIOSpec with Matchers {

  private val profileJson =
    """{
      |  "systemBlocks": ["system-cluster-concept", "tool-use-follow-up"],
      |  "tools": [{"name":"search_taxonomy","descriptionBlock":"tools/search-taxonomy"}],
      |  "loopPolicy": {"maxIterations": 3, "perCallTimeoutSeconds": 120, "tokenBudget": null}
      |}""".stripMargin

  private val blocks = Map("tools/search-taxonomy" -> "Searches the active taxonomy by vector similarity.")

  private def loader(profiles: Map[String, String] = Map("cluster-concept" -> profileJson)): InMemoryBlockLoader =
    new InMemoryBlockLoader(blocks, profiles)

  private val available: Map[String, LlmTool[IO]] = Map("search_taxonomy" -> new EchoTool("search_taxonomy"))

  "load" should "bind declared tools with their GCS description overriding the code default" in {
    DefaultToolRegistry.load[IO](loader(), available, List("cluster-concept")).asserting { registry =>
      registry.toolsFor("cluster-concept") match {
        case Right(tools) =>
          tools.map(_.spec.name) shouldBe List("search_taxonomy")
          tools.map(_.spec.description) shouldBe List("Searches the active taxonomy by vector similarity.")
        case Left(err) => fail(s"expected tools, got $err")
      }
    }
  }

  it should "expose the profile's loop policy" in {
    DefaultToolRegistry.load[IO](loader(), available, List("cluster-concept")).asserting { registry =>
      registry.policyFor("cluster-concept").map(_.maxIterations) shouldBe Right(3)
    }
  }

  it should "keep the code-side schemas/examples while overriding only the description" in {
    DefaultToolRegistry.load[IO](loader(), available, List("cluster-concept")).asserting { registry =>
      val tool = registry.toolsFor("cluster-concept").toOption.flatMap(_.headOption).getOrElse(fail("no tool"))
      tool.spec.parametersSchema shouldBe available("search_taxonomy").spec.parametersSchema
    }
  }

  it should "fail loudly at load on a tool name with no code impl" in {
    DefaultToolRegistry.load[IO](loader(), Map.empty, List("cluster-concept")).attempt.asserting {
      case Left(e: UnknownToolBinding) =>
        e.toolName shouldBe "search_taxonomy"
        e.profile shouldBe "cluster-concept"
      case other => fail(s"expected UnknownToolBinding, got $other")
    }
  }

  it should "fail loudly at load on a malformed profile document" in {
    DefaultToolRegistry
      .load[IO](loader(Map("cluster-concept" -> "{ not json")), available, List("cluster-concept"))
      .attempt
      .asserting {
        case Left(e: PromptProfileParseFailed) => e.profile shouldBe "cluster-concept"
        case other                             => fail(s"expected PromptProfileParseFailed, got $other")
      }
  }

  it should "fail loudly at load on a missing profile object" in {
    DefaultToolRegistry.load[IO](loader(Map.empty), available, List("cluster-concept")).attempt.asserting {
      case Left(e: PromptBlockNotFound) => e.blockId shouldBe "cluster-concept"
      case other                        => fail(s"expected PromptBlockNotFound, got $other")
    }
  }

  "lookups" should "return Left(UnknownProfile) for a profile that was not loaded — total, no throw" in {
    DefaultToolRegistry.load[IO](loader(), available, List("cluster-concept")).asserting { registry =>
      registry.toolsFor("nope") match {
        case Left(UnknownProfile(profile, known)) =>
          profile shouldBe "nope"
          known shouldBe Set("cluster-concept")
        case other => fail(s"expected Left(UnknownProfile), got $other")
      }
      registry.policyFor("nope").isLeft shouldBe true
    }
  }

}
