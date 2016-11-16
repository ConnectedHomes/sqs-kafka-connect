package com.hivehome.kafka.connect.sqs

import org.apache.kafka.common.utils.AppInfoParser

object Version {
  def apply(): String = AppInfoParser.getVersion
}
