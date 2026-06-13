package com.repcheck.prompt.engine

/**
 * A lookup named a profile the engine did not load. Returned as a `Left` from `ToolRegistry`'s pure lookups, and raised
 * (it is a `Throwable`) by `ChainAssembler.assemble` — both surfaces are total, the failure is never silent.
 */
final case class UnknownProfile(profile: String, known: Set[String])
    extends Exception(s"unknown prompt profile '$profile'; loaded profiles: ${known.toList.sorted.mkString(", ")}")
