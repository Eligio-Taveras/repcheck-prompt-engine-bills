package com.repcheck.prompt.engine

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class GcsObjectNameSpec extends AnyFlatSpec with Matchers {

  "block" should "place a json block under the prefix with the version suffix" in {
    GcsObjectName.block("bills", "system-cluster-concept-identification", "v1.0.0") shouldBe
      "bills/system-cluster-concept-identification-v1.0.0.json"
  }

  it should "keep a nested block name intact" in {
    GcsObjectName.block("bills", "tools/search-taxonomy", "v2.1.0") shouldBe
      "bills/tools/search-taxonomy-v2.1.0.json"
  }

  "profile" should "place a json profile under profiles/ with the version suffix" in {
    GcsObjectName.profile("bills", "taxonomy-build", "v1.0.0") shouldBe
      "bills/profiles/taxonomy-build-v1.0.0.json"
  }

  it should "tolerate surrounding slashes in the prefix" in {
    GcsObjectName.block("/bills/", "x", "v1.0.0") shouldBe "bills/x-v1.0.0.json"
  }

  it should "drop an all-slash (empty) prefix entirely" in {
    GcsObjectName.block("///", "x", "v1.0.0") shouldBe "x-v1.0.0.json"
  }

}
