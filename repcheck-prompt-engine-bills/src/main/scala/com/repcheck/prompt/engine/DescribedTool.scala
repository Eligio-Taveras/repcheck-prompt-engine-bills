package com.repcheck.prompt.engine

import io.circe.Json

import repcheck.shared.models.llm.tool.{LlmTool, ToolInputError, ToolSpec}

/**
 * An injected code [[LlmTool]] with only its model-facing description replaced by the GCS text — everything else
 * (decode/execute/encode, and the codec-derived schemas + examples) delegates unchanged. This is how a task spec tunes
 * tool wording without a deploy while the contract stays code-owned.
 */
final private[engine] class DescribedTool[F[_]](val underlying: LlmTool[F], description: String) extends LlmTool[F] {
  type In  = underlying.In
  type Out = underlying.Out

  val spec: ToolSpec = underlying.spec.copy(description = description)

  def decode(args: Json): Either[ToolInputError, In] = underlying.decode(args)
  def execute(in: In): F[Out]                        = underlying.execute(in)
  def encodeResult(out: Out): Json                   = underlying.encodeResult(out)
}
