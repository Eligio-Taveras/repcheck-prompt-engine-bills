package com.repcheck.prompt.engine

import java.nio.charset.StandardCharsets

/**
 * The versioned object-name convention (semver in filename, the universal rule). A logical name maps to
 * `<prefix>/<name>-<version>.json` for a prompt fragment, `<prefix>/task-specs/<name>-<version>.json` for a task spec,
 * and `<prefix>/<ref>-<version>.md` for a tool description (raw text). The constructed key is guarded against GCS's
 * object-name length limit — a too-long name fails loudly with [[PromptObjectNameTooLong]] at startup rather than
 * surfacing as an opaque GCS error. Pure, so tested without GCS.
 */
private[engine] object GcsObjectName {

  /** GCS caps object names at 1024 bytes (UTF-8); we reject before reaching it. */
  val maxObjectNameBytes: Int = 1024

  def fragment(prefix: String, fragmentName: String, version: String): Either[PromptObjectNameTooLong, String] =
    guarded(s"${join(prefix, fragmentName)}-$version.json")

  def taskSpec(prefix: String, taskSpecName: String, version: String): Either[PromptObjectNameTooLong, String] =
    guarded(s"${join(prefix, "task-specs", taskSpecName)}-$version.json")

  def toolDescription(
    prefix: String,
    descriptionRef: String,
    version: String,
  ): Either[PromptObjectNameTooLong, String] =
    guarded(s"${join(prefix, descriptionRef)}-$version.md")

  private def guarded(objectName: String): Either[PromptObjectNameTooLong, String] = {
    val bytes = objectName.getBytes(StandardCharsets.UTF_8).length
    if (bytes > maxObjectNameBytes) {
      Left(PromptObjectNameTooLong(objectName, bytes, maxObjectNameBytes))
    } else {
      Right(objectName)
    }
  }

  private def join(parts: String*): String =
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
