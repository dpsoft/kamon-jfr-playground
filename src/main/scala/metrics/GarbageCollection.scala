package metrics

import jdk.jfr.consumer.RecordedEvent
import kamon.Kamon
import kamon.metric.{Gauge, Histogram, InstrumentGroup, MeasurementUnit}
import kamon.tag.TagSet

import scala.collection.concurrent.TrieMap

object GarbageCollection {
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