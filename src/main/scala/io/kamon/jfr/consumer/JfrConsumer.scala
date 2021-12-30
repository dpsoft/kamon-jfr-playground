package io.kamon.jfr.consumer

import io.kamon.jfr.metrics.container.{ContainerCpu, ContainerMemory}
import io.kamon.jfr.metrics.jvm.{ClassLoading, GarbageCollection, Safepoint, Threads}
import io.kamon.jfr.metrics.os.{Cpu, Memory, Network}
import io.kamon.jfr.profiler.ObjectAllocation
import jdk.jfr.consumer.{MetadataEvent, RecordedEvent, RecordingStream}
import scala.jdk.CollectionConverters.*

import java.util.Map as JMap
import scala.util.Using

final class JfrConsumer(settings: JMap[String, String]) extends Thread {
  @volatile private var doRun = true

  override def run(): Unit =
    while (doRun) {
      Using.resource(RecordingStream()) { rs =>
        rs.setSettings(settings)
        //          rs.onMetadata(metadata => onMetadata(metadata))
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
      case "jdk.ObjectAllocationSample" => ObjectAllocation.onAllocationSample(event)
      case other => println(event)
    }


  private def onMetadata(metadata: MetadataEvent): Unit =
    metadata
      .getEventTypes
      .asScala
      .filterNot(_.isEnabled)
      .foreach(x => println(x.getName))
}