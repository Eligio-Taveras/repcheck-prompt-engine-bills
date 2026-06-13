package com.repcheck.prompt.engine

import java.nio.charset.StandardCharsets

import cats.effect.IO
import cats.effect.testing.scalatest.AsyncIOSpec

import com.google.cloud.storage.contrib.nio.testing.LocalStorageHelper
import com.google.cloud.storage.{BlobId, BlobInfo, Storage}

import org.scalatest.flatspec.AsyncFlatSpec
import org.scalatest.matchers.should.Matchers

/**
 * In-memory `LocalStorageHelper` exercises both wire branches without a container; the real-server cross-check is the
 * DockerRequired spec.
 */
class GcsPromptBlockLoaderSpec extends AsyncFlatSpec with AsyncIOSpec with Matchers {

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

  "load" should "read a present block by its versioned object name" in {
    val storage = storageWith("bills/system-cluster-concept-v1.0.0.md" -> "block body")
    loader(storage).load("system-cluster-concept").asserting(_ shouldBe "block body")
  }

  "loadProfile" should "read a present profile by its versioned object name" in {
    val storage = storageWith("bills/profiles/taxonomy-build-v1.0.0.json" -> """{"ok":true}""")
    loader(storage).loadProfile("taxonomy-build").asserting(_ shouldBe """{"ok":true}""")
  }

  it should "raise PromptBlockNotFound for a missing object" in {
    loader(storageWith()).load("absent").attempt.asserting {
      case Left(e: PromptBlockNotFound) =>
        e.blockId shouldBe "absent"
        e.objectName shouldBe "bills/absent-v1.0.0.md"
      case other => fail(s"expected PromptBlockNotFound, got $other")
    }
  }

}
