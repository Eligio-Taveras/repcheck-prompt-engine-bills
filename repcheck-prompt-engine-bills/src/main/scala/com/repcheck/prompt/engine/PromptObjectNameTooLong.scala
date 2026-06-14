package com.repcheck.prompt.engine

/** A constructed GCS object name exceeded GCS's object-name length limit — caught before the read, not by GCS. */
final case class PromptObjectNameTooLong(objectName: String, bytes: Int, limit: Int)
    extends Exception(s"prompt object name is $bytes bytes, over the $limit-byte GCS limit: '$objectName'")
