package com.repcheck.prompt.engine

import java.nio.charset.StandardCharsets

import cats.effect.Sync
import cats.syntax.all._

import io.circe.parser.decode

import com.google.cloud.storage.{BlobId, Storage}

import repcheck.shared.models.prompt.PromptFragment

/**
 * [[PromptLoader]] backed by the GCS Java SDK (wrapped in `Sync[F].blocking`). Resolves a logical name to its versioned
 * object via [[GcsObjectName]] (rejecting an over-long key with [[PromptObjectNameTooLong]]); fragments + task specs
 * are decoded, tool descriptions returned as raw text. A missing object is a typed [[PromptObjectNotFound]] and a
 * malformed one a typed [[PromptFragmentParseFailed]]/[[PromptTaskSpecParseFailed]] — never a raw SDK or circe
 * exception. Loads happen ONCE at startup, so this path is intentionally retry-free — a transient GCS failure fails
 * startup loudly and the orchestrator restarts.
 */
final class GcsPromptLoader[F[_]: Sync](storage: Storage, bucket: String, prefix: String, version: String)
    extends PromptLoader[F] {

  def load(fragmentName: String): F[PromptFragment] =
    GcsObjectName.fragment(prefix, fragmentName, version).liftTo[F].flatMap { objectName =>
      readObject(objectName, fragmentName).flatMap { raw =>
        decode[PromptFragment](raw).leftMap(e => PromptFragmentParseFailed(fragmentName, e.getMessage)).liftTo[F]
      }
    }

  def loadTaskSpec(taskSpecName: String): F[AgenticTaskSpec] =
    GcsObjectName.taskSpec(prefix, taskSpecName, version).liftTo[F].flatMap { objectName =>
      readObject(objectName, taskSpecName).flatMap { raw =>
        decode[AgenticTaskSpec](raw).leftMap(e => PromptTaskSpecParseFailed(taskSpecName, e.getMessage)).liftTo[F]
      }
    }

  def loadToolDescription(descriptionRef: String): F[String] =
    GcsObjectName.toolDescription(prefix, descriptionRef, version).liftTo[F].flatMap(readObject(_, descriptionRef))

  private def readObject(objectName: String, id: String): F[String] =
    Sync[F].blocking(Option(storage.get(BlobId.of(bucket, objectName)))).flatMap {
      case Some(blob) => Sync[F].delay(new String(blob.getContent(), StandardCharsets.UTF_8))
      case None       => Sync[F].raiseError(PromptObjectNotFound(id, bucket, objectName))
    }

}
