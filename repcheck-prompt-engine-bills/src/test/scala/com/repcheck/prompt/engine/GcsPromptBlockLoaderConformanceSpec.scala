package com.repcheck.prompt.engine

import java.nio.charset.StandardCharsets

import cats.effect.IO
import cats.effect.testing.scalatest.AsyncIOSpec

import com.google.cloud.storage.{BlobId, BlobInfo, Storage, StorageOptions}

import org.scalatest.flatspec.AsyncFlatSpec
import org.scalatest.matchers.should.Matchers

import com.repcheck.utils.tags.DockerRequired

/**
 * Verifies `GcsPromptBlockLoader` against a REAL GCS-compatible server (excluded from `sbt test`; run with a
 * fake-gcs-server reachable at `STORAGE_EMULATOR_HOST`, default `http://localhost:4443` — see README). The Java SDK
 * honors `STORAGE_EMULATOR_HOST`, so the same code path runs against the emulator and real GCS.
 */
class GcsPromptBlockLoaderConformanceSpec extends AsyncFlatSpec with AsyncIOSpec with Matchers {

  private val host    = sys.env.getOrElse("STORAGE_EMULATOR_HOST", "http://localhost:4443")
  private val bucket  = "repcheck-prompts-test"
  private val prefix  = "bills"
  private val version = "v1.0.0"

  private def storage: Storage =
    StorageOptions.newBuilder().setHost(host).setProjectId("test").build().getService

  private def put(objectName: String, content: String): IO[Unit] =
    IO.blocking {
      val info = BlobInfo.newBuilder(BlobId.of(bucket, objectName)).build()
      val _    = storage.create(info, content.getBytes(StandardCharsets.UTF_8))
      ()
    }

  private def ensureBucket(): IO[Unit] =
    IO.blocking {
      try {
        val _ = storage.create(com.google.cloud.storage.BucketInfo.of(bucket))
        ()
      } catch { case _: com.google.cloud.storage.StorageException => () }
    }

  private def loader: GcsPromptBlockLoader[IO] = new GcsPromptBlockLoader[IO](storage, bucket, prefix, version)

  "the loader against a real GCS server" should "read a block by its versioned object name" taggedAs DockerRequired in {
    val setup = ensureBucket() *> put("bills/system-cluster-concept-v1.0.0.md", "block body")
    (setup *> loader.load("system-cluster-concept")).asserting(_ shouldBe "block body")
  }

  it should "read a profile document by its versioned object name" taggedAs DockerRequired in {
    val body =
      """{"systemBlocks":[],"tools":[],"loopPolicy":{"maxIterations":1,"perCallTimeoutSeconds":30,"tokenBudget":null}}"""
    val setup = ensureBucket() *> put("bills/profiles/taxonomy-build-v1.0.0.json", body)
    (setup *> loader.loadProfile("taxonomy-build")).asserting(_ should include("systemBlocks"))
  }

  it should "raise PromptBlockNotFound for a missing object" taggedAs DockerRequired in {
    (ensureBucket() *> loader.load("does-not-exist")).attempt.asserting {
      case Left(e: PromptBlockNotFound) => e.blockId shouldBe "does-not-exist"
      case other                        => fail(s"expected PromptBlockNotFound, got $other")
    }
  }

}
