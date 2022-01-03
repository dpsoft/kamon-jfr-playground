package io.kamon.jfr.profiler.kafka

import io.kamon.jfr.profiler.Profiler.Sample
import io.kamon.jfr.profiler.serde.Jackson
import org.apache.kafka.clients.producer.{KafkaProducer, ProducerRecord}

import java.net.InetAddress
import java.util.Properties

object KafkaPusher:
  private val properties = Properties()
  properties.put("client.id", InetAddress.getLocalHost.getHostName)
  properties.put("bootstrap.servers", "localhost:19092")
  properties.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer")
  properties.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer")
  properties.put("acks", "1")

  private val producer = KafkaProducer[String, String](properties);
  sys.addShutdownHook(producer.close())

  def push(value: Sample): Unit =
    producer.send(new ProducerRecord[String, String]("jfr-allocation-topic", Jackson.toJson(value))).get()
