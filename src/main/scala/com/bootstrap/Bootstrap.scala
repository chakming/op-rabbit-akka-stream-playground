package com.bootstrap

import akka.NotUsed
import akka.actor.{ActorSystem, Props}
import akka.stream.ActorMaterializer
import com.spingo.op_rabbit.Directives._
import com.spingo.op_rabbit._
import com.spingo.op_rabbit.stream.RabbitSource
import com.timcharper.acked.AckedFlow
import com.typesafe.scalalogging.LazyLogging

import scala.concurrent.ExecutionContext

object Bootstrap extends App with LazyLogging with FlowFactory {
  implicit val ec = ExecutionContext.global
  implicit val system = ActorSystem("MainSystem")
  implicit val materializer = ActorMaterializer()
  implicit val recoveryStrategy = RecoveryStrategy.limitedRedeliver(
    onAbandon = RecoveryStrategy.abandonedQueue()
  )
  val qos = 1
  val rabbitControl = system.actorOf(Props[RabbitControl])

  RabbitSource(
    rabbitControl,
    channel(qos),
    consume(queue("censorship.inbound.queue", durable = true, exclusive = false, autoDelete = false)),
    body(as[String])
  ).via(domainProcessing)
    .via(publishMapping)
    .map(rabbitControl ! _)
    .runAck
}

trait FlowFactory {

  def domainProcessing(implicit ec: ExecutionContext): AckedFlow[String, CensoredMessage, NotUsed] =
    AckedFlow[String]
      .mapAsync(3)(DomainService.expensiveCall)
      .map(DomainService.classify)

  def publishMapping: AckedFlow[CensoredMessage, Message, NotUsed] =
    AckedFlow[CensoredMessage].map {
      case MessageSafe(msg) =>
        Message(
          body = msg.getBytes,
          publisher = publisher("censorship.outbound.okqueue")
        )
      case MessageThreat(msg) => Message(
        body = msg.getBytes,
        publisher = publisher("censorship.outbound.notokqueue")
      )
    }

  private def publisher(queueName: String): Publisher = {
    Publisher.queue(queue(queueName, durable = true, exclusive = false, autoDelete = false))
  }

}
