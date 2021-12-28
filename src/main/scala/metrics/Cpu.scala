package metrics

import jdk.jfr.consumer.RecordedEvent
import kamon.Kamon
import kamon.metric.{InstrumentGroup, MeasurementUnit}
import kamon.tag.TagSet

object Cpu {
  val CpuUsage = Kamon.gauge(
    name = "os.cpu.usage",
    description = "Samples the CPU usage percentage using JFR Events",
    unit = MeasurementUnit.percentage)

  class CpuInstruments(tags: TagSet) extends InstrumentGroup(tags):
    val user = register(CpuUsage, "mode", "user")
    val system = register(CpuUsage, "mode", "system")
    val combined = register(CpuUsage, "mode", "combined")

  val cpuInstruments = CpuInstruments(TagSet.of("component", "host"))

  def onCPULoad(event: RecordedEvent): Unit =
    cpuInstruments.user.update(event.getDouble("jvmUser"))
    cpuInstruments.system.update(event.getDouble("jvmSystem"))
    cpuInstruments.combined.update(event.getDouble("machineTotal"))
}