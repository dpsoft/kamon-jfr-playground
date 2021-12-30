package io.kamon.jfr.metrics.os

import jdk.jfr.consumer.RecordedEvent
import kamon.Kamon
import kamon.metric.{Counter, InstrumentGroup, MeasurementUnit}
import kamon.tag.TagSet
import Network.NetworkInstruments.InterfaceInstruments

import scala.collection.concurrent.TrieMap

object Network {
  private val NetworkDataRead = Kamon.counter(name = "os.network.data.read", description = "Number of incoming bits per second", unit = MeasurementUnit.information.bytes)
  private val NetworkDataWrite = Kamon.counter(name = "os.network.data.write", description = "Number of outgoing bits per second", unit = MeasurementUnit.information.bytes)

  class NetworkInstruments(tags: TagSet) extends InstrumentGroup(tags) {
    private val interfaceCache = TrieMap.empty[String, InterfaceInstruments]

    private val read = register(NetworkDataRead)
    private val write = register(NetworkDataWrite)

    def interfaceInstruments(interfaceName: String): InterfaceInstruments =
      interfaceCache.getOrElseUpdate(interfaceName, {
        val interface = TagSet.of("interface", interfaceName)

        InterfaceInstruments(
          DiffCounter(register(NetworkDataRead, interface)),
          DiffCounter(register(NetworkDataWrite, interface)),
        )
      })
  }

  object NetworkInstruments:
    case class InterfaceInstruments(readBytes: DiffCounter, writeBytes: DiffCounter)

  private val networkInstruments = NetworkInstruments(TagSet.of("component", "os"))

  def onNetworkUtilization(event: RecordedEvent): Unit =
    val networkInterface = event.getString("networkInterface")
    val instruments = networkInstruments.interfaceInstruments(networkInterface)

    instruments.readBytes.diff(event.getLong("readRate"))
    instruments.writeBytes.diff(event.getLong("writeRate"))

  /**
   * A modified Counter that keeps track of a monotonically increasing value and only records the difference between
   * the current and previous value on the target counter.
   */
  final case class DiffCounter(counter: Counter) {
    private var _previous = 0L

    def diff(current: Long): Unit = {
      if (_previous > 0L) {
        val delta = current - _previous
        if (delta > 0)
          counter.increment(delta)

      }
      _previous = current
    }
  }
}
