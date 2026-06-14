package com.repcheck.prompt.engine

/** A block object loaded from GCS but did not decode to a shared `InstructionBlock` (malformed or stale schema). */
final case class PromptBlockParseFailed(blockId: String, detail: String)
    extends Exception(s"prompt block '$blockId' could not be parsed: $detail")
