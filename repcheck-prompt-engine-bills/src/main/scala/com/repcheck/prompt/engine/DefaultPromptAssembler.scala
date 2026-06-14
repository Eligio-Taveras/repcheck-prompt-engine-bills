package com.repcheck.prompt.engine

import cats.MonadThrow
import cats.syntax.all._

import repcheck.shared.models.llm.prompt.AssembledPrompt
import repcheck.shared.models.prompt.{DefaultChainAssembler, InstructionBlock}

/**
 * Loads each profile's chain + its referenced blocks ONCE at startup ([[DefaultPromptAssembler.load]]), then assembles
 * per call by delegating to the shared `DefaultChainAssembler` (stage order + weight translation + `{{context}}`
 * injection). After load, assembly is pure; only the caller's `context` varies per cluster.
 */
final class DefaultPromptAssembler[F[_]: MonadThrow] private (
  resolved: Map[String, DefaultPromptAssembler.Resolved]
) extends PromptAssembler[F] {

  def assemble(profile: String, context: Map[String, String]): F[AssembledPrompt] =
    resolved
      .get(profile)
      .toRight(UnknownProfile(profile, resolved.keySet))
      .liftTo[F]
      .flatMap(r => DefaultPromptAssembler.assembleOne(r.profile, r.blocks, context).liftTo[F])

}

object DefaultPromptAssembler {

  final private case class Resolved(profile: AgenticProfile, blocks: Map[String, InstructionBlock])

  def load[F[_]: MonadThrow](loader: BlockLoader[F], profileNames: List[String]): F[PromptAssembler[F]] =
    profileNames
      .traverse(name => resolveOne(loader, name).map(name -> _))
      .map(pairs => new DefaultPromptAssembler[F](pairs.toMap))

  private def resolveOne[F[_]: MonadThrow](loader: BlockLoader[F], profileName: String): F[Resolved] =
    for {
      profile <- loader.loadProfile(profileName)
      blocks  <- profile.chain.flatMap(_.blockNames).distinct.traverse(n => loader.load(n).map(n -> _)).map(_.toMap)
    } yield Resolved(profile, blocks)

  /** Pure composition step — package-private so its failure branch is unit-testable without GCS. */
  private[engine] def assembleOne(
    profile: AgenticProfile,
    blocks: Map[String, InstructionBlock],
    context: Map[String, String],
  ): Either[PromptAssemblyFailed, AssembledPrompt] =
    DefaultChainAssembler
      .assemble(profile.promptProfile, blocks, context)
      .bimap(detail => PromptAssemblyFailed(profile.name, detail), system => AssembledPrompt(system, Nil))

}
