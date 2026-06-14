package com.repcheck.prompt.engine

/** An object loaded from GCS but did not decode to a shared `PromptFragment` (malformed or stale schema). */
final case class PromptFragmentParseFailed(fragmentName: String, detail: String)
    extends Exception(s"prompt fragment '$fragmentName' could not be parsed: $detail")
