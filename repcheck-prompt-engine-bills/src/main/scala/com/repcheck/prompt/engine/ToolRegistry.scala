package com.repcheck.prompt.engine

import repcheck.shared.models.llm.agentic.LoopPolicy
import repcheck.shared.models.llm.tool.LlmTool

/**
 * Per-task-spec tool sets + loop policy, resolved from GCS. Built effectfully ONCE via [[DefaultToolRegistry.load]]
 * (which reads + validates every task-spec document); after that, lookups are PURE and TOTAL — an unknown task spec is
 * a `Left`, never a throw. Each tool returned is the injected code impl with its `ToolSpec.description` overridden by
 * the GCS description (name = stable key; schemas + examples stay codec-derived, so they can't drift).
 */
trait ToolRegistry[F[_]] {

  def toolsFor(taskSpec: String): Either[UnknownTaskSpec, List[LlmTool[F]]]

  def policyFor(taskSpec: String): Either[UnknownTaskSpec, LoopPolicy]

}
