package com.repcheck.prompt.engine

import cats.MonadThrow
import cats.syntax.all._

import io.circe.parser.decode

import repcheck.shared.models.llm.agentic.LoopPolicy
import repcheck.shared.models.llm.tool.LlmTool

/** A profile fully resolved at load: its bound tools (descriptions applied) and its loop policy. */
final private[engine] case class ResolvedProfile[F[_]](tools: List[LlmTool[F]], policy: LoopPolicy)

final class DefaultToolRegistry[F[_]] private (resolved: Map[String, ResolvedProfile[F]]) extends ToolRegistry[F] {

  def toolsFor(profile: String): Either[UnknownProfile, List[LlmTool[F]]] =
    lookup(profile).map(_.tools)

  def policyFor(profile: String): Either[UnknownProfile, LoopPolicy] =
    lookup(profile).map(_.policy)

  private def lookup(profile: String): Either[UnknownProfile, ResolvedProfile[F]] =
    resolved.get(profile).toRight(UnknownProfile(profile, resolved.keySet))

}

object DefaultToolRegistry {

  /**
   * Read + validate every profile ONCE. Fails loudly here — not at classify time — on a malformed profile document
   * ([[PromptProfileParseFailed]]) or a declared tool name with no matching code impl ([[UnknownToolBinding]]). After
   * this returns, lookups are pure and total.
   *
   * @param available
   *   the code [[LlmTool]] impls keyed by their stable name; a profile may grant any subset (least privilege)
   */
  def load[F[_]: MonadThrow](
    loader: BlockLoader[F],
    available: Map[String, LlmTool[F]],
    profileNames: List[String],
  ): F[ToolRegistry[F]] =
    profileNames
      .traverse(name => resolveProfile(loader, available, name).map(name -> _))
      .map(pairs => new DefaultToolRegistry[F](pairs.toMap))

  private def resolveProfile[F[_]: MonadThrow](
    loader: BlockLoader[F],
    available: Map[String, LlmTool[F]],
    profileName: String,
  ): F[ResolvedProfile[F]] =
    for {
      raw     <- loader.loadProfile(profileName)
      profile <- decode[PromptProfile](raw).leftMap(e => PromptProfileParseFailed(profileName, e.getMessage)).liftTo[F]
      tools   <- profile.tools.traverse(binding => bindTool(loader, available, profileName, binding))
    } yield ResolvedProfile(tools, profile.loopPolicy.toLoopPolicy)

  private def bindTool[F[_]: MonadThrow](
    loader: BlockLoader[F],
    available: Map[String, LlmTool[F]],
    profileName: String,
    binding: ToolBinding,
  ): F[LlmTool[F]] =
    available.get(binding.name) match {
      case None => MonadThrow[F].raiseError(UnknownToolBinding(profileName, binding.name, available.keySet))
      case Some(impl) =>
        loader.load(binding.descriptionBlock).map(description => new DescribedTool[F](impl, description))
    }

}
