package com.bootstrap

import akka.actor.{ActorSystem, Props}
import akka.stream.ActorMaterializer
import com.spingo.op_rabbit.Directives._
import com.spingo.op_rabbit._
import com.spingo.op_rabbit.stream.RabbitSource

object Bootstrap extends App {
  implicit val system = ActorSystem("MainSystem")
  implicit val materializer = ActorMaterializer()
  implicit val recoveryStrategy = RecoveryStrategy.limitedRedeliver(
    onAbandon = RecoveryStrategy.abandonedQueue()
  )
  val qos = 8

  val rabbitControl = system.actorOf(Props[RabbitControl])

  implicit val simpleIntMarshaller = new RabbitMarshaller[Int] with RabbitUnmarshaller[Int] {
    val contentType = "text/plain"
    val contentEncoding = Some("UTF-8")

    def marshall(value: Int) =
      value.toString.getBytes

    def unmarshall(value: Array[Byte], contentType: Option[String], charset: Option[String]) = {
      new String(value).toInt
    }
  }

  RabbitSource(
    rabbitControl,
    channel(qos),
    consume(queue("such-queue", durable = true, exclusive = false, autoDelete = false)),
    body(as[Int])
  ).runForeach { result =>
    println(result)
  }
}
