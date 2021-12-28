package metrics

import jdk.jfr.consumer.RecordedEvent
import kamon.Kamon
import kamon.metric.InstrumentGroup
import kamon.tag.TagSet

object Threads {
    val ThreadsTotal = Kamon.gauge(
        name = "jvm.threads.total",
        description = "Tracks the current number of live threads on the JVM"
    )

    val ThreadsPeak = Kamon.gauge(
        name = "jvm.threads.peak",
        description = "Tracks the peak live thread count since the JVM started"
    )

    val ThreadsDaemon = Kamon.gauge(
        name = "jvm.threads.daemon",
        description = "Tracks the current number of daemon threads on the JVM"
    )

    val ThreadsStates = Kamon.gauge(
        name = "jvm.threads.states",
        description = "Tracks the current number of threads on each possible state"
    )

    class ThreadsInstruments(tags: TagSet) extends InstrumentGroup(tags) {
        val total = register(ThreadsTotal)
        val peak = register(ThreadsPeak)
        val daemon = register(ThreadsDaemon)
    }

    val threadsInstruments = ThreadsInstruments(TagSet.Empty)

    def onJavaThreadStatistics(event: RecordedEvent): Unit =
        threadsInstruments.total.update(event.getLong("activeCount"))
        threadsInstruments.peak.update(event.getLong("peakCount"))
        threadsInstruments.daemon.update(event.getLong("daemonCount"))
}