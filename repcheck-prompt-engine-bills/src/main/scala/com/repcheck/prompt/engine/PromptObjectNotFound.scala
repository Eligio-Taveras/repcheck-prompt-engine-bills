package com.repcheck.prompt.engine

/** A logical name resolved to no object in the bucket — a missing or mis-versioned prompt fragment or task spec. */
final case class PromptObjectNotFound(id: String, bucket: String, objectName: String)
    extends Exception(s"prompt object '$id' not found at gs://$bucket/$objectName")
