package com.repcheck.prompt.engine

import scala.concurrent.duration.DurationLong

import io.circe.Decoder
import io.circe.generic.semiauto.deriveDecoder

import repcheck.shared.models.llm.agentic.LoopPolicy

/**
 * A GCS profile document (e.g. `taxonomy-build`, `cluster-concept`): which system blocks compose its prompt, which
 * tools it grants (least privilege), and the `LoopPolicy` capping its agentic loop. D19: the policy lives HERE so it is
 * tunable without a deploy; D4 narrows `maxIterations` adaptively at call time.
 */
final case class PromptProfile(
  systemBlocks: List[String],
  tools: List[ToolBinding],
  loopPolicy: PromptProfile.LoopPolicyDoc,
)

object PromptProfile {

  /** Wire shape of the loop policy in the GCS doc; `toLoopPolicy` lifts it to the F1 contract type. */
  final case class LoopPolicyDoc(maxIterations: Int, perCallTimeoutSeconds: Long, tokenBudget: Option[Int]) {

    def toLoopPolicy: LoopPolicy =
      LoopPolicy(maxIterations, perCallTimeout = perCallTimeoutSeconds.seconds, tokenBudget = tokenBudget)

  }

  object LoopPolicyDoc {
    given Decoder[LoopPolicyDoc] = deriveDecoder[LoopPolicyDoc]
  }

  given Decoder[PromptProfile] = deriveDecoder[PromptProfile]
}
