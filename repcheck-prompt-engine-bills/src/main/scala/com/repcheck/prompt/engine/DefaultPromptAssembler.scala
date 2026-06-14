package com.repcheck.prompt.engine

import cats.MonadThrow
import cats.syntax.all._

import repcheck.shared.models.llm.prompt.AssembledPrompt
import repcheck.shared.models.prompt.{DefaultChainAssembler, PromptFragment}

/**
 * Loads each task spec's chain + its referenced fragments ONCE at startup ([[DefaultPromptAssembler.load]]), then
 * assembles per call by delegating to the shared `DefaultChainAssembler` (stage order + weight translation +
 * `{{context}}` injection). After load, assembly is pure; only the caller's `context` varies per cluster.
 */
final class DefaultPromptAssembler[F[_]: MonadThrow] private (
  resolved: Map[String, DefaultPromptAssembler.Resolved]
) extends PromptAssembler[F] {

  def assemble(taskSpec: String, context: Map[String, String]): F[AssembledPrompt] =
    resolved
      .get(taskSpec)
      .toRight(UnknownTaskSpec(taskSpec, resolved.keySet))
      .liftTo[F]
      .flatMap(r => DefaultPromptAssembler.assembleOne(r.taskSpec, r.fragments, context).liftTo[F])

}

object DefaultPromptAssembler {

  final private case class Resolved(taskSpec: AgenticTaskSpec, fragments: Map[String, PromptFragment])

  def load[F[_]: MonadThrow](loader: PromptLoader[F], taskSpecNames: List[String]): F[PromptAssembler[F]] =
    taskSpecNames
      .traverse(name => resolveOne(loader, name).map(name -> _))
      .map(pairs => new DefaultPromptAssembler[F](pairs.toMap))

  private def resolveOne[F[_]: MonadThrow](loader: PromptLoader[F], taskSpecName: String): F[Resolved] =
    for {
      taskSpec <- loader.loadTaskSpec(taskSpecName)
      fragments <-
        taskSpec.chain.flatMap(_.promptFragmentNames).distinct.traverse(n => loader.load(n).map(n -> _)).map(_.toMap)
    } yield Resolved(taskSpec, fragments)

  /** Pure composition step — package-private so its failure branch is unit-testable without GCS. */
  private[engine] def assembleOne(
    taskSpec: AgenticTaskSpec,
    fragments: Map[String, PromptFragment],
    context: Map[String, String],
  ): Either[PromptAssemblyFailed, AssembledPrompt] =
    DefaultChainAssembler
      .assemble(taskSpec.promptProfile, fragments, context)
      .bimap(detail => PromptAssemblyFailed(taskSpec.name, detail), system => AssembledPrompt(system, Nil))

}
