package com.hivehome.kafka.connect.sqs

import com.amazon.sqs.javamessaging.message.{SQSObjectMessage, SQSBytesMessage, SQSTextMessage}
import org.scalacheck.Gen
import org.scalatest.prop.GeneratorDrivenPropertyChecks
import org.scalatest.{FunSuite, Matchers}

class MessageExtractorSuite extends FunSuite with GeneratorDrivenPropertyChecks with Matchers {

  test("should extract text message") {
    forAll(Gen.alphaStr) { text =>
      val msg = new SQSTextMessage(text)

      val actual = MessageExtractor(msg)

      actual shouldEqual text
    }
  }

  test("should extract bytes message") {
    forAll(Gen.alphaStr) { text =>
      val msg = new SQSBytesMessage()
      msg.writeBytes(text.getBytes)

      val actual = MessageExtractor(msg)

      actual shouldEqual text
    }
  }

  test("should extract object message") {
    forAll(Gen.alphaStr) { text =>
      val msg = new SQSObjectMessage()
      msg.setObject(text)

      val actual = MessageExtractor(msg)

      actual shouldEqual text
    }
  }
}
