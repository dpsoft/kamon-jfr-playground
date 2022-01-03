package io.kamon.jfr.profiler

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import io.kamon.jfr.profiler.kafka.KafkaPusher
import io.kamon.jfr.profiler.parser.MethodSignatureParser
import io.kamon.jfr.profiler.serde.Jackson
import jdk.jfr.consumer.{RecordedEvent, RecordedFrame}

import java.lang.reflect.Modifier
import scala.jdk.CollectionConverters.*

object Profiler {
  case class Sample(startTime: Long,
                    clazz: String,
                    weight: Long,
                    threadName: String,
                    stackTrace: List[String])

  def onAllocationSample(event: RecordedEvent): Unit = {
    val startTime = event.getLong("startTime")
    val eventThread = event.getThread("eventThread").getJavaName
    val clazz = event.getClass("objectClass").getName
    val weight = event.getLong("weight")

    val stackTraces = Option(event.getStackTrace).map { stackTrace =>
      stackTrace
        .getFrames
        .asScala
        .filterNot(f => !f.isJavaFrame || f.getMethod.isHidden)
        .flatMap(rf => parseFrame(rf))
    }

    stackTraces
      .map(stack => Sample(startTime, clazz, weight, eventThread, stack.toList))
      .foreach(KafkaPusher.push)
  }

  private def parseFrame(rf: RecordedFrame): Option[String] =
    val method = rf.getMethod
    val methodDescriptor = s"${method.getType.getName}.${method.getName}${method.getDescriptor}"

    MethodSignatureParser
      .methodSignature(methodDescriptor)
      .map(methodSignature => s"$methodSignature:${rf.getLineNumber}")
}
