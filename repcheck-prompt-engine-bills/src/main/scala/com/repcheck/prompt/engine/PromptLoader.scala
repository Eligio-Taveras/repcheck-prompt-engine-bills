package com.repcheck.prompt.engine

import repcheck.shared.models.prompt.PromptFragment

/**
 * Loads prompt artifacts by LOGICAL name: a shared `PromptFragment` by fragment name, an [[AgenticTaskSpec]] by
 * task-spec name, and a tool's model-facing description as raw text by ref. The implementation resolves + decodes the
 * versioned object; callers never see filenames or raw JSON. The trait is the test seam — unit specs drive the engine
 * through an in-memory map; [[GcsPromptLoader]] is the prod impl.
 */
trait PromptLoader[F[_]] {

  /** Raise [[PromptObjectNotFound]] (missing object) or [[PromptFragmentParseFailed]] (malformed fragment). */
  def load(fragmentName: String): F[PromptFragment]

  /** Raise [[PromptObjectNotFound]] (missing object) or [[PromptTaskSpecParseFailed]] (malformed task spec). */
  def loadTaskSpec(taskSpecName: String): F[AgenticTaskSpec]

  /** A tool's model-facing description as raw text (no stage/weight). Raise [[PromptObjectNotFound]] if absent. */
  def loadToolDescription(descriptionRef: String): F[String]

}
