package com.repcheck.prompt.engine

import cats.effect.IO
import cats.effect.testing.scalatest.AsyncIOSpec

import org.scalatest.flatspec.AsyncFlatSpec
import org.scalatest.matchers.should.Matchers
import repcheck.shared.models.llm.tool.LlmTool

class ChainAssemblerSpec extends AsyncFlatSpec with AsyncIOSpec with Matchers {

  private val profileJson =
    """{
      |  "systemBlocks": ["system-cluster-concept", "tool-use-follow-up"],
      |  "tools": [{"name":"search_taxonomy","descriptionBlock":"tools/search-taxonomy"}],
      |  "loopPolicy": {"maxIterations": 3, "perCallTimeoutSeconds": 120, "tokenBudget": null}
      |}""".stripMargin

  private val blocks = Map(
    "system-cluster-concept" -> "You classify a cluster of bill sections into one concept.",
    "tool-use-follow-up"     -> "Gather via tools, iterate only if needed, submit when confident.",
    "tools/search-taxonomy"  -> "Searches the active taxonomy by vector similarity.",
  )

  private val loader = new InMemoryBlockLoader(blocks, Map("cluster-concept" -> profileJson))
  private val available: Map[String, LlmTool[IO]] = Map("search_taxonomy" -> new EchoTool("search_taxonomy"))

  private def assembler: IO[ChainAssembler[IO]] =
    DefaultToolRegistry
      .load[IO](loader, available, List("cluster-concept"))
      .flatMap(registry => DefaultChainAssembler.load[IO](loader, registry, List("cluster-concept")))

  "assemble" should "compose system blocks in order then the GCS-described tools, messages empty" in {
    assembler.flatMap(_.assemble("cluster-concept")).asserting { prompt =>
      prompt.messages shouldBe Nil
      prompt.system shouldBe
        "You classify a cluster of bill sections into one concept.\n\n" +
        "Gather via tools, iterate only if needed, submit when confident.\n\n" +
        "Tool: search_taxonomy\nSearches the active taxonomy by vector similarity."
    }
  }

  it should "raise UnknownProfile for a profile that was not loaded" in {
    assembler.flatMap(_.assemble("nope")).attempt.asserting {
      case Left(e: UnknownProfile) => e.profile shouldBe "nope"
      case other                   => fail(s"expected UnknownProfile, got $other")
    }
  }

  it should "raise PromptBlockNotFound when a declared system block is missing at load" in {
    val brokenLoader = new InMemoryBlockLoader(blocks - "tool-use-follow-up", Map("cluster-concept" -> profileJson))
    DefaultToolRegistry
      .load[IO](brokenLoader, available, List("cluster-concept"))
      .flatMap(registry => DefaultChainAssembler.load[IO](brokenLoader, registry, List("cluster-concept")))
      .attempt
      .asserting {
        case Left(e: PromptBlockNotFound) => e.blockId shouldBe "tool-use-follow-up"
        case other                        => fail(s"expected PromptBlockNotFound, got $other")
      }
  }

}
