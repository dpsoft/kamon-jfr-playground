package profiler.parser

import java.beans.MethodDescriptor
import scala.util.parsing.combinator.RegexParsers

/**
 * taken from: https://stackoverflow.com/a/12375332/6646267
 */
class MethodSignatureParser extends RegexParsers {
  def signature: Parser[String] = methodName ~ "(" ~ parameters ~ ")" ~ result ^^ {
    case m ~ "(" ~ p ~ ")" ~ r => r + " " + m + "(" + p + ")"
  }
  def methodName: Parser[String] = regularName ~ opt(specialName) ^^ {
    case m ~ None => m
    case c ~ Some(s) => c + s
  }
  def regularName: Parser[String] = """[A-Z0-9a-z_.$]+""".r
  def specialName: Parser[String] = "<" ~> initializer <~ ">" ^^ ("<" + _ + ">")
  def initializer: Parser[String] = "init" | "clinit"
  def result: Parser[String] = void | nonVoid
  def parameters: Parser[String] = rep(nonVoid) ^^ {
    case Nil => ""
    case p => p.reduceLeft(_ + ", " + _)
  }
  def nonVoid: Parser[String] = primitive | reference | array
  def primitive: Parser[String] = boolean | char | byte | short | int | float | long | double
  def void: Parser[String] = "V" ^^ (_ => "void")
  def boolean: Parser[String] = "Z" ^^ (_ => "boolean")
  def char: Parser[String] = "C" ^^ (_ => "char")
  def byte: Parser[String] = "B" ^^ (_ => "byte")
  def short: Parser[String] = "S" ^^ (_ => "short")
  def int: Parser[String] = "I" ^^ (_ => "int")
  def float: Parser[String] = "F" ^^ (_ => "float")
  def long: Parser[String] = "J" ^^ (_ => "long")
  def double: Parser[String] = "D" ^^ (_ => "double")
  def reference: Parser[String] = "L" ~> """[^;]+""".r <~ ";" ^^ { path =>
    val JavaName = """^java\..*\.([^.]+)""".r
    val fqcn = (path.replaceAll("/", "."))

    fqcn match {
      case JavaName(simpleName) => simpleName
      case qualifiedName => qualifiedName
    }
  }
  def array: Parser[String] = "[" ~> nonVoid ^^ (_ + "[]")
}

object MethodSignatureParser extends MethodSignatureParser {
  def methodSignature(methodDescriptor: String): Option[String] =
    parse(signature, methodDescriptor) match {
      case Success(matched, _) => Some(matched)
      case Failure(msg, input) =>
        println(s"FAILURE: $msg Input: $input")
        None
      case Error(msg, _) =>
        println(s"ERROR: $msg")
        None
    }
}