package com.repcheck.prompt.engine

import repcheck.shared.models.llm.prompt.AssembledPrompt

/**
 * Assembles the system prompt for a profile from its ordered GCS system instruction blocks, producing the F4→F2
 * contract [[AssembledPrompt]]. It does NOT handle tools — a tool's model-facing description reaches the LLM through
 * the tools channel (registry → runner → provider), per `runtime/03`. `messages` starts empty; the task caller (D4's
 * classifier/builder) appends its cluster/concept content before handing the prompt to the runner.
 */
trait ChainAssembler[F[_]] {

  def assemble(profile: String): F[AssembledPrompt]

}
