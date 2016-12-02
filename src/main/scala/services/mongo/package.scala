package services

import errors.BaseError

import scalaz.\/

/**
  * @author Maxim Ochenashko
  */
package object mongo {

  sealed trait MongoError extends BaseError

  final case class SaveError(error: String, entity: Option[Any] = None) extends MongoError {
    val reason = s"MongoDB save error. Entity: $entity. Error: $error"
  }

  type MongoMaybeError = MongoError \/ Unit

  type MongoResult[X] = MongoError \/ X

}
