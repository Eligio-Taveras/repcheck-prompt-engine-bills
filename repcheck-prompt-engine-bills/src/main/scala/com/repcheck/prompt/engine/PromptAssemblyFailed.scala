package com.repcheck.prompt.engine

/**
 * The shared `ChainAssembler` could not compose a profile's chain (e.g. a stage referenced a block absent from the
 * map).
 */
final case class PromptAssemblyFailed(profile: String, detail: String)
    extends Exception(s"prompt assembly failed for profile '$profile': $detail")
