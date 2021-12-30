package io.kamon.jfr.metrics.container

import jdk.jfr.consumer.RecordedEvent
import kamon.Kamon
import kamon.metric.{InstrumentGroup, MeasurementUnit}
import kamon.tag.TagSet

object ContainerMemory {
  val ContainerMemoryUsage = Kamon.gauge(name = "container.memory.usage", description = "Amount of physical memory, in bytes, that is currently allocated in the current container", unit = MeasurementUnit.information.bytes)
  val ContainerMemorySwap = Kamon.gauge(name = "container.memory.swap", description = "Amount of physical memory and swap space, in bytes, that is currently allocated in the current container", unit = MeasurementUnit.information.bytes)
  val ContainerMemoryFail = Kamon.gauge(name = "container.memory.fail", description = "Number of times that user memory requests in the container have exceeded the memory limit")

  class ContainerMemoryInstruments(tags: TagSet) extends InstrumentGroup(tags) {
    val usage = register(ContainerMemoryUsage)
    val swap = register(ContainerMemorySwap)
    val fail = register(ContainerMemoryFail)
  }

  val containerMemoryInstruments = ContainerMemoryInstruments(TagSet.of("component", "container"))

  def onContainerMemory(event: RecordedEvent): Unit =
    val usage = event.getLong("memoryUsage")
    val swap = event.getLong("swapMemoryUsage")
    val fail = event.getLong("memoryFailCount")

    containerMemoryInstruments.usage.update(usage)
    containerMemoryInstruments.swap.update(swap)
    containerMemoryInstruments.fail.update(fail)
}
