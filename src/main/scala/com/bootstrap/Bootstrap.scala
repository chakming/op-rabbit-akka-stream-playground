package com.bootstrap

import akka.actor.{ActorSystem, Props}
import akka.stream.ActorMaterializer
import com.spingo.op_rabbit.Directives._
import com.spingo.op_rabbit._
import com.spingo.op_rabbit.stream.RabbitSource
import com.timcharper.acked.AckedSink
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

  RabbitSource(
    rabbitControl,
    channel(qos),
    consume(queue("censorship.inbound.queue", durable = true, exclusive = false, autoDelete = false)),
    body(as[String])
  )
    .mapAsync(3)(DomainService.expensiveCall)
    .map(DomainService.classify)
    .map {
      case MessageSafe(msg) => publish("censorship.outbound.okqueue", msg)
      case MessageThreat(msg) => publish("censorship.outbound.notokqueue", msg)
    }
    .runAck

  private def publish(queueName: String, msg: String)(implicit ec: ExecutionContext) = {
    logger.debug("queue: {}, msg: {}", queueName, msg)
    val publisher = Publisher.queue(Queue.passive(queue(queueName, durable = true, exclusive = false, autoDelete = false)))
    rabbitControl ! Message(
      body = msg.getBytes,
      publisher = publisher
    )
  }
}
