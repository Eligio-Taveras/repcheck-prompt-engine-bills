package com.repcheck.prompt.engine

import cats.MonadThrow
import cats.syntax.all._

import repcheck.shared.models.llm.agentic.LoopPolicy
import repcheck.shared.models.llm.tool.LlmTool

/** A task spec fully resolved at load: its bound tools (descriptions applied) and its loop policy. */
final private[engine] case class ResolvedTaskSpec[F[_]](tools: List[LlmTool[F]], policy: LoopPolicy)

final class DefaultToolRegistry[F[_]] private (resolved: Map[String, ResolvedTaskSpec[F]]) extends ToolRegistry[F] {

  def toolsFor(taskSpec: String): Either[UnknownTaskSpec, List[LlmTool[F]]] =
    lookup(taskSpec).map(_.tools)

  def policyFor(taskSpec: String): Either[UnknownTaskSpec, LoopPolicy] =
    lookup(taskSpec).map(_.policy)

  private def lookup(taskSpec: String): Either[UnknownTaskSpec, ResolvedTaskSpec[F]] =
    resolved.get(taskSpec).toRight(UnknownTaskSpec(taskSpec, resolved.keySet))

}

object DefaultToolRegistry {

  /**
   * Read + validate every task spec ONCE. Fails loudly here — not at classify time — on a malformed task-spec document
   * ([[PromptTaskSpecParseFailed]]) or a declared tool name with no matching code impl ([[UnknownToolBinding]]). After
   * this returns, lookups are pure and total.
   *
   * @param available
   *   the code [[LlmTool]] impls keyed by their stable name; a task spec may grant any subset (least privilege)
   */
  def load[F[_]: MonadThrow](
    loader: PromptLoader[F],
    available: Map[String, LlmTool[F]],
    taskSpecNames: List[String],
  ): F[ToolRegistry[F]] =
    taskSpecNames
      .traverse(name => resolveTaskSpec(loader, available, name).map(name -> _))
      .map(pairs => new DefaultToolRegistry[F](pairs.toMap))

  private def resolveTaskSpec[F[_]: MonadThrow](
    loader: PromptLoader[F],
    available: Map[String, LlmTool[F]],
    taskSpecName: String,
  ): F[ResolvedTaskSpec[F]] =
    for {
      taskSpec <- loader.loadTaskSpec(taskSpecName)
      tools    <- taskSpec.tools.traverse(binding => bindTool(loader, available, taskSpecName, binding))
    } yield ResolvedTaskSpec(tools, taskSpec.loopPolicy.toLoopPolicy)

  private def bindTool[F[_]: MonadThrow](
    loader: PromptLoader[F],
    available: Map[String, LlmTool[F]],
    taskSpecName: String,
    binding: ToolBinding,
  ): F[LlmTool[F]] =
    available.get(binding.name) match {
      case None => MonadThrow[F].raiseError(UnknownToolBinding(taskSpecName, binding.name, available.keySet))
      case Some(impl) =>
        loader.loadToolDescription(binding.descriptionRef).map(description => new DescribedTool[F](impl, description))
    }

}
