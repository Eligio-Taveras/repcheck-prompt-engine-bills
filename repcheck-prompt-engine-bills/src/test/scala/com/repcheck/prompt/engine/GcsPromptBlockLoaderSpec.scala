package com.repcheck.prompt.engine

import java.nio.charset.StandardCharsets

import cats.effect.IO
import cats.effect.testing.scalatest.AsyncIOSpec

import com.google.cloud.storage.contrib.nio.testing.LocalStorageHelper
import com.google.cloud.storage.{BlobId, BlobInfo, Storage}

import org.scalatest.flatspec.AsyncFlatSpec
import org.scalatest.matchers.should.Matchers
import repcheck.shared.models.prompt.PromptStage

/**
 * In-memory `LocalStorageHelper` exercises the read + decode branches without a container; the real-server cross-check
 * is the DockerRequired spec.
 */
class GcsPromptBlockLoaderSpec extends AsyncFlatSpec with AsyncIOSpec with Matchers {

  private val blockJson =
    """{"name":"system-cluster-concept-identification","stage":"system","weight":1.0,"version":"v1.0.0","content":"block body"}"""

  private val profileJson =
    """{"name":"cluster-concept-identification","chain":[{"stage":"system","blockNames":["system-cluster-concept-identification"],"weight":1.0}],"tools":[{"name":"search_taxonomy","descriptionBlock":"tools/search-taxonomy"}],"loopPolicy":{"maxIterations":3,"perCallTimeoutSeconds":120,"tokenBudget":null}}"""

  private def storageWith(objects: (String, String)*): Storage = {
    val storage = LocalStorageHelper.getOptions.getService
    objects.foreach {
      case (name, content) =>
        val _ = storage.create(
          BlobInfo.newBuilder(BlobId.of("repcheck-prompts-test", name)).build(),
          content.getBytes(StandardCharsets.UTF_8),
        )
    }
    storage
  }

  private def loader(storage: Storage): GcsPromptBlockLoader[IO] =
    new GcsPromptBlockLoader[IO](storage, "repcheck-prompts-test", "bills", "v1.0.0")

  "load" should "read and decode a structured block by its versioned object name" in {
    val storage = storageWith("bills/system-cluster-concept-identification-v1.0.0.json" -> blockJson)
    loader(storage).load("system-cluster-concept-identification").asserting { block =>
      block.stage shouldBe PromptStage.System
      block.weight shouldBe 1.0
      block.content shouldBe "block body"
    }
  }

  "loadProfile" should "read and decode an agentic profile by its versioned object name" in {
    val storage = storageWith("bills/profiles/cluster-concept-identification-v1.0.0.json" -> profileJson)
    loader(storage).loadProfile("cluster-concept-identification").asserting { profile =>
      profile.chain.flatMap(_.blockNames) shouldBe List("system-cluster-concept-identification")
      profile.tools.map(_.name) shouldBe List("search_taxonomy")
      profile.loopPolicy.maxIterations shouldBe 3
    }
  }

  it should "raise PromptBlockNotFound for a missing object" in {
    loader(storageWith()).load("absent").attempt.asserting {
      case Left(e: PromptBlockNotFound) =>
        e.blockId shouldBe "absent"
        e.objectName shouldBe "bills/absent-v1.0.0.json"
      case other => fail(s"expected PromptBlockNotFound, got $other")
    }
  }

  it should "raise PromptBlockParseFailed for a malformed block object" in {
    val storage = storageWith("bills/broken-v1.0.0.json" -> "{ not json")
    loader(storage).load("broken").attempt.asserting {
      case Left(e: PromptBlockParseFailed) => e.blockId shouldBe "broken"
      case other                           => fail(s"expected PromptBlockParseFailed, got $other")
    }
  }

  it should "raise PromptProfileParseFailed for a malformed profile object" in {
    val storage = storageWith("bills/profiles/broken-v1.0.0.json" -> "{ not json")
    loader(storage).loadProfile("broken").attempt.asserting {
      case Left(e: PromptProfileParseFailed) => e.profile shouldBe "broken"
      case other                             => fail(s"expected PromptProfileParseFailed, got $other")
    }
  }

}
