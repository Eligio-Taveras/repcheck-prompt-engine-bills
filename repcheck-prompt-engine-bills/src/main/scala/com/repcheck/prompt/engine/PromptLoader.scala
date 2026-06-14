package com.repcheck.prompt.engine

import repcheck.shared.models.prompt.PromptFragment

/**
 * Loads structured prompt artifacts by LOGICAL name: a shared `PromptFragment` by fragment name, and an
 * [[AgenticTaskSpec]] document by task-spec name. The implementation resolves + decodes the versioned object; callers
 * never see filenames or raw JSON. The trait is the test seam — unit specs drive the assembler through an in-memory
 * map; [[GcsPromptLoader]] is the prod impl.
 */
trait PromptLoader[F[_]] {

  /** Raise [[PromptObjectNotFound]] (missing object) or [[PromptFragmentParseFailed]] (malformed fragment). */
  def load(fragmentName: String): F[PromptFragment]

  /** Raise [[PromptObjectNotFound]] (missing object) or [[PromptTaskSpecParseFailed]] (malformed task spec). */
  def loadTaskSpec(taskSpecName: String): F[AgenticTaskSpec]

}
