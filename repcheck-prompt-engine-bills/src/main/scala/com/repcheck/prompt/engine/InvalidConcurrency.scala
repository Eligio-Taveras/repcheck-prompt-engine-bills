package com.repcheck.prompt.engine

/**
 * A tool registry was asked to load with a non-positive concurrency — a config bug; fails loudly at load, not later.
 */
final case class InvalidConcurrency(concurrency: Int)
    extends Exception(s"tool registry load concurrency must be >= 1, got: $concurrency")
