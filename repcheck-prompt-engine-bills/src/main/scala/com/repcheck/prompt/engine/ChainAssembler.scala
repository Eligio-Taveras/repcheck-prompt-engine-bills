package com.repcheck.prompt.engine

import repcheck.shared.models.llm.prompt.AssembledPrompt

/**
 * Assembles the system prompt for a profile from its GCS blocks + tool descriptions, producing the F4→F2 contract
 * [[AssembledPrompt]]. `messages` starts empty; the task caller (D4's classifier/builder) appends its cluster/concept
 * content before handing the prompt to the runner.
 */
trait ChainAssembler[F[_]] {

  def assemble(profile: String): F[AssembledPrompt]

}
