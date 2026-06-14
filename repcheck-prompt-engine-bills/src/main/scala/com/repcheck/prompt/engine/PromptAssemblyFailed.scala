package com.repcheck.prompt.engine

/** The shared `ChainAssembler` could not compose a task spec's chain (e.g. a stage referenced an absent fragment). */
final case class PromptAssemblyFailed(taskSpec: String, detail: String)
    extends Exception(s"prompt assembly failed for task spec '$taskSpec': $detail")
