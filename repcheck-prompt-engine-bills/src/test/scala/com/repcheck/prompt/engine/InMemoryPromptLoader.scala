package com.repcheck.prompt.engine

import cats.effect.IO

import repcheck.shared.models.prompt.PromptFragment

/**
 * Test seam for the engine: fragments, task specs, and tool descriptions served from maps; a miss raises
 * [[PromptObjectNotFound]].
 */
final class InMemoryPromptLoader(
  fragments: Map[String, PromptFragment],
  taskSpecs: Map[String, AgenticTaskSpec],
  toolDescriptions: Map[String, String] = Map.empty,
) extends PromptLoader[IO] {

  def load(fragmentName: String): IO[PromptFragment] =
    IO.fromOption(fragments.get(fragmentName))(PromptObjectNotFound(fragmentName, "in-memory", fragmentName))

  def loadTaskSpec(taskSpecName: String): IO[AgenticTaskSpec] =
    IO.fromOption(taskSpecs.get(taskSpecName))(PromptObjectNotFound(taskSpecName, "in-memory", taskSpecName))

  def loadToolDescription(descriptionRef: String): IO[String] =
    IO.fromOption(toolDescriptions.get(descriptionRef))(
      PromptObjectNotFound(descriptionRef, "in-memory", descriptionRef)
    )

}
