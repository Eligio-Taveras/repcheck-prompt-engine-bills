package com.repcheck.prompt.engine

import java.nio.charset.StandardCharsets

import cats.effect.Sync
import cats.syntax.all._

import com.google.cloud.storage.{BlobId, Storage}

/**
 * [[BlockLoader]] backed by the GCS Java SDK (wrapped in `Sync[F].blocking`). Resolves a logical block id to its
 * versioned object via [[GcsObjectName]]; a missing object is a typed [[PromptBlockNotFound]], not an SDK exception.
 * Loads happen ONCE at startup (the registry materializes every profile, the assembler reads its blocks), so this path
 * is intentionally retry-free — a transient GCS failure fails startup loudly and the orchestrator restarts.
 */
final class GcsPromptBlockLoader[F[_]: Sync](storage: Storage, bucket: String, prefix: String, version: String)
    extends BlockLoader[F] {

  def load(blockId: String): F[String] =
    readObject(GcsObjectName.block(prefix, blockId, version), blockId)

  def loadProfile(profileName: String): F[String] =
    readObject(GcsObjectName.profile(prefix, profileName, version), profileName)

  private def readObject(objectName: String, id: String): F[String] =
    Sync[F].blocking(Option(storage.get(BlobId.of(bucket, objectName)))).flatMap {
      case Some(blob) => Sync[F].delay(new String(blob.getContent(), StandardCharsets.UTF_8))
      case None       => Sync[F].raiseError(PromptBlockNotFound(id, bucket, objectName))
    }

}
