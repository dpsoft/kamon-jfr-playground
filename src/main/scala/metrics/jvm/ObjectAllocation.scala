package metrics.jvm

import jdk.jfr.consumer.RecordedEvent

object ObjectAllocation {

  def onAllocationSample(event: RecordedEvent): Unit = {
    println(event)
  }
}
