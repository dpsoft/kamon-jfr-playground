package io.kamon.jfr.metrics.jvm

import jdk.jfr.consumer.RecordedEvent
import kamon.Kamon
import kamon.metric.{Gauge, Histogram, InstrumentGroup, MeasurementUnit}
import kamon.tag.TagSet
import GarbageCollection.GarbageCollectionInstruments.GCInstruments

import scala.collection.concurrent.TrieMap

object GarbageCollection {
  private val GC = Kamon.histogram(
    name = "jvm.gc",
    description = "Tracks the distribution of GC events duration",
    unit = MeasurementUnit.time.milliseconds
  )

  private val GcPauses = Kamon.histogram(
    name = "jvm.gc.pauses",
    description = "Sum of all the times in which Java execution was paused during the garbage collection",
    unit = MeasurementUnit.time.milliseconds
  )

  private val GcLongestPause = Kamon.gauge(
    name = "jvm.gc.longest.pause",
    description = "Longest individual pause during the garbage collection"
  )

  class GarbageCollectionInstruments(tags: TagSet) extends InstrumentGroup(tags) {
    private val collectorCache = TrieMap.empty[String, GCInstruments]

    def instruments(collectorName: String): GCInstruments =
      collectorCache.getOrElseUpdate(collectorName, {
        val collectorTags = TagSet.builder()
          .add("collector", collectorName)
          .build()

        GCInstruments(
          register(GC, collectorTags),
          register(GcPauses, collectorTags),
          register(GcLongestPause, collectorTags)
        )
      })
  }

  object GarbageCollectionInstruments:
    case class GCInstruments(gcTime: Histogram, gcPauses: Histogram, gcLongestPause: Gauge)

  private val gcInstruments = GarbageCollectionInstruments(TagSet.of("component", "jvm"))

  def onGarbageCollection(event: RecordedEvent): Unit = {
    val instruments = gcInstruments.instruments(event.getString("name"))

    instruments.gcTime.record(event.getLong("duration"))
    instruments.gcPauses.record(event.getLong("sumOfPauses"))
    instruments.gcLongestPause.update(event.getLong("longestPause"))
  }
}
