package com.repcheck.prompt.engine

import java.nio.charset.StandardCharsets

/**
 * GCS object-name mapping for prompt fragments, plus the shared mechanics ([[join]], [[guard]]) that the per-type
 * naming methods reuse ([[AgenticTaskSpec.objectName]], [[ToolBinding.descriptionObjectName]]). A fragment maps to
 * `<prefix>/<name>-<version>.json`. The constructed key is guarded against GCS's object-name length limit — a too-long
 * name fails loudly with [[PromptObjectNameTooLong]] at startup rather than surfacing as an opaque GCS error. Pure, so
 * tested without GCS.
 */
private[engine] object GcsObjectName {

  /** GCS caps object names at 1024 bytes (UTF-8); we reject before reaching it. */
  val maxObjectNameBytes: Int = 1024

  def fragment(prefix: String, fragmentName: String, version: String): Either[PromptObjectNameTooLong, String] =
    guard(s"${join(prefix, fragmentName)}-$version.json")

  /** Guard a constructed object name against the GCS length limit. Shared by the per-type naming methods. */
  private[engine] def guard(objectName: String): Either[PromptObjectNameTooLong, String] = {
    val bytes = objectName.getBytes(StandardCharsets.UTF_8).length
    if (bytes > maxObjectNameBytes) {
      Left(PromptObjectNameTooLong(objectName, bytes, maxObjectNameBytes))
    } else {
      Right(objectName)
    }
  }

  /** Join path parts, trimming stray slashes and dropping empties. Shared by the per-type naming methods. */
  private[engine] def join(parts: String*): String =
    parts.iterator.map(trimSlashes).filter(_.nonEmpty).mkString("/")

  private def trimSlashes(part: String): String = {
    val start = part.indexWhere(_ != '/')
    if (start < 0) {
      ""
    } else {
      val end = part.lastIndexWhere(_ != '/')
      part.substring(start, end + 1)
    }
  }

}
