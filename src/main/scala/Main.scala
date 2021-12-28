import com.typesafe.config.Config
import jdk.jfr.Configuration
import jdk.jfr.consumer.{EventStream, RecordedEvent, RecordingStream, MetadataEvent}
import kamon.Kamon
import kamon.metric.PeriodSnapshot
import kamon.module.MetricReporter

import java.time.Duration
import java.util.function.Consumer
import scala.collection.concurrent.TrieMap
import scala.util.Using
import java.time.Instant
import scala.jdk.CollectionConverters._


object KamonJfr {
  @volatile var jfrConfig = jfrConfigFrom(Kamon.config());

  def jfrConfigFrom(config:Config): Map[String, String] = {
    config
    .getConfig("kamon.jfr")
    .getStringList("config")
    .asScala.map(_.split("="))
    .map(a => a(0) -> a(1))
    .toMap
  }
}

@main def hello: Unit = {
//  val c = Configuration.getConfiguration("default")
//  println(c.getSettings())

Kamon.init()

 Kamon.registerModule("Metrics", new MetricReporter {
   override def reportPeriodSnapshot(snapshot: PeriodSnapshot): Unit = {
     println(snapshot)
   }

   override def stop(): Unit = {}

   override def reconfigure(newConfig: Config): Unit = {}
 })
 
 val configuration = KamonJfr.jfrConfig

 Using.resource(RecordingStream()) { es =>  
   es.setSettings(configuration.asJava)
  //  es.onMetadata(metadata => onMetadata(metadata))
   es.onEvent(e => onEvent(e))
   // start and block
   es.start
 }

  def onMetadata(metadata: MetadataEvent):Unit = {
    metadata.getEventTypes.asScala.filterNot(_.isEnabled).foreach(x => println(x.getName))
  }

  def onEvent(event: RecordedEvent):Unit = {
    event.getEventType.getName match {
      case "jdk.CPULoad" => CpuHandler.onCPULoad(event)
      // case "jdk.JavaMonitorEnter" => JavaMonitorHandler.onMonitorEnter(event)
      // case "jdk.GarbageCollection" => GCHandler.onGCRun(event)
      // case "jdk.JavaThreadStatistics" => Threads.onJavaThreadStatistics(event)
      // case "jdk.SafepointBegin" => SafePointHandler.onSafepointBegin(event)
      // case "jdk.SafepointEnd" => SafePointHandler.onSafepointEnd(event)
      // case "jdk.ExecutionSample" => println("====>>>> ExecutionSample"+ event)
      // case "jdk.ObjectAllocationSample" => println("====>>>> allocationSample"+ event)
      // case "jdk.NetworkUtilization" => println("====>>>> jdk.NetworkUtilization"+ event)
      case other => println(event)
    }
  }
}



// val safepointBegin = TrieMap.empty[Long, Instant]

//  
//  System.gc()



////https://github.com/DataDog/dd-trace-java/blob/master/dd-java-agent/agent-profiling/profiling-controller-openjdk/src/main/java/com/datadog/profiling/controller/openjdk/OpenJdkController.java
//

 //  es.enable("jdk.CPULoad").withPeriod(duration)
  //  es.enable("jdk.YoungGarbageCollection").withoutThreshold
  //  es.enable("jdk.OldGarbageCollection").withoutThreshold
  //  es.enable("jdk.GCHeapSummary").withPeriod(duration)
  //  es.enable("jdk.PhysicalMemory").withPeriod(duration)
  //  es.enable("jdk.GCConfiguration").withPeriod(duration)
  //  es.enable("jdk.SafepointBegin")
  //  es.enable("jdk.SafepointEnd")
  //  es.enable("jdk.ObjectAllocationSample").`with`("throttle", "150/s")
  //  es.enable("jdk.ExecutionSample").withPeriod(Duration.ofMillis(10)).withStackTrace
  //  es.enable("jdk.JavaThreadStatistics").withPeriod(duration)
  //  es.enable("jdk.ClassLoadingStatistics").withPeriod(duration)
  //  es.enable("jdk.Compilation").withoutThreshold
  //  es.enable("jdk.GCHeapConfiguration").withPeriod(duration)
  //  es.enable("jdk.Flush").withoutThreshold