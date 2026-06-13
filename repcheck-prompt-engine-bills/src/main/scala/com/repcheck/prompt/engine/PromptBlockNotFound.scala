package com.repcheck.prompt.engine

/** A declared block id resolved to no object in the bucket — a missing or mis-versioned GCS prompt fragment. */
final case class PromptBlockNotFound(blockId: String, bucket: String, objectName: String)
    extends Exception(s"prompt block '$blockId' not found at gs://$bucket/$objectName")
