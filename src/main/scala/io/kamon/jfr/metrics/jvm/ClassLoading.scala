package io.kamon.jfr.metrics.jvm

import jdk.jfr.consumer.RecordedEvent
import kamon.Kamon
import kamon.metric.InstrumentGroup
import kamon.tag.TagSet

object ClassLoading {
  private val ClassesLoaded = Kamon.gauge(name = "jvm.class-loading.loaded", description = "Total number of classes loaded")
  private val ClassesUnloaded = Kamon.gauge(name = "jvm.class-loading.unloaded", description = "Total number of classes unloaded")

  class ClassLoadingInstruments(tags: TagSet) extends InstrumentGroup(tags):
    val loaded = register(ClassesLoaded)
    val unloaded = register(ClassesUnloaded)

  private val classLoadingInstruments = ClassLoadingInstruments(TagSet.of("component", "jvm"))

  def onClassLoadingStatistics(event: RecordedEvent): Unit =
    classLoadingInstruments.loaded.update(event.getLong("loadedClassCount"))
    classLoadingInstruments.unloaded.update(event.getLong("unloadedClassCount"))
}
