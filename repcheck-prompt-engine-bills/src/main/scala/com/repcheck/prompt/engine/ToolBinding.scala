package com.repcheck.prompt.engine

import io.circe.Decoder
import io.circe.generic.semiauto.deriveDecoder

/**
 * One tool a profile grants: the stable code-side `name` (the key bound to an `LlmTool` impl) plus the GCS block id
 * holding its model-facing description. Schemas + examples come from the code codec; only the description is
 * GCS-tunable.
 */
final case class ToolBinding(name: String, descriptionBlock: String)

object ToolBinding {
  given Decoder[ToolBinding] = deriveDecoder[ToolBinding]
}
