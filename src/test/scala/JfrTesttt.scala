import org.junit.jupiter.api.Test
import org.moditect.jfrunit._
import java.time.Duration
import java.util.stream.Collectors
import org.moditect.jfrunit.JfrEventsAssert._
import org.moditect.jfrunit.ExpectedEvent._


@JfrEventTest
class JfrTesttt {

  var jfrEvents = new JfrEvents

  @Test
  @EnableEvent("jdk.GarbageCollection")
  @EnableEvent("jdk.ThreadSleep")
  @throws[Exception]
  def shouldHaveGcAndSleepEvents(): Unit = {
    System.gc()

    Thread.sleep(1000)

    jfrEvents.awaitEvents()

    System.out.println(jfrEvents.events.collect(Collectors.toList))
    assertThat(jfrEvents).contains(event("jdk.GarbageCollection"))
    assertThat(jfrEvents).contains(event("jdk.ThreadSleep").`with`("time", Duration.ofSeconds(1)))
  }
}
