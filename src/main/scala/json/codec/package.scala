package json

import java.util.UUID

import io.circe.Decoder._
import io.circe._
import io.circe.syntax._
import reactivemongo.bson._

import scalaz._

/**
  * @author Maxim Ochenashko
  */
package object codec {

  implicit def decoder[A, X](implicit e: Decoder[A]): Decoder[A @@ X] = new Decoder[A @@ X] {
    override def apply(c: HCursor): Result[A @@ X] = c.as[A].map(a => Tag[A, X](a))
  }

  implicit def encoder[A, X](implicit e: Encoder[A]): Encoder[A @@ X] = new Encoder[A @@ X] {
    override def apply(a: A @@ X): Json = Tag.unwrap(a).asJson
  }

  implicit val uuidBsonReads: BSONReader[BSONString, UUID] =
    BSONReader(v => UUID.fromString(v.as[String]))

  implicit val uuidBonWrites: BSONWriter[UUID, BSONString] =
    BSONWriter(v => BSON.write(v.toString))

}
