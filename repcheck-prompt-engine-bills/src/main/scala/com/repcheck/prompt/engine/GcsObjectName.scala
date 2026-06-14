package com.repcheck.prompt.engine

/**
 * The versioned object-name convention (semver in filename, the universal rule). A logical name maps to
 * `<prefix>/<name>-<version>.json` for a prompt fragment, and `<prefix>/task-specs/<name>-<version>.json` for a task
 * spec. Pure, so the mapping is tested without GCS.
 */
private[engine] object GcsObjectName {

  def fragment(prefix: String, fragmentName: String, version: String): String =
    s"${join(prefix, fragmentName)}-$version.json"

  def taskSpec(prefix: String, taskSpecName: String, version: String): String =
    s"${join(prefix, "task-specs", taskSpecName)}-$version.json"

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
