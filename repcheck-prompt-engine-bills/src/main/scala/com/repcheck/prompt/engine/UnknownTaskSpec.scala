package com.repcheck.prompt.engine

/**
 * A lookup named a task spec the engine did not load. Raised (it is a `Throwable`) by `PromptAssembler.assemble`, and —
 * being a plain value too — returned as a typed `Left` by the pure lookups added alongside the tool registry. Both
 * surfaces are total; the failure is never silent.
 */
final case class UnknownTaskSpec(taskSpec: String, known: Set[String])
    extends Exception(s"unknown prompt task spec '$taskSpec'; loaded task specs: ${known.toList.sorted.mkString(", ")}")
