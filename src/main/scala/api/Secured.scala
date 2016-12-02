package api

import akka.http.scaladsl.marshalling.ToResponseMarshallable
import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.model.headers.{ModeledCustomHeaderCompanion, ModeledCustomHeader}
import akka.http.scaladsl.server.Directives._
import api.Secured.TokenHeader
import services.security.AuthService
import services.security.AuthService.AuthToken

import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try

/**
  * @author Maxim Ochenashko
  */
trait Secured {

  def authService: AuthService

  /**
    * Extracts auth token and verifies it.
    *
    * @param f  if token is valid applies `f` method
    * @param ec ctx
    * @return
    */
  def secured(f: AuthToken => Future[ToResponseMarshallable])(implicit ec: ExecutionContext) = {
    headerValueByName(TokenHeader.name) { token =>
      complete {
        authService.loadToken(token).flatMap[ToResponseMarshallable] {
          case Some(userInfo) =>
            f(userInfo)

          case None =>
            Future successful Forbidden
        }
      }
    }
  }
}

object Secured {

  final class TokenHeader(val value: String) extends ModeledCustomHeader[TokenHeader] {

    override def renderInResponses(): Boolean = true

    override def renderInRequests(): Boolean = true

    override def companion: ModeledCustomHeaderCompanion[TokenHeader] = TokenHeader

  }

  object TokenHeader extends ModeledCustomHeaderCompanion[TokenHeader] {
    override def name: String = "Api-Token"

    override def parse(value: String): Try[TokenHeader] = Try(new TokenHeader(value))
  }

}
