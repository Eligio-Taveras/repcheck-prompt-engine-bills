package com.repcheck.prompt.engine

import cats.effect.Concurrent
import cats.effect.syntax.all._
import cats.syntax.all._

import repcheck.shared.models.llm.agentic.LoopPolicy
import repcheck.shared.models.llm.tool.LlmTool

/** A task spec fully resolved at load: its bound tools (descriptions applied) and its loop policy. */
final private[engine] case class ResolvedTaskSpec[F[_]](tools: List[LlmTool[F, ?, ?]], policy: LoopPolicy)

final class DefaultToolRegistry[F[_]] private (resolved: Map[String, ResolvedTaskSpec[F]]) extends ToolRegistry[F] {

  def toolsFor(taskSpec: String): Either[UnknownTaskSpec, List[LlmTool[F, ?, ?]]] =
    lookup(taskSpec).map(_.tools)

  def policyFor(taskSpec: String): Either[UnknownTaskSpec, LoopPolicy] =
    lookup(taskSpec).map(_.policy)

  private def lookup(taskSpec: String): Either[UnknownTaskSpec, ResolvedTaskSpec[F]] =
    resolved.get(taskSpec).toRight(UnknownTaskSpec(taskSpec, resolved.keySet))

}

object DefaultToolRegistry {

  /**
   * Read + validate every task spec ONCE, resolving task specs (and the tools within each) concurrently up to
   * `concurrency`. Fails loudly here — not at classify time — on a non-positive `concurrency` ([[InvalidConcurrency]]),
   * a malformed task-spec document ([[PromptTaskSpecParseFailed]]), or a declared tool name with no matching code impl
   * ([[UnknownToolBinding]]). After this returns, lookups are pure and total.
   *
   * @param available
   *   the code [[LlmTool]] impls keyed by their stable name; a task spec may grant any subset (least privilege)
   * @param concurrency
   *   max in-flight GCS loads (MUST be >= 1; bounds both the task-spec fan-out and the per-task-spec tool fan-out)
   */
  def load[F[_]: Concurrent](
    loader: PromptLoader[F],
    available: Map[String, LlmTool[F, ?, ?]],
    taskSpecNames: List[String],
    concurrency: Int,
  ): F[ToolRegistry[F]] =
    if (concurrency < 1) {
      InvalidConcurrency(concurrency).raiseError[F, ToolRegistry[F]]
    } else {
      taskSpecNames
        .parTraverseN(concurrency)(name => resolveTaskSpec(loader, available, name, concurrency).map(name -> _))
        .map(pairs => new DefaultToolRegistry[F](pairs.toMap))
    }

  private def resolveTaskSpec[F[_]: Concurrent](
    loader: PromptLoader[F],
    available: Map[String, LlmTool[F, ?, ?]],
    taskSpecName: String,
    concurrency: Int,
  ): F[ResolvedTaskSpec[F]] =
    for {
      taskSpec <- loader.loadTaskSpec(taskSpecName)
      tools <- taskSpec.tools.parTraverseN(concurrency)(binding => bindTool(loader, available, taskSpecName, binding))
    } yield ResolvedTaskSpec(tools, taskSpec.loopPolicy.toLoopPolicy)

  private def bindTool[F[_]: Concurrent](
    loader: PromptLoader[F],
    available: Map[String, LlmTool[F, ?, ?]],
    taskSpecName: String,
    binding: ToolBinding,
  ): F[LlmTool[F, ?, ?]] =
    available.get(binding.name) match {
      case None => UnknownToolBinding(taskSpecName, binding.name, available.keySet).raiseError[F, LlmTool[F, ?, ?]]
      case Some(impl) =>
        loader.loadToolDescription(binding.descriptionRef).map(description => describe(impl, description))
    }

  /**
   * Wrap an injected tool with its GCS description; the helper opens the heterogeneous tool's In/Out for
   * [[DescribedTool]].
   */
  private def describe[F[_], In, Out](tool: LlmTool[F, In, Out], description: String): LlmTool[F, In, Out] =
    new DescribedTool(tool, description)

}
