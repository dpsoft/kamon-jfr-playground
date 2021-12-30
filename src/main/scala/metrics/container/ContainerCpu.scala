package metrics.container

import jdk.jfr.consumer.RecordedEvent
import kamon.Kamon
import kamon.metric.{InstrumentGroup, MeasurementUnit}
import kamon.tag.TagSet

object ContainerCpu {
  val CpuUsage = Kamon.gauge(name = "container.cpu.usage",
    description = "Samples the container CPU time using JFR Events",
    unit = MeasurementUnit.percentage)

  class ContainerCpuInstruments(tags: TagSet) extends InstrumentGroup(tags):
    val user = register(CpuUsage, "mode", "user")
    val system = register(CpuUsage, "mode", "system")
    val combined = register(CpuUsage, "mode", "combined")

  val containerCpuInstruments = ContainerCpuInstruments(TagSet.of("component", "container"))

  def onContainerCPULoad(event: RecordedEvent): Unit =
    containerCpuInstruments.user.update(event.getLong("cpuUserTime"))
    containerCpuInstruments.system.update(event.getLong("cpuSystemTime"))
    containerCpuInstruments.combined.update(event.getLong("cpuTime"))
}
