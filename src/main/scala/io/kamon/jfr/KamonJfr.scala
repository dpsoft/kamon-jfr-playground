package io.kamon.jfr

import com.typesafe.config.Config
import io.kamon.jfr.metrics.container.{ContainerCpu, ContainerMemory}
import io.kamon.jfr.metrics.jvm.{ClassLoading, GarbageCollection, Safepoint, Threads}
import io.kamon.jfr.metrics.os.{Cpu, Memory, Network}
import io.kamon.jfr.profiler.ObjectAllocation
import jdk.jfr.Configuration
import jdk.jfr.consumer.{EventStream, MetadataEvent, RecordedEvent, RecordingStream}
import kamon.Kamon
import kamon.metric.PeriodSnapshot
import kamon.module.{MetricReporter, Module, ModuleFactory, ScheduledAction}

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

  def onMetadata(metadata: MetadataEvent): Unit = {
    metadata.getEventTypes.asScala.filterNot(_.isEnabled).foreach(x => println(x.getName))
  }

  final class JfrConsumer(settings: JMap[String, String]) extends Thread {
    @volatile private var doRun = true

    override def run(): Unit = {
      while (doRun) {
        Using.resource(RecordingStream()) { rs =>
          rs.setSettings(settings)
          //          rs.onMetadata(metadata => onMetadata(metadata))
          rs.onEvent(e => onEvent(e))
          rs.start()
        }
      }
    }

    private def onEvent(event: RecordedEvent): Unit = {
      event.getEventType.getName match {
        case "jdk.CPULoad" => Cpu.onCPULoad(event)
        case "jdk.PhysicalMemory" => Memory.onPhysicalMemory(event)
        case "jdk.GarbageCollection" => GarbageCollection.onGarbageCollection(event)
        case "jdk.JavaThreadStatistics" => Threads.onJavaThreadStatistics(event)
        case "jdk.SafepointBegin" => Safepoint.onSafepointBegin(event)
        case "jdk.SafepointEnd" => Safepoint.onSafepointEnd(event)
        case "jdk.NetworkUtilization" => Network.onNetworkUtilization(event)
        case "jdk.ClassLoadingStatistics" => ClassLoading.onClassLoadingStatistics(event)
        case "jdk.ContainerMemoryUsage" => ContainerMemory.onContainerMemory(event)
        case "jdk.ContainerCPUUsage" => ContainerCpu.onContainerCPULoad(event)
        case "jdk.ObjectAllocationSample" => ObjectAllocation.onAllocationSample(event)
        case other => println(event)
      }
    }

    def terminate(): Unit =
      doRun = false
  }
}

object KamonJfr:
  class Factory extends ModuleFactory:
    override def create(settings: ModuleFactory.Settings): Module =
      new KamonJfr(settings.executionContext)
