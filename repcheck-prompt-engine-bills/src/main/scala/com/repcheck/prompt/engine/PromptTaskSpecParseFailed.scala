package com.repcheck.prompt.engine

/** A task-spec document loaded from GCS but did not decode to an [[AgenticTaskSpec]] (malformed or stale schema). */
final case class PromptTaskSpecParseFailed(taskSpec: String, detail: String)
    extends Exception(s"prompt task spec '$taskSpec' could not be parsed: $detail")
