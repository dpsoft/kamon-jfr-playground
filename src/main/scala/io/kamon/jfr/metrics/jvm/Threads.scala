package io.kamon.jfr.metrics.jvm

import jdk.jfr.consumer.RecordedEvent
import kamon.Kamon
import kamon.metric.InstrumentGroup
import kamon.tag.TagSet

object Threads {
  private val ThreadsTotal = Kamon.gauge(name = "jvm.threads.total", description = "Tracks the current number of live threads on the JVM")
  private val ThreadsPeak = Kamon.gauge(name = "jvm.threads.peak", description = "Tracks the peak live thread count since the JVM started")
  private val ThreadsDaemon = Kamon.gauge(name = "jvm.threads.daemon", description = "Tracks the current number of daemon threads on the JVM")
  private val ThreadsStates = Kamon.gauge(name = "jvm.threads.states", description = "Tracks the current number of threads on each possible state")

  class ThreadsInstruments(tags: TagSet) extends InstrumentGroup(tags):
    val total = register(ThreadsTotal)
    val peak = register(ThreadsPeak)
    val daemon = register(ThreadsDaemon)

  private val threadsInstruments = ThreadsInstruments(TagSet.of("component", "jvm"))

  def onJavaThreadStatistics(event: RecordedEvent): Unit =
    threadsInstruments.total.update(event.getLong("activeCount"))
    threadsInstruments.peak.update(event.getLong("peakCount"))
    threadsInstruments.daemon.update(event.getLong("daemonCount"))
}
