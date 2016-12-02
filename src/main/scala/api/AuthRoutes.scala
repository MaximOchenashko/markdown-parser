package api

import akka.http.scaladsl.marshalling.ToResponseMarshallable
import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.model.HttpResponse
import akka.http.scaladsl.server.Directives
import akka.stream.Materializer
import api.Secured.TokenHeader
import de.heikoseeberger.akkahttpcirce.CirceSupport
import domain.User.{Email, Password}
import api.AuthRoutes.UserInfo

import scala.concurrent.{Future, ExecutionContext}
import scalaz.{Tag, \/-, -\/}

/**
  * @author Maxim Ochenashko
  */
trait AuthRoutes extends Secured {

  import json.codec._
  import CirceSupport._
  import Directives._
  import io.circe.generic.auto._

  private[this] val regex =
    "\\b[a-zA-Z0-9.!#$%&'*+/=?^_`{|}~-]+@[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?(?:\\.[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?)*\\b"

  def authRoutes(implicit ec: ExecutionContext, mat: Materializer) =
    pathPrefix("api" / "v1") {
      path("signIn") {
        (post & entity(as[UserInfo])) { case UserInfo(email, password) =>
          complete {
            isValid(email, password) match {
              case false =>
                Future successful BadRequest -> "Invalid e-mail or password format."
              case true =>
                authService.authorize(email, password).map[ToResponseMarshallable] {
                  case Some(token) =>
                    HttpResponse(OK, headers = List(TokenHeader(token.key)))
                  case None =>
                    BadRequest -> "Invalid credentials"
                }
            }
          }
        }
      } ~
        path("logout") {
          post {
            secured { authToken =>
              authService.logout(authToken.key)
              Future successful OK
            }
          }
        } ~
        path("signUp") {
          (post & entity(as[UserInfo])) { case UserInfo(email, password) =>
            complete {
              isValid(email, password) match {
                case false =>
                  Future successful BadRequest -> "Invalid e-mail or password format. Password must be non empty."
                case true =>
                  authService.createUser(email, password).map[ToResponseMarshallable] {
                    case \/-(v) =>
                      HttpResponse(Created)
                    case -\/(e) =>
                      BadRequest -> e.reason
                  }
              }
            }
          }
        }
    }

  private def isValid(email: Email, pwd: Password): Boolean =
    Tag.unwrap(email).matches(regex) && Tag.unwrap(pwd).trim.nonEmpty
}

object AuthRoutes {

  final case class UserInfo(email: Email, password: Password)


}
