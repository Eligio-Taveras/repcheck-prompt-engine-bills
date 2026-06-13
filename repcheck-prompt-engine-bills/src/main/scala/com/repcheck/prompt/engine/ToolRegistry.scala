package com.repcheck.prompt.engine

import repcheck.shared.models.llm.agentic.LoopPolicy
import repcheck.shared.models.llm.tool.LlmTool

/**
 * Per-profile tool sets + loop policy, resolved from GCS. Built effectfully ONCE via [[ToolRegistry.load]] (which reads
 * + validates every profile document); after that, lookups are PURE and TOTAL — an unknown profile is a `Left`, never a
 * throw. Each tool returned is the injected code impl with its `ToolSpec.description` overridden by the GCS block (name
 * \= stable key; schemas + examples stay codec-derived, so they can't drift).
 */
trait ToolRegistry[F[_]] {

  def toolsFor(profile: String): Either[UnknownProfile, List[LlmTool[F]]]

  def policyFor(profile: String): Either[UnknownProfile, LoopPolicy]

}
