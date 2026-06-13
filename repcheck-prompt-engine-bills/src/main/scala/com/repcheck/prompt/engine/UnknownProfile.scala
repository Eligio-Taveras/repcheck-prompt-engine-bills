package com.repcheck.prompt.engine

/**
 * A lookup named a profile the engine did not load. Raised (it is a `Throwable`) by `ChainAssembler.assemble`, and —
 * being a plain value too — returned as a typed `Left` by the pure profile lookups added alongside the tool registry.
 * Both surfaces are total; the failure is never silent.
 */
final case class UnknownProfile(profile: String, known: Set[String])
    extends Exception(s"unknown prompt profile '$profile'; loaded profiles: ${known.toList.sorted.mkString(", ")}")
