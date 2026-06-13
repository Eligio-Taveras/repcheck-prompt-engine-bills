package com.repcheck.prompt.engine

import cats.effect.IO
import cats.effect.testing.scalatest.AsyncIOSpec

import org.scalatest.flatspec.AsyncFlatSpec
import org.scalatest.matchers.should.Matchers

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
  )

  private val loader = new InMemoryBlockLoader(blocks, Map("cluster-concept" -> profileJson))

  private def assembler: IO[ChainAssembler[IO]] =
    DefaultChainAssembler.load[IO](loader, List("cluster-concept"))

  "assemble" should "compose ONLY the profile's system blocks in order, messages empty — no tool handling" in {
    assembler.flatMap(_.assemble("cluster-concept")).asserting { prompt =>
      prompt.messages shouldBe Nil
      prompt.system shouldBe
        "You classify a cluster of bill sections into one concept.\n\n" +
        "Gather via tools, iterate only if needed, submit when confident."
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
    DefaultChainAssembler.load[IO](brokenLoader, List("cluster-concept")).attempt.asserting {
      case Left(e: PromptBlockNotFound) => e.blockId shouldBe "tool-use-follow-up"
      case other                        => fail(s"expected PromptBlockNotFound, got $other")
    }
  }

  it should "raise PromptProfileParseFailed on a malformed profile document" in {
    val brokenLoader = new InMemoryBlockLoader(blocks, Map("cluster-concept" -> "{ not json"))
    DefaultChainAssembler.load[IO](brokenLoader, List("cluster-concept")).attempt.asserting {
      case Left(e: PromptProfileParseFailed) => e.profile shouldBe "cluster-concept"
      case other                             => fail(s"expected PromptProfileParseFailed, got $other")
    }
  }

}
