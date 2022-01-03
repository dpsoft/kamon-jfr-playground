package io.kamon.jfr.profiler

import com.fasterxml.jackson.databind.JsonSerializer
import io.kamon.jfr.profiler.Profiler.Sample
import io.kamon.jfr.profiler.serde.Jackson
import org.apache.kafka.clients.producer.{KafkaProducer, ProducerConfig, ProducerRecord}
import org.apache.kafka.common.serialization.StringSerializer

import java.net.InetAddress
import java.util.Properties

object KafkaPusher:
  private val config = Properties()
  config.put("client.id", InetAddress.getLocalHost.getHostName)
  config.put("bootstrap.servers", "localhost:9092")
  config.put("key.serializer","org.apache.kafka.common.serialization.StringSerializer")
  config.put("value.serializer","org.apache.kafka.common.serialization.StringSerializer")
  config.put("acks", "1")

  private val producer = KafkaProducer[String, String](config);
  sys.addShutdownHook(producer.close())

  def push(value: Sample): Unit =
    producer.send(new ProducerRecord[String, String]("jfr-allocation-topic", Jackson.toJson(value))).get()

