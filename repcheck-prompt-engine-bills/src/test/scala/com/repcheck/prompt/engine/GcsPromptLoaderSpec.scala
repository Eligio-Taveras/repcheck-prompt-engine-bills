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
class GcsPromptLoaderSpec extends AsyncFlatSpec with AsyncIOSpec with Matchers {

  private val fragmentJson =
    """{"name":"system-cluster-concept-identification","stage":"system","weight":1.0,"version":"v1.0.0","content":"fragment body"}"""

  private val taskSpecJson =
    """{"name":"cluster-concept-identification","chain":[{"stage":"system","promptFragmentNames":["system-cluster-concept-identification"],"weight":1.0}],"tools":[{"name":"search_taxonomy","descriptionBlock":"tools/search-taxonomy"}],"loopPolicy":{"maxIterations":3,"perCallTimeoutSeconds":120,"tokenBudget":null}}"""

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

  private def loader(storage: Storage): GcsPromptLoader[IO] =
    new GcsPromptLoader[IO](storage, "repcheck-prompts-test", "bills", "v1.0.0")

  "load" should "read and decode a fragment by its versioned object name" in {
    val storage = storageWith("bills/system-cluster-concept-identification-v1.0.0.json" -> fragmentJson)
    loader(storage).load("system-cluster-concept-identification").asserting { fragment =>
      fragment.stage shouldBe PromptStage.System
      fragment.weight shouldBe 1.0
      fragment.content shouldBe "fragment body"
    }
  }

  "loadTaskSpec" should "read and decode a task spec by its versioned object name" in {
    val storage = storageWith("bills/task-specs/cluster-concept-identification-v1.0.0.json" -> taskSpecJson)
    loader(storage).loadTaskSpec("cluster-concept-identification").asserting { taskSpec =>
      taskSpec.chain.flatMap(_.promptFragmentNames) shouldBe List("system-cluster-concept-identification")
      taskSpec.tools.map(_.name) shouldBe List("search_taxonomy")
      taskSpec.loopPolicy.maxIterations shouldBe 3
    }
  }

  it should "raise PromptObjectNotFound for a missing object" in {
    loader(storageWith()).load("absent").attempt.asserting {
      case Left(e: PromptObjectNotFound) =>
        e.id shouldBe "absent"
        e.objectName shouldBe "bills/absent-v1.0.0.json"
      case other => fail(s"expected PromptObjectNotFound, got $other")
    }
  }

  it should "raise PromptFragmentParseFailed for a malformed fragment object" in {
    val storage = storageWith("bills/broken-v1.0.0.json" -> "{ not json")
    loader(storage).load("broken").attempt.asserting {
      case Left(e: PromptFragmentParseFailed) => e.fragmentName shouldBe "broken"
      case other                              => fail(s"expected PromptFragmentParseFailed, got $other")
    }
  }

  it should "raise PromptTaskSpecParseFailed for a malformed task spec object" in {
    val storage = storageWith("bills/task-specs/broken-v1.0.0.json" -> "{ not json")
    loader(storage).loadTaskSpec("broken").attempt.asserting {
      case Left(e: PromptTaskSpecParseFailed) => e.taskSpec shouldBe "broken"
      case other                              => fail(s"expected PromptTaskSpecParseFailed, got $other")
    }
  }

}
