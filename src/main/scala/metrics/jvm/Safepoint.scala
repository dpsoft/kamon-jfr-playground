package metrics.jvm

import jdk.jfr.consumer.RecordedEvent
import kamon.Kamon
import kamon.metric.{InstrumentGroup, MeasurementUnit}
import kamon.tag.TagSet

import java.time.{Duration, Instant}
import scala.collection.concurrent.TrieMap

object Safepoint {
  private val safepointBegin = TrieMap.empty[Long, Instant]
  private val SafepointTime = Kamon.histogram(name = "jvm.safepoint.time", description = "Time spent in a safepoint", unit = MeasurementUnit.time.milliseconds)

  class SafepointInstruments(tags: TagSet) extends InstrumentGroup(tags):
    val time = register(SafepointTime)

  private val safepointInstruments = SafepointInstruments(TagSet.of("component", "jvm"))

  def onSafepointBegin(event:RecordedEvent): Unit =
    safepointBegin.put(event.getValue("safepointId"), event.getEndTime);

  def onSafepointEnd(event:RecordedEvent): Unit =
    val id = event.getLong("safepointId")

    safepointBegin.get(id).foreach { begin =>
      val nanos = Duration.between(begin, event.getEndTime).toNanos
      safepointBegin.remove(id)
      safepointInstruments.time.record(nanos)
    }
}
