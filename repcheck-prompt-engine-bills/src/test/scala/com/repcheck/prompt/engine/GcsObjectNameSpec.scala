package com.repcheck.prompt.engine

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class GcsObjectNameSpec extends AnyFlatSpec with Matchers {

  "fragment" should "place a json fragment under the prefix with the version suffix" in {
    GcsObjectName.fragment("bills", "system-cluster-concept-identification", "v1.0.0") shouldBe
      "bills/system-cluster-concept-identification-v1.0.0.json"
  }

  it should "keep a nested fragment name intact" in {
    GcsObjectName.fragment("bills", "tools/search-taxonomy", "v2.1.0") shouldBe
      "bills/tools/search-taxonomy-v2.1.0.json"
  }

  "taskSpec" should "place a json task spec under task-specs/ with the version suffix" in {
    GcsObjectName.taskSpec("bills", "taxonomy-build", "v1.0.0") shouldBe
      "bills/task-specs/taxonomy-build-v1.0.0.json"
  }

  it should "tolerate surrounding slashes in the prefix" in {
    GcsObjectName.fragment("/bills/", "x", "v1.0.0") shouldBe "bills/x-v1.0.0.json"
  }

  it should "drop an all-slash (empty) prefix entirely" in {
    GcsObjectName.fragment("///", "x", "v1.0.0") shouldBe "x-v1.0.0.json"
  }

}
