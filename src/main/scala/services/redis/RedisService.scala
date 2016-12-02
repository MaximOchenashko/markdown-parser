package services.redis

import redis.RedisClient

import scala.concurrent.{ExecutionContext => EC, Future}
import scalaz.Reader

/**
  * @author Maxim Ochenashko
  */
trait RedisService {

  import RedisService._
  import io.circe.syntax._
  import io.circe.{Decoder, Encoder, jawn}

  import scalaz._
  import Scalaz._
  import OptionT._

  def client: RedisClient

  def get[X <: RedisObject](key: String)(implicit d: Decoder[X], ec: EC): Future[Option[X]] =
    (for {
      buffer <- optionT(client get key)
    } yield jawn.decode(buffer.utf8String).valueOr(throw _)).run

  def save[X <: RedisObject](obj: X, ttl: Long)(implicit e: Encoder[X], ec: EC): Future[Boolean] =
    client.setex(obj.key, ttl, obj.asJson.noSpaces)

  def delete(key: String)(implicit ec: EC): Future[Long] =
    client del key

  def delete[X <: RedisObject](obj: X)(implicit ec: EC): Future[Long] =
    client del obj.key

  def exists(key: String)(implicit ec: EC): Future[Boolean] =
    client exists key

}

object RedisService {

  type RedisResult[X] = Reader[RedisClient, Future[X]]

  trait RedisObject {
    def key: String
  }

}
