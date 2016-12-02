package services.security

import java.util.UUID

import domain.User
import domain.User.{Password, Email, UserID}
import services.mongo.{SaveError, MongoResult, UsersRepository}
import services.redis.RedisService
import services.redis.RedisService.RedisObject

import scala.concurrent.{ExecutionContext => EC, Future}

/**
  * @author Maxim Ochenashko
  */
trait AuthService {

  import AuthService._
  import scalaz._
  import Scalaz._
  import OptionT._
  import EitherT._
  import json.codec._
  import io.circe.generic.auto._

  def usersRepository: UsersRepository

  def redisService: RedisService

  def loadToken(key: String)(implicit ec: EC): Future[Option[AuthToken]] =
    redisService get[AuthToken] key

  def authorize(email: Email, password: Password)(implicit ec: EC): Future[Option[AuthToken]] =
    (for {
      user <- optionT(usersRepository.byEmailAndPwd(email, password))
      token = AuthToken(UUID.randomUUID.toString, user)
      _ <- optionT(redisService.save(token, DefaultExpireSeconds).map(_.some))
    } yield token).run

  def createUser(email: Email, password: Password)(implicit ec: EC): Future[MongoResult[User]] = {
    val newUser = User(UserID(UUID.randomUUID()), email, password)
    usersRepository.exists(email) flatMap {
      case true =>
        Future successful SaveError("Already exists").left
      case false =>
        (for {
          _ <- eitherT(usersRepository.save(newUser))
        } yield newUser).run
    }
  }

  def logout(key: String)(implicit ec: EC): Unit =
    redisService delete key
}

object AuthService {

  private val DefaultExpireSeconds = 60 * 60

  case class AuthToken(key: String, userInfo: User) extends RedisObject

}
