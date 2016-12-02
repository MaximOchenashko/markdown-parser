package services.mongo

import domain.User
import domain.User.{Password, Email}
import reactivemongo.api.DefaultDB
import reactivemongo.api.collections.bson.BSONCollection
import reactivemongo.bson.{BSONArray, BSONDocument}

import scala.concurrent.{ExecutionContext => EC, Future}
import scalaz._

/**
  * @author Maxim Ochenashko
  */
trait UsersRepository {

  import scalaz._
  import Scalaz._
  import reactivemongo.api._
  import UsersRepository._

  def db: Future[DefaultDB]

  def byEmail(email: Email)(implicit ec: EC): Future[Option[User]] =
    for {
      coll <- usersCollection
      r <- coll.find(BSONDocument("email" -> BSONDocument("$eq" -> Tag.unwrap(email))))
        .cursor[User]()
        .headOption
    } yield r

  def byEmailAndPwd(email: Email, pwd: Password)(implicit ec: EC): Future[Option[User]] =
    for {
      coll <- usersCollection
      r <- coll.find(BSONDocument("$and" -> BSONArray(Seq(
        BSONDocument("email" -> BSONDocument("$eq" -> Tag.unwrap(email))),
        BSONDocument("password" -> BSONDocument("$eq" -> Tag.unwrap(pwd)))
      ))))
        .cursor[User]()
        .headOption
    } yield r

  def exists(email: Email)(implicit ec: EC): Future[Boolean] =
    for {v <- byEmail(email)} yield v.isDefined

  def save(user: User)(implicit ec: EC): Future[MongoMaybeError] =
    for {
      coll <- usersCollection
      r <- coll insert user
    } yield r.errmsg.<\/(unitInstance.zero).leftMap(SaveError(_, user.some))

  private[this] def usersCollection(implicit ec: EC): Future[BSONCollection] =
    for {
      database <- db
    } yield database[BSONCollection](UsersCollectionName)
}


object UsersRepository {
  private val UsersCollectionName = "users"

  type MongoResult[X] = Reader[DefaultDB, Future[X]]

}
