package io.kamon.jfr

import com.typesafe.config.Config
import io.kamon.jfr.consumer.JfrConsumer
import io.kamon.jfr.metrics.container.{ContainerCpu, ContainerMemory}
import io.kamon.jfr.metrics.jvm.{ClassLoading, GarbageCollection, Safepoint, Threads}
import io.kamon.jfr.metrics.os.{Cpu, Memory, Network}
import io.kamon.jfr.profiler.Profiler
import jdk.jfr.Configuration
import jdk.jfr.consumer.{EventStream, MetadataEvent, RecordedEvent, RecordingStream}
import kamon.Kamon
import kamon.metric.PeriodSnapshot
import kamon.module.{MetricReporter, Module, ModuleFactory, ScheduledAction}

import java.nio.file.Path
import java.time.{Duration, Instant}
import java.util.Map as JMap
import java.util.concurrent.atomic.AtomicBoolean
import java.util.function.Consumer
import scala.collection.concurrent.TrieMap
import scala.concurrent.ExecutionContext
import scala.jdk.CollectionConverters.*
import scala.util.Using

class KamonJfr(ec: ExecutionContext) extends ScheduledAction {
  @volatile var jfrConfig: Map[String, String] = jfrConfigFrom(Kamon.config())

  private val jfrConsumer = startJfrConsumer(jfrConfig.asJava)

  def startJfrConsumer(settings: JMap[String, String]): JfrConsumer = {
    val jfrConsumer = new JfrConsumer(settings)
    jfrConsumer.setName("jfr-consumer")
    jfrConsumer.setDaemon(true)
    jfrConsumer.start()
    jfrConsumer
  }

  private def jfrConfigFrom(config: Config): Map[String, String] = {
    config
      .getConfig("kamon.jfr")
      .getStringList("config")
      .asScala.map(_.split("="))
      .map(a => a(0) -> a(1))
      .toMap
  }

  override def stop(): Unit =
    jfrConsumer.terminate()

  override def run(): Unit = {}
  override def reconfigure(newConfig: Config): Unit = {}
}

object KamonJfr:
  class Factory extends ModuleFactory:
    override def create(settings: ModuleFactory.Settings): Module =
      new KamonJfr(settings.executionContext)
