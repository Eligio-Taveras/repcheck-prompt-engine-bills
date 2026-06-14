package com.repcheck.prompt.engine

import pureconfig.ConfigReader

/**
 * Where the prompt fragments live and which version to read. `bucket` is `repcheck-prompts-{dev|stg|prod}`; `version`
 * (semver, e.g. `v1.0.0`) selects the filename suffix — app-configurable, CI sets the default on release.
 */
final case class PromptEngineConfig(bucket: String, prefix: String, version: String) derives ConfigReader
