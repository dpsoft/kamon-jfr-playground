package io.kamon.jfr.profiler.kafka

import ch.qos.logback.classic.Logger
import io.kamon.jfr.profiler.Profiler.Sample
import io.kamon.jfr.profiler.serde.Jackson
import org.apache.kafka.clients.producer.{Callback, KafkaProducer, ProducerRecord, RecordMetadata}
import org.slf4j.LoggerFactory

import java.net.InetAddress
import java.util.Properties
import scala.util.Try

object KafkaPusher:

  private val log = LoggerFactory.getLogger(KafkaPusher.getClass)

  private val properties = Properties()
  properties.put("client.id", InetAddress.getLocalHost.getHostName)
  properties.put("bootstrap.servers", "localhost:19092")
  properties.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer")
  properties.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer")
  properties.put("acks", "1")

  private val producer = KafkaProducer[String, String](properties);
  sys.addShutdownHook(producer.close())

  def push(value: Sample): Unit = {
    val record = new ProducerRecord[String, String]("jfr-allocation-topic", Jackson.toJson(value))
    
    producer.send(record, (metadata: RecordMetadata, exception: Exception) => {
      if (exception != null) log.error("Send failed for record" + record, exception)
    })
  }
