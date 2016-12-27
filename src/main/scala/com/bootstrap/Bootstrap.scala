package com.bootstrap

import akka.actor.{ActorSystem, Props}
import akka.stream.ActorMaterializer
import com.spingo.op_rabbit.Directives._
import com.spingo.op_rabbit._
import com.spingo.op_rabbit.stream.RabbitSource
import com.typesafe.scalalogging.LazyLogging

import scala.concurrent.ExecutionContext

object Bootstrap extends App with LazyLogging {
  implicit val ec = ExecutionContext.global
  implicit val system = ActorSystem("MainSystem")
  implicit val materializer = ActorMaterializer()
  implicit val recoveryStrategy = RecoveryStrategy.limitedRedeliver(
    onAbandon = RecoveryStrategy.abandonedQueue()
  )
  val qos = 1
  val rabbitControl = system.actorOf(Props[RabbitControl])

  implicit val simpleStringMarshaller = new RabbitMarshaller[String] with RabbitUnmarshaller[String] {
    val contentType = "text/plain"
    val contentEncoding = Some("UTF-8")

    def marshall(value: String) = value.getBytes
    def unmarshall(value: Array[Byte], contentType: Option[String], charset: Option[String]) = new String(value)
  }

  val source = RabbitSource(
    rabbitControl,
    channel(qos),
    consume(queue("such-queue", durable = true, exclusive = false, autoDelete = false)),
    body(as[String])
  ).runForeach { result =>
    logger.debug("string: {}", result)
  }

}
