package com.bootstrap

import com.typesafe.scalalogging.LazyLogging

import scala.concurrent.{ExecutionContext, Future}
import scala.util.Random

sealed trait CensoredMessage {
  def message: String
}

case class MessageSafe(message: String) extends CensoredMessage

case class MessageThreat(message: String) extends CensoredMessage

object DomainService extends LazyLogging {

  val unsafeWords = Set("terror")

  def classify(msg: String): CensoredMessage = {
    val unsafe = unsafeWords.exists(msg.contains)
    val processedMessage = msg + " [message processed]"

    if (unsafe) {
      logger.debug("message classified as 'threat'")
      MessageThreat(processedMessage)
    } else {
      logger.debug("message classified as 'safe': {}", msg)
      MessageSafe(processedMessage)
    }
  }

  def expensiveCall(msg: String)(implicit ec: ExecutionContext): Future[String] = Future {
    val millis = Random.nextInt(2000) + 1000
    logger.debug(s"message: '$msg' \n will be held for $millis ms")
    Thread.sleep(millis.toLong)
    msg
  }
}
