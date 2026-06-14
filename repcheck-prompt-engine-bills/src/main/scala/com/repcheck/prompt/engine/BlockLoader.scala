package com.repcheck.prompt.engine

import repcheck.shared.models.prompt.InstructionBlock

/**
 * Loads structured prompt fragments by LOGICAL name: an `InstructionBlock` (shared §1.7 — name, stage, weight, content)
 * by block name, and an [[AgenticProfile]] document by profile name. The implementation resolves + decodes the
 * versioned object; callers never see filenames or raw JSON. The trait is the test seam — unit specs drive the
 * assembler through an in-memory map; [[GcsPromptBlockLoader]] is the prod impl.
 */
trait BlockLoader[F[_]] {

  /** Raise [[PromptBlockNotFound]] (missing object) or [[PromptBlockParseFailed]] (malformed block). */
  def load(blockName: String): F[InstructionBlock]

  /** Raise [[PromptBlockNotFound]] (missing object) or [[PromptProfileParseFailed]] (malformed profile). */
  def loadProfile(profileName: String): F[AgenticProfile]

}
