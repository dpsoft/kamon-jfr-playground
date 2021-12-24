import com.typesafe.config.Config
import jdk.jfr.Configuration
import jdk.jfr.consumer.{EventStream, RecordedEvent, RecordingStream}
import kamon.Kamon
import kamon.metric.PeriodSnapshot
import kamon.module.MetricReporter

import java.time.Duration
import java.util.function.Consumer
import scala.collection.concurrent.TrieMap
import scala.util.Using
import java.time.Instant


@main def hello: Unit = {

 val safepointBegin = TrieMap.empty[Long, Instant]

 Kamon.counter("Pepe").withoutTags().increment()

 Kamon.init()
 Kamon.registerModule("Metrics", new MetricReporter {
   override def reportPeriodSnapshot(snapshot: PeriodSnapshot): Unit = {
     println(snapshot)
   }

   override def stop(): Unit = {}

   override def reconfigure(newConfig: Config): Unit = {}
 })
 
 System.gc()

 val configuration = Configuration.getConfiguration("default")
 println(configuration.getSettings())

 Using.resource(RecordingStream(configuration)) { es =>
//

   val duration = Duration.ofSeconds(1)
   es.enable("jdk.CPULoad").withPeriod(duration)
   es.enable("jdk.YoungGarbageCollection").withoutThreshold
   es.enable("jdk.OldGarbageCollection").withoutThreshold
   es.enable("jdk.GCHeapSummary").withPeriod(duration)
   es.enable("jdk.PhysicalMemory").withPeriod(duration)
   es.enable("jdk.GCConfiguration").withPeriod(duration)
   es.enable("jdk.SafepointBegin")
   es.enable("jdk.SafepointEnd")
   es.enable("jdk.ObjectAllocationSample").`with`("throttle", "150/s")
   es.enable("jdk.ExecutionSample").withPeriod(Duration.ofMillis(10)).withStackTrace
   es.enable("jdk.JavaThreadStatistics").withPeriod(duration)
   es.enable("jdk.ClassLoadingStatistics").withPeriod(duration)
   es.enable("jdk.Compilation").withoutThreshold
   es.enable("jdk.GCHeapConfiguration").withPeriod(duration)
   es.enable("jdk.Flush").withoutThreshold

   // register event handlers
   es.onEvent("jdk.CPULoad", onCPULoad)
   es.onEvent("jdk.JavaThreadStatistics", onJavaThreadStatistics)
   es.onEvent("jdk.SafepointBegin", onSafepointBegin)
   es.onEvent("jdk.SafepointEnd", onSafepointEnd)

   es.onEvent("jdk.JavaMonitorEnter", event => Kamon.gauge("jdk.java-mohitor-enter.monitor-class").withoutTags().update(event.getFloat("monitorClass")))

   // register event handlers
   es.onEvent("jdk.GarbageCollection", System.out.println)
   es.onEvent("jdk.CPULoad", System.out.println)
   es.onEvent("jdk.JVMInformation", System.out.println)

   // start and block
   es.start
 }

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
    //  println(s"safepoint-time: $nanos")
   }
 }

 def onJavaThreadStatistics(event:RecordedEvent) = {
  // println(s"Thread-count: ${event.getDouble("activeCount")}")
 }

 def onCPULoad(event:RecordedEvent) = {
   Kamon.gauge("cpu-load.machine-total").withoutTags().update(event.getDouble("machineTotal"))
 }

 for(i <- 1 to 10000) { println(i)}
}







////https://github.com/DataDog/dd-trace-java/blob/master/dd-java-agent/agent-profiling/profiling-controller-openjdk/src/main/java/com/datadog/profiling/controller/openjdk/OpenJdkController.java
//