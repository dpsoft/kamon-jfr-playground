import com.typesafe.config.Config
import jdk.jfr.Configuration
import jdk.jfr.consumer.{EventStream, MetadataEvent, RecordedEvent, RecordingStream}
import kamon.Kamon
import kamon.metric.PeriodSnapshot
import kamon.module.MetricReporter
import metrics.{Cpu, GarbageCollection, Memory, Threads}

import java.time.Duration
import java.util.function.Consumer
import scala.collection.concurrent.TrieMap
import scala.util.Using
import java.time.Instant
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

    Kamon.registerModule("jfr-metrics", new MetricReporter {
      override def reportPeriodSnapshot(snapshot: PeriodSnapshot): Unit = { println(snapshot) }
      override def stop(): Unit = {}
      override def reconfigure(newConfig: Config): Unit = {}
    })

    val configuration = KamonJfr.jfrConfig

    Thread(() => {
      Using.resource(RecordingStream()) { rs =>
        rs.setSettings(configuration.asJava)
        rs.onMetadata(metadata => onMetadata(metadata))
        rs.onEvent(e => onEvent(e))

        rs.start()
      }
    }).start()

    def onMetadata(metadata: MetadataEvent): Unit = {
      metadata.getEventTypes.asScala.filterNot(_.isEnabled).foreach(x => println(x.getName))
    }

    def onEvent(event: RecordedEvent): Unit = {
      event.getEventType.getName match {
        case "jdk.CPULoad" => Cpu.onCPULoad(event)
        case "jdk.PhysicalMemory" => Memory.onPhysicalMemory(event)
        case "jdk.GarbageCollection" => GarbageCollection.onGarbageCollection(event)
        case "jdk.JavaThreadStatistics" => Threads.onJavaThreadStatistics(event)

        // case "jdk.JavaMonitorEnter" => JavaMonitorHandler.onMonitorEnter(event)
//         case "jdk.SafepointBegin" => SafePointHandler.onSafepointBegin(event)
//         case "jdk.SafepointEnd" => SafePointHandler.onSafepointEnd(event)
        // case "jdk.ExecutionSample" => println("====>>>> ExecutionSample"+ event)
        // case "jdk.ObjectAllocationSample" => println("====>>>> allocationSample"+ event)
        // case "jdk.NetworkUtilization" => println("====>>>> jdk.NetworkUtilization"+ event)
        case other => println(event)
      }
    }

    System.gc()
    System.gc()

    Thread.sleep(10000000)
  }
}
////https://github.com/DataDog/dd-trace-java/blob/master/dd-java-agent/agent-profiling/profiling-controller-openjdk/src/main/java/com/datadog/profiling/controller/openjdk/OpenJdkController.java