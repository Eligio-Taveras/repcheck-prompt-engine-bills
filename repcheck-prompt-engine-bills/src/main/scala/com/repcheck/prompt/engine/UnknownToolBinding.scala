package com.repcheck.prompt.engine

/**
 * A profile declared a tool name with no matching code impl in the injected `available` map — fails LOUDLY at registry
 * load, never at classify time (a profile that references a tool the deploy doesn't ship is a config bug).
 */
final case class UnknownToolBinding(profile: String, toolName: String, available: Set[String])
    extends Exception(
      s"profile '$profile' binds tool '$toolName', which is not among the available impls: ${available.toList.sorted.mkString(", ")}"
    )
