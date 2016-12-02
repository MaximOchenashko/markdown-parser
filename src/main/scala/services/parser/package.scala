package services

import errors.BaseError

/**
  * @author Maxim Ochenashko
  */
package object parser {

  sealed trait ParserError extends BaseError

  final case class ParsingError(reason: String) extends ParserError

  sealed trait MarkdownNode {
    def show: String
  }

  final case class em(children: Seq[MarkdownNode]) extends MarkdownNode {
    lazy val show: String = children.map(_.show).mkString("<em>", "", "</em>")
  }

  final case class link(text: Seq[MarkdownNode], url: string) extends MarkdownNode {
    lazy val show: String = "<a href=\"" + url.show + "\">" + text.map(_.show).mkString + "</a>"
  }

  final case class string(value: String) extends MarkdownNode {
    lazy val show: String = value
  }

  final case class strong(children: Seq[MarkdownNode]) extends MarkdownNode {
    lazy val show: String = children.map(_.show).mkString("<strong>", "", "</strong>")
  }

  final case class header(hType: Int, children: Seq[MarkdownNode]) extends MarkdownNode {

    lazy val show: String = hType match {
      case x if x < 7 =>
        children.map(_.show).mkString(s"<h$x>", "", s"<h$x>")
      case x =>
        val rest = (1 to x - 6).map { _ => "#" }.mkString
        children.map(_.show).mkString(s"<h6>" + rest, "", s"<h6>")
    }

  }

  final case class paragraph(children: Seq[MarkdownNode]) extends MarkdownNode {
    lazy val show: String = children.map(_.show).mkString("<p>", "", "</p>")
  }

  final case class body(lines: Seq[MarkdownNode]) extends MarkdownNode {
    lazy val show: String = lines.map(_.show).mkString("<body>\n", "\n", "\n</body>")
  }

  final case class html(body: body) extends MarkdownNode {
    lazy val show: String = s"<html>\n${body.show}\n</html>"
  }
}
