package app

import kamon.Kamon

import java.net.InetSocketAddress
import uzhttp.Response
import uzhttp.server.Server
import zio.blocking.Blocking
import zio.clock.Clock
import zio.system.*
import zio.{App, ZIO}

import scala.util.Try

object WebApp extends App {

  sealed trait PortError
  case class InvalidPortValue(port: String) extends PortError
  case class SecurityError(e: SecurityException) extends PortError

  val zioPort: ZIO[System, PortError, Char] = {
    def convertToCharOrFail(s: String): Either[InvalidPortValue, Char] = {
      for {
        i <- Try(s.toInt)
        if i >= Char.MinValue
        if i <= Char.MaxValue
      } yield i.toChar
    }.toEither.left.map(_ => InvalidPortValue(s))

    env("PORT").mapError(SecurityError.apply).flatMap { maybePort =>
      ZIO.fromEither {
        maybePort.fold[Either[InvalidPortValue, Char]](Right(8080.toChar))(convertToCharOrFail)
      }
    }
  }

  override def run(args:  List[String]): ZIO[zio.ZEnv, Nothing, zio.ExitCode] = {
    Kamon.init()

    def server(port: Char): ZIO[Blocking with Clock, Throwable, Nothing] = {
      Server.builder(new InetSocketAddress(port)).handleAll { _ =>
        ZIO.succeed(Response.plain("hello, world"))
      }.serve.useForever
    }

    val appLogic = for {
      port <- zioPort.mapError {
        case InvalidPortValue(s) => new Error(s"The specified PORT '$s' was invalid")
        case SecurityError(e) => e
      }
      _ <- server(port)
    } yield ()

    appLogic.exitCode
  }
}
