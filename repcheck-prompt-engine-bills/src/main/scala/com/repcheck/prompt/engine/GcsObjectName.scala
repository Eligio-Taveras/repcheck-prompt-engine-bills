package com.repcheck.prompt.engine

/**
 * The versioned object-name convention (semver in filename, the universal rule). A logical block id maps to
 * `<prefix>/<id>-<version>.<ext>` — profiles are `.json`, every other block is `.md`. Pure, so the mapping is tested
 * without GCS.
 */
private[engine] object GcsObjectName {

  def block(prefix: String, blockId: String, version: String): String =
    s"${join(prefix, blockId)}-$version.md"

  def profile(prefix: String, profileName: String, version: String): String =
    s"${join(prefix, "profiles", profileName)}-$version.json"

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
