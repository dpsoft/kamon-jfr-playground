import com.typesafe.config.Config
import jdk.jfr.Configuration
import jdk.jfr.consumer.{EventStream, MetadataEvent, RecordedEvent, RecordingStream}
import kamon.Kamon
import kamon.metric.PeriodSnapshot
import kamon.module.MetricReporter
import metrics.jvm.{ClassLoading, GarbageCollection, Safepoint, Threads}
import metrics.os.{Cpu, Memory, Network}
import profiler.ObjectAllocation

import java.util.Map as JMap
import java.time.Duration
import java.util.function.Consumer
import scala.collection.concurrent.TrieMap
import scala.util.Using
import java.time.Instant
import java.util.concurrent.atomic.AtomicBoolean
import scala.jdk.CollectionConverters.*

object KamonJfr {
  @volatile var jfrConfig: Map[String, String] = jfrConfigFrom(Kamon.config())

  def jfrConfigFrom(config:Config): Map[String, String] =
    config
      .getConfig("kamon.jfr")
      .getStringList("config")
      .asScala.map(_.split("="))
      .map(a => a(0) -> a(1))
      .toMap

  @main def main(): Unit = {
    Kamon.init()
    val configuration = KamonJfr.jfrConfig.asJava

    startJfrConsumer(configuration)
  }

  def onEvent(event: RecordedEvent): Unit = {
    event.getEventType.getName match {
      case "jdk.CPULoad" => Cpu.onCPULoad(event)
      case "jdk.PhysicalMemory" => Memory.onPhysicalMemory(event)
      case "jdk.GarbageCollection" => GarbageCollection.onGarbageCollection(event)
      case "jdk.JavaThreadStatistics" => Threads.onJavaThreadStatistics(event)
      case "jdk.SafepointBegin" => Safepoint.onSafepointBegin(event)
      case "jdk.SafepointEnd" => Safepoint.onSafepointEnd(event)
      case "jdk.NetworkUtilization" => Network.onNetworkUtilization(event)
      case "jdk.ClassLoadingStatistics" => ClassLoading.onClassLoadingStatistics(event)
      case "jdk.ObjectAllocationSample" => ObjectAllocation.onAllocationSample(event)
      case other => println(event)
    }
  }

    def onMetadata(metadata: MetadataEvent): Unit = {
      metadata.getEventTypes.asScala.filterNot(_.isEnabled).foreach(x => println(x.getName))
    }

    def startJfrConsumer(settings: JMap[String, String]): Unit =
      val consumer = Thread(() => {
        Using.resource(RecordingStream()) { rs =>
          rs.setSettings(settings)
//          rs.onMetadata(metadata => onMetadata(metadata))
          rs.onEvent(e => onEvent(e))
          rs.start()
        }
      })

      consumer.setName("jfr-consumer")
      consumer.setDaemon(true)
      consumer.start()
  }
