package profiler

import jdk.jfr.consumer.RecordedEvent
import profiler.parser.MethodSignatureParser
import scala.jdk.CollectionConverters.*

object ObjectAllocation {

  def onAllocationSample(event: RecordedEvent): Unit = {
    val clazz = event.getClass("objectClass")
    val weight = event.getLong("weight")
    println(s"${clazz.getName} weight: $weight")

    Option(event.getStackTrace).foreach { stackTrace =>
      stackTrace
        .getFrames
        .asScala
        .filterNot(f => !f.isJavaFrame || f.getMethod.isHidden)
        .foreach { rf =>
          val method = rf.getMethod
          val methodDescriptor = s"${method.getType.getName}.${method.getName}${method.getDescriptor}"
          val methodSignature = MethodSignatureParser.methodParameters(methodDescriptor)
          val ppMethod = s"${methodSignature}:${rf.getLineNumber}"

          println("\t" + ppMethod)
        }
    }
  }
}
