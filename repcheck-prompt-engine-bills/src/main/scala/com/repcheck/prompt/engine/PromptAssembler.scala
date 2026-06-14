package com.repcheck.prompt.engine

import repcheck.shared.models.llm.prompt.AssembledPrompt

/**
 * Assembles a decomposition task spec's system prompt from its staged GCS prompt fragments — delegating to the shared
 * §1.7 `ChainAssembler` (stage-ordered, weight-translated, `{{context}}`-injected) — into the F4→F2 contract
 * [[AssembledPrompt]]. It does NOT handle tools or the loop: those ride on the [[AgenticTaskSpec]] but are consumed by
 * the registry/runner. `messages` starts empty; the task caller (D4) appends the cluster/concept content before handing
 * the prompt to the runner.
 */
trait PromptAssembler[F[_]] {

  def assemble(taskSpec: String, context: Map[String, String]): F[AssembledPrompt]

}
