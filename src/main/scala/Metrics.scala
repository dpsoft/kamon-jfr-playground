import scala.collection.concurrent.TrieMap
import java.time.Instant
import jdk.jfr.consumer.{EventStream, RecordedEvent, RecordingStream}

import java.time.Duration
import kamon.Kamon
import kamon.tag.TagSet
import kamon.metric.{Gauge, Histogram, InstrumentGroup, MeasurementUnit}

import scala.collection.mutable


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

object MemoryHandler {
    val MemoryUsed = Kamon.gauge(
        name = "os.memory.used",
        description = "Tracks the amount of used memory",
        unit = MeasurementUnit.information.bytes)

    val MemoryFree = Kamon.gauge(
        name = "os.memory.free",
        description = "Tracks the amount of free memory",
        unit = MeasurementUnit.information.bytes)

    val MemoryTotal = Kamon.gauge(
        name = "os.memory.total",
        description = "Tracks the total memory available",
        unit = MeasurementUnit.information.bytes)    

    class MemoryInstruments(tags: TagSet) extends InstrumentGroup(tags) {
        val used = register(MemoryUsed)
        val total = register(MemoryTotal)
        val free = register(MemoryFree)
    }

    val memoryInstruments = MemoryInstruments(TagSet.of("component", "host"))

    def onPhysicalMemory(event: RecordedEvent): Unit =
        val used = event.getDouble("usedSize")
        val total = event.getDouble("totalSize")
        val free = total - used //???

        memoryInstruments.used.update(used)
        memoryInstruments.total.update(total)
        memoryInstruments.free.update(free)
}

object GCHandler {
    val GC = Kamon.histogram(
        name = "jvm.gc",
        description = "Tracks the distribution of GC events duration",
        unit = MeasurementUnit.time.milliseconds
    )

    val GcPauses = Kamon.histogram(
        name = "jvm.gc.pauses",
        description = "Sum of all the times in which Java execution was paused during the garbage collection",
        unit = MeasurementUnit.time.milliseconds
    )

    val GcLongestPause = Kamon.gauge(
        name = "jvm.gc.longest-pause",
        description = "Longest individual pause during the garbage collection"
    )

    class GarbageCollectionInstruments(tags: TagSet) extends InstrumentGroup(tags) {
        private val collectorCache = TrieMap.empty[String, (Histogram, Histogram, Gauge)]

        def instruments(collectorName: String): (Histogram, Histogram, Gauge) =
            collectorCache.getOrElseUpdate(collectorName, {
                val collectorTags = TagSet.builder()
                  .add("collector", collectorName)
                  .build()

                (register(GC, collectorTags), register(GcPauses, collectorTags),  register(GcLongestPause, collectorTags))
            })
    }

    val gcInstruments: GarbageCollectionInstruments = GarbageCollectionInstruments(TagSet.of("component", "jvm"))

    def onGarbageCollection(event:RecordedEvent): Unit = {
        val (gcTime, pauses, longest) = gcInstruments.instruments(event.getString("name"))

        gcTime.record(event.getLong("duration"))
        pauses.record(event.getLong("sumOfPauses"))
        longest.update(event.getLong("longestPause"))
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
