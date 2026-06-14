package com.repcheck.prompt.engine

import cats.effect.IO

import io.circe.Json

import repcheck.shared.models.llm.tool.{LlmTool, ToolInputError, ToolSpec}

/** A minimal code-side tool impl for registry tests; its description is what the GCS text overrides. */
final class EchoTool(name: String) extends LlmTool[IO] {
  type In  = String
  type Out = String

  val spec: ToolSpec = ToolSpec(name, "code-default description", Json.obj(), Json.obj(), Json.obj(), Json.obj())

  def decode(args: Json): Either[ToolInputError, String] =
    args.asString.toRight(ToolInputError("arguments", "expected a string"))

  def execute(in: String): IO[String] = IO.pure(in)
  def encodeResult(out: String): Json = Json.fromString(out)
}
