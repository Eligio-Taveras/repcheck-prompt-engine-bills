package com.repcheck.prompt.engine

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class GcsObjectNameSpec extends AnyFlatSpec with Matchers {

  "fragment" should "place a json fragment under the prefix with the version suffix" in {
    GcsObjectName.fragment("bills", "system-cluster-concept-identification", "v1.0.0") shouldBe
      Right("bills/system-cluster-concept-identification-v1.0.0.json")
  }

  it should "keep a nested fragment name intact" in {
    GcsObjectName.fragment("bills", "tools/search-taxonomy", "v2.1.0") shouldBe
      Right("bills/tools/search-taxonomy-v2.1.0.json")
  }

  it should "tolerate surrounding slashes in the prefix" in {
    GcsObjectName.fragment("/bills/", "x", "v1.0.0") shouldBe Right("bills/x-v1.0.0.json")
  }

  it should "drop an all-slash (empty) prefix entirely" in {
    GcsObjectName.fragment("///", "x", "v1.0.0") shouldBe Right("x-v1.0.0.json")
  }

  it should "reject a constructed name that exceeds the GCS object-name length limit" in {
    val huge = "n" * (GcsObjectName.maxObjectNameBytes + 1)
    GcsObjectName.fragment("bills", huge, "v1.0.0") match {
      case Left(e: PromptObjectNameTooLong) =>
        val _ = e.limit shouldBe GcsObjectName.maxObjectNameBytes
        e.bytes should be > GcsObjectName.maxObjectNameBytes
      case other => fail(s"expected PromptObjectNameTooLong, got $other")
    }
  }

  it should "allow a name right at the limit" in {
    val prefix = "bills"
    val suffix = "-v1.0.0.json"
    val name   = "n" * (GcsObjectName.maxObjectNameBytes - prefix.length - 1 - suffix.length)
    GcsObjectName.fragment(prefix, name, "v1.0.0").map(_.length) shouldBe Right(GcsObjectName.maxObjectNameBytes)
  }

}
