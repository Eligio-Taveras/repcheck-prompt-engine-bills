package com.repcheck.prompt.engine

/**
 * A task spec declared a tool name with no matching code impl in the injected `available` map — fails LOUDLY at
 * registry load, never at classify time (a task spec that references a tool the deploy doesn't ship is a config bug).
 */
final case class UnknownToolBinding(taskSpec: String, toolName: String, available: Set[String])
    extends Exception(
      s"task spec '$taskSpec' binds tool '$toolName', not among the available impls: ${available.toList.sorted.mkString(", ")}"
    )
