package com.repcheck.prompt.engine

/** A profile document loaded from GCS but did not decode to a [[PromptProfile]] (malformed or stale schema). */
final case class PromptProfileParseFailed(profile: String, detail: String)
    extends Exception(s"prompt profile '$profile' could not be parsed: $detail")
