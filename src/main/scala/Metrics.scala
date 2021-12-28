import scala.collection.concurrent.TrieMap
import java.time.Instant
import jdk.jfr.consumer.{EventStream, RecordedEvent, RecordingStream}
import java.time.Duration
import kamon.Kamon
import kamon.tag.TagSet
import kamon.metric.InstrumentGroup
import kamon.metric.MeasurementUnit

enum JfrMetrics(name:String) {
    case SafePoint extends JfrMetrics("")
    case CPU extends JfrMetrics("")
}

object SafePointHandler {
    private val safepointBegin = TrieMap.empty[Long, Instant]

    def onSafepointBegin(event:RecordedEvent) = {
        safepointBegin.put(event.getValue("safepointId"), event.getEndTime());
    }

    def onSafepointEnd(event:RecordedEvent) = {
        val id = event.getValue("safepointId").asInstanceOf[Long];
        val begin = safepointBegin.get(id).getOrElse(null);

        if (begin != null) {
            val nanos = Duration.between(begin, event.getEndTime()).toNanos()
            safepointBegin.remove(id);
            Kamon.histogram("safepoint-time").withoutTags().record(nanos)
            println(s"safepoint-time: $nanos")
        }
    }
}

object CpuHandler {
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

object GCHandler {
    def onGCRun(event:RecordedEvent) = {
        System.out.println(event)
    }
}

object JavaMonitorHandler {
    def onMonitorEnter(event:RecordedEvent) = {
        // Kamon.gauge("jdk.java-mohitor-enter.monitor-class").withoutTags().update(event.getFloat("monitorClass"))
        System.out.println(event)
    }
}

object Threads {
     def onJavaThreadStatistics(event:RecordedEvent) = {
        println(s"Thread info: ${event}")

        println(s"Thread-count: ${event.getDouble("activeCount")}")
    }
}
