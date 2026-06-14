package com.repcheck.prompt.engine

import cats.effect.IO

import repcheck.shared.models.prompt.InstructionBlock

/**
 * Test seam for the assembler: structured blocks and profiles served from maps; a miss raises [[PromptBlockNotFound]].
 */
final class InMemoryBlockLoader(blocks: Map[String, InstructionBlock], profiles: Map[String, AgenticProfile])
    extends BlockLoader[IO] {

  def load(blockName: String): IO[InstructionBlock] =
    IO.fromOption(blocks.get(blockName))(PromptBlockNotFound(blockName, "in-memory", blockName))

  def loadProfile(profileName: String): IO[AgenticProfile] =
    IO.fromOption(profiles.get(profileName))(PromptBlockNotFound(profileName, "in-memory", profileName))

}
