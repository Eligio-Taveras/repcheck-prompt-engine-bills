package com.repcheck.prompt.engine

import java.nio.charset.StandardCharsets

import cats.effect.IO
import cats.effect.testing.scalatest.AsyncIOSpec

import com.google.cloud.storage.{BlobId, BlobInfo, Storage, StorageOptions}

import org.scalatest.flatspec.AsyncFlatSpec
import org.scalatest.matchers.should.Matchers
import repcheck.shared.models.prompt.PromptStage

import com.repcheck.utils.tags.DockerRequired

/**
 * Verifies `GcsPromptLoader` against a REAL GCS-compatible server (excluded from `sbt test`; run with a fake-gcs-server
 * reachable at `STORAGE_EMULATOR_HOST`, default `http://localhost:4443` — see README). The Java SDK honors
 * `STORAGE_EMULATOR_HOST`, so the same code path runs against the emulator and real GCS.
 */
class GcsPromptLoaderConformanceSpec extends AsyncFlatSpec with AsyncIOSpec with Matchers {

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

  private def loader: GcsPromptLoader[IO] = new GcsPromptLoader[IO](storage, bucket, prefix, version)

  "the loader against a real GCS server" should "read and decode a fragment by its versioned object name" taggedAs DockerRequired in {
    val body =
      """{"name":"system-cluster-concept-identification","stage":"system","weight":1.0,"version":"v1.0.0","content":"fragment body"}"""
    val setup = ensureBucket() *> put("bills/system-cluster-concept-identification-v1.0.0.json", body)
    (setup *> loader.load("system-cluster-concept-identification")).asserting { fragment =>
      fragment.stage shouldBe PromptStage.System
      fragment.content shouldBe "fragment body"
    }
  }

  it should "read and decode a task spec document by its versioned object name" taggedAs DockerRequired in {
    val body =
      """{"name":"taxonomy-build","chain":[{"stage":"system","promptFragmentNames":["system-taxonomy-build"],"weight":1.0}],"tools":[],"loopPolicy":{"maxIterations":8,"perCallTimeoutSeconds":180,"tokenBudget":null}}"""
    val setup = ensureBucket() *> put("bills/task-specs/taxonomy-build-v1.0.0.json", body)
    (setup *> loader.loadTaskSpec("taxonomy-build")).asserting { taskSpec =>
      taskSpec.name shouldBe "taxonomy-build"
      taskSpec.loopPolicy.maxIterations shouldBe 8
    }
  }

  it should "raise PromptObjectNotFound for a missing object" taggedAs DockerRequired in {
    (ensureBucket() *> loader.load("does-not-exist")).attempt.asserting {
      case Left(e: PromptObjectNotFound) => e.id shouldBe "does-not-exist"
      case other                         => fail(s"expected PromptObjectNotFound, got $other")
    }
  }

}
