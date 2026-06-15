package com.repcheck.prompt.engine

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class ToolBindingSpec extends AnyFlatSpec with Matchers {

  "descriptionObjectName" should "place a tool description as a versioned .md object under the prefix" in {
    ToolBinding.descriptionObjectName("tools/search-taxonomy", "bills", "v1.0.0") shouldBe
      Right("bills/tools/search-taxonomy-v1.0.0.md")
  }

}
