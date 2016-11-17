package com.hivehome.kafka.connect.sqs

import javax.jms.Message

import com.amazon.sqs.javamessaging.message.{SQSObjectMessage, SQSBytesMessage, SQSTextMessage}

object MessageExtractor {
  def apply(msg: Message): String = msg match {
    case text: SQSTextMessage => text.getText
    case bytes: SQSBytesMessage => new String(bytes.getBodyAsBytes)
    case objectMsg: SQSObjectMessage => objectMsg.getObject.toString
    case _ => msg.toString
  }
}
