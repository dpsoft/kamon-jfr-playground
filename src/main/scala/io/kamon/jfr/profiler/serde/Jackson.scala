package io.kamon.jfr.profiler.serde

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.scala.DefaultScalaModule

object Jackson:
  private val mapper = ObjectMapper()
  mapper.registerModule(DefaultScalaModule)

  def toJson(value: Any): String =
    mapper.writeValueAsString(value)

