package com.repcheck.prompt.engine

import cats.MonadThrow
import cats.syntax.all._

import io.circe.parser.decode

import repcheck.shared.models.llm.prompt.AssembledPrompt

/**
 * Assembles each profile's STATIC system prompt ONCE at startup — its ordered GCS system instruction blocks (per-task
 * block + the shared tool-use/follow-up-loop block). It does NOT handle tools: a tool's model-facing description
 * reaches the LLM through the tools channel (`ToolRegistry` → runner → provider `toolJson`/`toolOf`), per `runtime/03`
 * steps 3-4-5 — the assembler and the registry are independent concerns. After [[load]], `assemble` is a pure cached
 * lookup (the system text never varies per cluster); the per-task variation is the caller appending its content via
 * `AssembledPrompt.appended`. `messages` is empty.
 */
final class DefaultChainAssembler[F[_]: MonadThrow] private (assembled: Map[String, AssembledPrompt])
    extends ChainAssembler[F] {

  def assemble(profile: String): F[AssembledPrompt] =
    assembled.get(profile).toRight(UnknownProfile(profile, assembled.keySet)).liftTo[F]

}

object DefaultChainAssembler {

  def load[F[_]: MonadThrow](loader: BlockLoader[F], profileNames: List[String]): F[ChainAssembler[F]] =
    profileNames
      .traverse(name => assembleOne(loader, name).map(name -> _))
      .map(pairs => new DefaultChainAssembler[F](pairs.toMap))

  private def assembleOne[F[_]: MonadThrow](loader: BlockLoader[F], profileName: String): F[AssembledPrompt] =
    for {
      raw    <- loader.loadProfile(profileName)
      doc    <- decode[PromptProfile](raw).leftMap(e => PromptProfileParseFailed(profileName, e.getMessage)).liftTo[F]
      blocks <- doc.systemBlocks.traverse(loader.load)
    } yield AssembledPrompt(system = blocks.mkString("\n\n"), messages = Nil)

}
