package com.repcheck.prompt.engine

import scala.concurrent.duration.DurationLong

import io.circe.Decoder
import io.circe.generic.semiauto.deriveDecoder

import repcheck.shared.models.llm.agentic.LoopPolicy
import repcheck.shared.models.prompt.{PromptProfile, StageConfig}

/**
 * The GCS document for one agentic decomposition task (e.g. `cluster-concept-identification`, `taxonomy-build`): the
 * staged prompt `chain` (shared §1.7 — stage-ordered, weighted instruction blocks) PLUS the agentic extras the
 * decomposition engine adds — the `tools` the profile grants (least privilege) and the `loopPolicy` capping its agentic
 * loop. [[promptProfile]] bridges the chain to the shared assembler; `tools`/`loopPolicy` are consumed by the
 * registry/runner, never by the assembler. This is the F4-side reconciliation: rich blocks from shared-models, agentic
 * fields kept local.
 */
final case class AgenticProfile(
  name: String,
  chain: List[StageConfig],
  tools: List[ToolBinding],
  loopPolicy: AgenticProfile.LoopPolicyDoc,
) {

  /** The shared §1.7 view consumed by `DefaultChainAssembler` — the staged prompt, sans agentic fields. */
  def promptProfile: PromptProfile = PromptProfile(name, chain)

}

object AgenticProfile {

  /**
   * Wire shape of the loop policy in the GCS doc; the shared `LoopPolicy` is runtime-only (no codec), so it is lifted.
   */
  final case class LoopPolicyDoc(maxIterations: Int, perCallTimeoutSeconds: Long, tokenBudget: Option[Int]) {

    def toLoopPolicy: LoopPolicy =
      LoopPolicy(maxIterations, perCallTimeout = perCallTimeoutSeconds.seconds, tokenBudget = tokenBudget)

  }

  object LoopPolicyDoc {
    given Decoder[LoopPolicyDoc] = deriveDecoder[LoopPolicyDoc]
  }

  given Decoder[AgenticProfile] = deriveDecoder[AgenticProfile]
}
