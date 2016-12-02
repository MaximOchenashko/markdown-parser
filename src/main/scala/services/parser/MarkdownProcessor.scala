package services.parser

import scala.util.parsing.combinator.RegexParsers
import scalaz._
import Scalaz._

/**
  * @author Maxim Ochenashko
  */
class MarkdownProcessor extends RegexParsers {

  import scala.language.postfixOps

  private val ws: Parser[String] = """( |\t|\v)+""".r

  private val specialInlineChars = Set(' ', '`', '<', '[', '*', '_', '!')
  private val specialLinkInlineChars = specialInlineChars + ']'

  private lazy val ows: Parser[String] = Parser { in =>
    if (in.atEnd) Failure("End of input.", in)
    else {
      val (res, input) = extractSeq(in) { char => char == ' ' || char == '\t' }
      Success(res, input)
    }
  }

  private lazy val url: Parser[string] = extractText(Set(')', ' ', '\t'))

  private lazy val aChar: Parser[string] = Parser { in =>
    if (in.atEnd) Failure("End of input reached.", in)
    else Success(string(Character.toString(in.first)), in.rest)
  }

  private lazy val elementParsers: Parser[MarkdownNode] = Parser { in =>
    if (in.atEnd) {
      Failure("End of Input Reached", in)
    } else {
      in.first match {
        case '[' => hrefMatcher(in)
        case '*' => spanMatcher(in)
        case _ => Failure("Lookahead does not start inline element.", in)
      }
    }
  }

  private lazy val linkMatcher: Parser[Seq[MarkdownNode]] =
    (extractText(specialLinkInlineChars) | elementParsers | not(']') ~> aChar) *

  private lazy val hrefMatcher: Parser[link] =
    '[' ~> linkMatcher ~ ("](" ~ ows) ~ url <~ (ows ~ ')') >> {
      case txt ~ _ ~ u =>
        val containsNested = txt exists { e => e.isInstanceOf[link] }
        if (containsNested) failure("Cannot nest strong text")
        else success(link(txt, u))
    }

  private lazy val lineMatcher: Parser[MarkdownNode] =
    opt("""#+""".r) ~ ((extractText(specialInlineChars) | elementParsers | aChar) *) ^^ {
      case Some(prefix) ~ other =>
        header(prefix.length, other)
      case None ~ other =>
        paragraph(other)
    }

  private lazy val spanMatcher: Parser[MarkdownNode] = strongMatcher | emMatcher

  private lazy val strongMatcher: Parser[strong] =
    span("**") >> { v =>
      val containsNested = v exists { e => e.isInstanceOf[strong] }
      if (containsNested) failure("Nested <strong> is restricted")
      else success(strong(v))
    }

  private lazy val emMatcher: Parser[em] =
    span("*") >> { v =>
      val containsNested = v exists { e => e.isInstanceOf[em] }
      if (containsNested) failure("Nested <em> is restricted")
      else success(em(v))
    }

  /**
    * Parse method
    *
    * @param s source
    * @return left if error happens, right if parsed successfully
    */
  def tryParse(s: String): ParserError \/ MarkdownNode =
    parseAll(lineMatcher, s) match {
      case Success(r, _) => r.right
      case NoSuccess(e, _) => ParsingError(e).left
    }

  private def spanInline(end: Parser[Any]): Parser[MarkdownNode] =
    extractText(specialInlineChars) | elementParsers | (not(end) ~> aChar)

  private def span(limiter: String): Parser[Seq[MarkdownNode]] =
    (limiter ~ not(ws)) ~> (spanInline(not(lookbehind(Set(' ', '\t', '\n'))) ~ limiter) +) <~ limiter

  /**
    * Extracts text from the input
    *
    * @param special stop symbols
    * @return
    */
  private def extractText(special: Set[Char]): Parser[string] = Parser { in =>
    if (in.atEnd) {
      Failure("End of input.", in)
    } else {
      val (result, input) = extractSeq(in) { char => !special.contains(char) }
      if (result.length == 0) Failure("No text consumed.", in)
      else Success(string(result.toString), input)
    }
  }

  private def lookbehind(cs: Set[Char]): Parser[Unit] = Parser { in =>
    val source = in.source
    val offset = in.offset
    if (offset == 0) {
      Failure("No chars before current char, cannot look behind.", in)
    } else if (!cs.contains(source.charAt(offset - 1))) {
      Failure("Previous char was '" + source.charAt(offset - 1) + "' expected one of " + cs, in)
    } else {
      Success((), in)
    }
  }

  /**
    * Extracts sequence that match filter
    *
    * @param in     Input data
    * @param filter predicate
    * @return extracted sequence and input without extracted seq
    */
  private def extractSeq(in: Input)(filter: Char => Boolean): (String, Input) = {
    val start = in.offset
    val source = in.source
    val end = source.length
    val lastIdx = Stream.from(start)
      .takeWhile { idx => idx < end && filter(source charAt idx) }
      .lastOption
      .map(_ + 1)

    lastIdx match {
      case Some(idx) =>
        source.subSequence(start, idx).toString -> in.drop(idx - start)
      case None =>
        "" -> in
    }
  }
}

object MarkdownProcessor extends MarkdownProcessor {

  def parse(source: String): ParserError \/ html = {
    val parseResult = source.split("\n") map { line => tryParse(line) }
    val firstError = parseResult collectFirst { case -\/(e) => e }

    firstError <\/ html(body(parseResult.collect { case \/-(line) => line }))
  }

}