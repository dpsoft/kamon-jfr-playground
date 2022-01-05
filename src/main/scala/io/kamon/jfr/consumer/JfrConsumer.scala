package io.kamon.jfr.consumer

import io.kamon.jfr.metrics.container.{ContainerCpu, ContainerMemory}
import io.kamon.jfr.metrics.jvm.{ClassLoading, GarbageCollection, Safepoint, Threads}
import io.kamon.jfr.metrics.os.{Cpu, Memory, Network}
import io.kamon.jfr.profiler.Profiler
import jdk.jfr.consumer.{MetadataEvent, RecordedEvent, RecordingStream}
import org.slf4j.LoggerFactory

import scala.jdk.CollectionConverters.*
import java.util.Map as JMap
import scala.util.Using

final class JfrConsumer(settings: JMap[String, String]) extends Thread {

  private val log = LoggerFactory.getLogger(classOf[JfrConsumer])

  @volatile private var doRun = true

  override def run(): Unit =
    while (doRun) {
      Using.resource(RecordingStream()) { rs =>
        rs.setSettings(settings)
        rs.onEvent(e => onEvent(e))
        rs.start()
      }
    }
  
  def terminate(): Unit =
    doRun = false

  private def onEvent(event: RecordedEvent): Unit =
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
      case "jdk.ObjectAllocationSample" => Profiler.onAllocationSample(event)
      case other => log.info(s"Event not registered: $event")
    }
}