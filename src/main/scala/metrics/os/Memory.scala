package metrics.os

import jdk.jfr.consumer.RecordedEvent
import kamon.Kamon
import kamon.metric.{InstrumentGroup, MeasurementUnit}
import kamon.tag.TagSet

object Memory {
  val MemoryUsed = Kamon.gauge(name = "os.memory.used", description = "Tracks the amount of used memory", unit = MeasurementUnit.information.bytes)
  val MemoryFree = Kamon.gauge(name = "os.memory.free", description = "Tracks the amount of free memory", unit = MeasurementUnit.information.bytes)
  val MemoryTotal = Kamon.gauge(name = "os.memory.total", description = "Tracks the total memory available", unit = MeasurementUnit.information.bytes)

  class MemoryInstruments(tags: TagSet) extends InstrumentGroup(tags) {
    val used = register(MemoryUsed)
    val total = register(MemoryTotal)
    val free = register(MemoryFree)
  }

  val memoryInstruments = MemoryInstruments(TagSet.of("component", "os"))

  def onPhysicalMemory(event: RecordedEvent): Unit =
    val used = event.getDouble("usedSize")
    val total = event.getDouble("totalSize")
    val free = total - used //???

    memoryInstruments.used.update(used)
    memoryInstruments.total.update(total)
    memoryInstruments.free.update(free)
}
