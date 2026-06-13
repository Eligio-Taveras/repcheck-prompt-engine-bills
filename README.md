# repcheck-prompt-engine-bills

Prompt assembly for the bill-decomposition LLM tasks (plan F4). Loads versioned prompt fragments from GCS and composes
them into the `AssembledPrompt` the llm-adapter runner consumes; binds each profile's declared tool names to injected
code `LlmTool` impls (overriding only their model-facing descriptions) and carries the per-profile `LoopPolicy`.

- `BlockLoader` / `GcsPromptBlockLoader` — fetch a fragment by logical id (semver in the object name).
- `ToolRegistry.load` — read + validate every profile ONCE at startup (unknown tool name or malformed profile fails
  loudly here); after load, `toolsFor` / `policyFor` are pure, total (`Either[UnknownProfile, …]`).
- `DefaultChainAssembler.load` — pre-assemble each profile's static system prompt once; `assemble(profile)` is a
  cached pure lookup (per-task content is appended by the caller via `AssembledPrompt.appended`).

## GCS layout

Buckets are `repcheck-prompts-{dev|stg|prod}`; fragments live under `prompts/bills/` in this repo and upload to the
same layout (`scripts/upload-prompts.sh <bucket>`):

```
bills/profiles/<profile>-vX.Y.Z.json     # systemBlocks + tools[{name, descriptionBlock}] + loopPolicy
bills/<system-block>-vX.Y.Z.md           # instruction text
bills/tool-use-follow-up-vX.Y.Z.md       # shared loop-protocol block
bills/tools/<tool>-vX.Y.Z.md             # one description block per tool
```

The seeded fragments carry **placeholder wording** — author the production prompt content before promoting past dev.

## Conformance tests against real GCS (DockerRequired)

`GcsPromptBlockLoaderConformanceSpec` runs against a fake-gcs-server (excluded from `sbt test`):

```bash
docker run -d -p 4443:4443 fsouza/fake-gcs-server -scheme http -public-host localhost:4443
```

Then (`STORAGE_EMULATOR_HOST` defaults to `http://localhost:4443`):

```text
set repcheckpromptenginebills / Test / testOptions := Seq(Tests.Argument(TestFrameworks.ScalaTest, "-n", "DockerRequired"))
repcheckpromptenginebills/testOnly com.repcheck.prompt.engine.GcsPromptBlockLoaderConformanceSpec
```

---

A new RepCheck module

Part of the [RepCheck](https://github.com/Eligio-Taveras) platform -- a citizen-facing system that helps users understand how their legislators vote relative to their personal political interests.

## Dependencies

This module depends on the following shared libraries:

- **[shared-models](https://github.com/Eligio-Taveras/repcheck-shared-models)** -- Domain types (DTOs, DOs, enums, type classes)
- **[pipeline-models](https://github.com/Eligio-Taveras/repcheck-pipeline-models)** -- Pipeline operational types (events, retry, error classification, workflow)

## Tech Stack

| Concern | Technology |
|---------|-----------|
| Language | Scala 3.7.3 |
| Effect system | Cats Effect (tagless final `F[_]`) |
| HTTP | http4s Ember |
| JSON | Circe (semi-auto derivation) |
| Streaming | FS2 |
| Config | PureConfig (auto-derivation) |
| Testing | ScalaTest + MockitoScala + WireMock |
| Build | SBT 1.9.9 |
| Linting | WartRemover, Scalafix, tpolecat |
| Container | Google Distroless Java 21 |

## Prerequisites

- JDK 21 (Temurin recommended)
- SBT 1.9.9

## Build Commands

```bash
sbt compile              # Compile with WartRemover + tpolecat
sbt test                 # Run all tests
sbt scalafmtCheckAll     # Check formatting (fails if unformatted)
sbt scalafmtAll          # Auto-format all source files
sbt scalafixAll --check  # Check import ordering and lint rules
sbt scalafixAll          # Auto-fix import ordering
sbt coverage test coverageReport  # Run tests with coverage
```

## Project Structure

```
repcheck-prompt-engine-bills/
  src/
    main/scala/          # Application code
    test/scala/          # Tests
doc-generator/           # Doc compression utility
docs/                    # Full documentation
.claude/agent-docs/      # Compressed docs for agents
```

## CI Checks

Before pushing, always run the full CI check suite:

```bash
source scripts/ci-functions.sh
CreatePR "title" "body"   # For new PRs: runs checks, pushes, creates PR
pushToPR                   # For existing PRs: runs checks, pushes
```

## Publishing

Published to [GitHub Packages](https://github.com/Eligio-Taveras/repcheck-prompt-engine-bills/packages) via sbt-dynver (git-based semver). Add as a dependency:

```scala
libraryDependencies += "com.repcheck" %% "repcheck-prompt-engine-bills" % "<version>"
```

## Documentation

See `CLAUDE.md` for the agent routing guide, coding conventions, and task routing table.
