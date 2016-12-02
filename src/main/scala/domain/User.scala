package domain

import java.util.UUID

import domain.User.{Password, Email, UserID}
import reactivemongo.bson._

import scalaz._

/**
  * @author Maxim Ochenashko
  */
case class User(userId: UserID, email: Email, password: Password)

object User {

  trait UserIDTag

  type UserID = UUID @@ UserIDTag

  def UserID(value: UUID): UserID = Tag[UUID, UserIDTag](value)

  trait EmailTag

  type Email = String @@ EmailTag

  def Email(value: String): Email = Tag[String, EmailTag](value)

  trait PasswordTag

  type Password = String @@ PasswordTag

  def Password(value: String): Password = Tag[String, PasswordTag](value)

  import json.codec._

  implicit object reads extends BSONDocumentReader[User] {
    override def read(bson: BSONDocument): User = User(
      Tag[UUID, UserIDTag](bson.getAs[UUID]("userId").get),
      Tag[String, EmailTag](bson.getAs[String]("email").get),
      Tag[String, PasswordTag](bson.getAs[String]("password").get)
    )
  }

  implicit object writes extends BSONDocumentWriter[User] {
    override def write(t: User): BSONDocument = BSONDocument(
      "userId" -> Tag.unwrap(t.userId),
      "email" -> Tag.unwrap(t.email),
      "password" -> Tag.unwrap(t.password)
    )
  }

}
