import scala.collection.concurrent.TrieMap
import java.time.Instant
import jdk.jfr.consumer.{EventStream, RecordedEvent, RecordingStream}

import java.time.Duration
import kamon.Kamon
import kamon.tag.TagSet
import kamon.metric.{Gauge, Histogram, InstrumentGroup, MeasurementUnit}

import scala.collection.mutable



object JavaMonitorHandler {


    def onMonitorEnter(event:RecordedEvent) = {

        // Kamon.gauge("jdk.java-mohitor-enter.monitor-class").withoutTags().update(event.getFloat("monitorClass"))
        System.out.println(event)
    }
}
