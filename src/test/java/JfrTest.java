import org.junit.jupiter.api.Test;
import org.moditect.jfrunit.*;

import java.time.Duration;
import java.util.stream.Collectors;

import static org.moditect.jfrunit.JfrEventsAssert.*;
import static org.moditect.jfrunit.ExpectedEvent.*;


@JfrEventTest
public class JfrTest {

    public JfrEvents jfrEvents = new JfrEvents();

    @Test
    @EnableEvent("jdk.GarbageCollection")
    @EnableEvent("jdk.ThreadSleep")
    public void shouldHaveGcAndSleepEvents() throws Exception {
        System.gc();
        Thread.sleep(1000);

        jfrEvents.awaitEvents();

        System.out.println(jfrEvents.events().collect(Collectors.toList()));
        assertThat(jfrEvents).contains(event("jdk.GarbageCollection"));
        assertThat(jfrEvents).contains(
                event("jdk.ThreadSleep").with("time", Duration.ofSeconds(1)));
    }
}