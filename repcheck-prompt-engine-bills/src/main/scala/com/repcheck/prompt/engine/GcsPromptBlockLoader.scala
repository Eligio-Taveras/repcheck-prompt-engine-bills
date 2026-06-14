package com.repcheck.prompt.engine

import java.nio.charset.StandardCharsets

import cats.effect.Sync
import cats.syntax.all._

import io.circe.parser.decode

import com.google.cloud.storage.{BlobId, Storage}

import repcheck.shared.models.prompt.InstructionBlock

/**
 * [[BlockLoader]] backed by the GCS Java SDK (wrapped in `Sync[F].blocking`). Resolves a logical name to its versioned
 * JSON object via [[GcsObjectName]] and decodes it; a missing object is a typed [[PromptBlockNotFound]] and a malformed
 * object a typed [[PromptBlockParseFailed]]/[[PromptProfileParseFailed]] — never a raw SDK or circe exception. Loads
 * happen ONCE at startup, so this path is intentionally retry-free — a transient GCS failure fails startup loudly and
 * the orchestrator restarts.
 */
final class GcsPromptBlockLoader[F[_]: Sync](storage: Storage, bucket: String, prefix: String, version: String)
    extends BlockLoader[F] {

  def load(blockName: String): F[InstructionBlock] =
    readObject(GcsObjectName.block(prefix, blockName, version), blockName).flatMap { raw =>
      decode[InstructionBlock](raw).leftMap(e => PromptBlockParseFailed(blockName, e.getMessage)).liftTo[F]
    }

  def loadProfile(profileName: String): F[AgenticProfile] =
    readObject(GcsObjectName.profile(prefix, profileName, version), profileName).flatMap { raw =>
      decode[AgenticProfile](raw).leftMap(e => PromptProfileParseFailed(profileName, e.getMessage)).liftTo[F]
    }

  private def readObject(objectName: String, id: String): F[String] =
    Sync[F].blocking(Option(storage.get(BlobId.of(bucket, objectName)))).flatMap {
      case Some(blob) => Sync[F].delay(new String(blob.getContent(), StandardCharsets.UTF_8))
      case None       => Sync[F].raiseError(PromptBlockNotFound(id, bucket, objectName))
    }

}
