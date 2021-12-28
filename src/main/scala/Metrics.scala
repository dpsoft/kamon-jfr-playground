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

object JavaMonitorHandler {


    def onMonitorEnter(event:RecordedEvent) = {

        // Kamon.gauge("jdk.java-mohitor-enter.monitor-class").withoutTags().update(event.getFloat("monitorClass"))
        System.out.println(event)
    }
}
