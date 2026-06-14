package com.repcheck.prompt.engine

import java.nio.charset.StandardCharsets

import cats.effect.Sync
import cats.syntax.all._

import io.circe.parser.decode

import com.google.cloud.storage.{BlobId, Storage}

import repcheck.shared.models.prompt.PromptFragment

/**
 * [[PromptLoader]] backed by the GCS Java SDK (wrapped in `Sync[F].blocking`). Resolves a logical name to its versioned
 * JSON object via [[GcsObjectName]] and decodes it; a missing object is a typed [[PromptObjectNotFound]] and a
 * malformed object a typed [[PromptFragmentParseFailed]]/[[PromptTaskSpecParseFailed]] — never a raw SDK or circe
 * exception. Loads happen ONCE at startup, so this path is intentionally retry-free — a transient GCS failure fails
 * startup loudly and the orchestrator restarts.
 */
final class GcsPromptLoader[F[_]: Sync](storage: Storage, bucket: String, prefix: String, version: String)
    extends PromptLoader[F] {

  def load(fragmentName: String): F[PromptFragment] =
    readObject(GcsObjectName.fragment(prefix, fragmentName, version), fragmentName).flatMap { raw =>
      decode[PromptFragment](raw).leftMap(e => PromptFragmentParseFailed(fragmentName, e.getMessage)).liftTo[F]
    }

  def loadTaskSpec(taskSpecName: String): F[AgenticTaskSpec] =
    readObject(GcsObjectName.taskSpec(prefix, taskSpecName, version), taskSpecName).flatMap { raw =>
      decode[AgenticTaskSpec](raw).leftMap(e => PromptTaskSpecParseFailed(taskSpecName, e.getMessage)).liftTo[F]
    }

  private def readObject(objectName: String, id: String): F[String] =
    Sync[F].blocking(Option(storage.get(BlobId.of(bucket, objectName)))).flatMap {
      case Some(blob) => Sync[F].delay(new String(blob.getContent(), StandardCharsets.UTF_8))
      case None       => Sync[F].raiseError(PromptObjectNotFound(id, bucket, objectName))
    }

}
