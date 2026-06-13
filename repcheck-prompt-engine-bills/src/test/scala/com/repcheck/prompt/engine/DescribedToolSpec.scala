package com.repcheck.prompt.engine

import cats.effect.IO
import cats.effect.testing.scalatest.AsyncIOSpec

import io.circe.Json

import org.scalatest.flatspec.AsyncFlatSpec
import org.scalatest.matchers.should.Matchers

class DescribedToolSpec extends AsyncFlatSpec with AsyncIOSpec with Matchers {

  private val described = new DescribedTool[IO](new EchoTool("echo"), "GCS-authored description")

  "DescribedTool" should "override only the description, keeping name/schemas from the code impl" in {
    described.spec.name shouldBe "echo"
    described.spec.description shouldBe "GCS-authored description"
    described.spec.parametersSchema shouldBe new EchoTool("echo").spec.parametersSchema
  }

  it should "delegate decode, execute, and encodeResult unchanged" in {
    described.decode(Json.obj()).isLeft shouldBe true // bad input still rejected by the underlying decoder
    described.decode(Json.fromString("hi")) match {
      case Right(in) =>
        described.execute(in).map(out => described.encodeResult(out)).asserting(_ shouldBe Json.fromString("hi"))
      case Left(err) => fail(s"expected a decoded input, got $err")
    }
  }

}
