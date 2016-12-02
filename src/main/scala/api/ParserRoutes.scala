package api

import java.time.{OffsetDateTime, ZoneOffset}

import akka.actor.ActorSystem
import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.model.{ContentType, HttpCharsets, HttpEntity, HttpResponse, MediaTypes}
import akka.http.scaladsl.server.Directives
import akka.stream.Materializer
import api.ParserRoutes.Markdown
import de.heikoseeberger.akkahttpcirce.CirceSupport
import domain.ParseProcessInfo
import domain.User.UserID
import errors.BaseError
import services.parser.{html, MarkdownProcessor}

import scala.concurrent.{ExecutionContext, Future}
import scalaz.{-\/, \/, \/-}

/**
  * @author Maxim Ochenashko
  */
trait ParserRoutes extends Secured {

  import CirceSupport._
  import Directives._
  import io.circe.generic.auto._

  def parserRoutes(implicit ec: ExecutionContext, mat: Materializer, system: ActorSystem) =
    pathPrefix("api" / "v1") {
      path("parseMarkdown") {
        (post & entity(as[Markdown])) { case Markdown(source) =>
          secured { authToken =>
            processRequest(authToken.userInfo.userId, source) match {
              case -\/(e) =>
                Future successful BadRequest -> e.reason
              case \/-(r) =>
                Future successful HttpResponse(
                  OK,
                  entity = HttpEntity(ContentType(MediaTypes.`text/html`, HttpCharsets.`UTF-8`), r.show)
                )
            }
          }
        }
      }
    }

  private[this] def processRequest(userId: UserID, source: String)(implicit system: ActorSystem): BaseError \/ html = {
    val start = System.nanoTime()
    val result = MarkdownProcessor parse source
    val end = System.nanoTime()
    val parseInfo = ParseProcessInfo(
      userId,
      OffsetDateTime now ZoneOffset.UTC,
      source,
      _: Option[String],
      _: Option[String],
      source.length,
      end - start
    )

    result match {
      case -\/(e) => parseInfo(None, Some(e.reason))
      case \/-(r) => parseInfo(Some(r.show), None)
    }

    system.eventStream.publish(parseInfo)
    result
  }
}

object ParserRoutes {

  final case class Markdown(source: String)

}
