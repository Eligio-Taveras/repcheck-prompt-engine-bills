package com.repcheck.prompt.engine

/**
 * Loads prompt fragments by LOGICAL id (e.g. `system-cluster-concept`, `tools/search-taxonomy`) and profile documents
 * by name. The implementation resolves the versioned object name; callers never see filenames. The trait is the test
 * seam — unit specs drive the assembler/registry through an in-memory map; [[GcsPromptBlockLoader]] is the prod impl.
 */
trait BlockLoader[F[_]] {

  /** A markdown instruction/description block. Raise [[PromptBlockNotFound]] if the id resolves to no object. */
  def load(blockId: String): F[String]

  /** A profile's raw JSON document. Raise [[PromptBlockNotFound]] if the profile resolves to no object. */
  def loadProfile(profileName: String): F[String]

}
