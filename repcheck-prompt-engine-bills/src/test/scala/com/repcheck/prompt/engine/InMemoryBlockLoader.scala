package com.repcheck.prompt.engine

import cats.effect.IO

/**
 * Test seam for the assembler/registry: blocks and profiles served from maps; a miss raises [[PromptBlockNotFound]].
 */
final class InMemoryBlockLoader(blocks: Map[String, String], profiles: Map[String, String]) extends BlockLoader[IO] {

  def load(blockId: String): IO[String] =
    IO.fromOption(blocks.get(blockId))(PromptBlockNotFound(blockId, "in-memory", blockId))

  def loadProfile(profileName: String): IO[String] =
    IO.fromOption(profiles.get(profileName))(PromptBlockNotFound(profileName, "in-memory", profileName))

}
