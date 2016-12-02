package domain

import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import java.util.UUID

import domain.User.{UserIDTag, UserID}
import reactivemongo.bson.{BSONNull, BSONValue, BSON, BSONDocumentWriter, BSONDocument, BSONDocumentReader}

import scalaz.Tag

/**
  * @author Maxim Ochenashko
  */
final case class ParseProcessInfo(userId: UserID,
                                  timestamp: OffsetDateTime,
                                  source: String,
                                  result: Option[String],
                                  error: Option[String],
                                  sourceSize: Long,
                                  processingTime: Long)

object ParseProcessInfo {

  import json.codec._

  implicit object reads extends BSONDocumentReader[ParseProcessInfo] {
    override def read(bson: BSONDocument): ParseProcessInfo = ParseProcessInfo(
      Tag[UUID, UserIDTag](bson.getAs[UUID]("userId").get),
      bson.getAs[String]("timestamp").map(OffsetDateTime.parse).get,
      bson.getAs[String]("source").get,
      bson.getAs[String]("result"),
      bson.getAs[String]("error"),
      bson.getAs[Long]("sourceSize").get,
      bson.getAs[Long]("processingTime").get
    )
  }

  implicit object writes extends BSONDocumentWriter[ParseProcessInfo] {
    override def write(t: ParseProcessInfo): BSONDocument = BSONDocument(
      "userId" -> BSON.write(Tag.unwrap(t.userId)),
      "timestamp" -> BSON.write(DateTimeFormatter.ISO_OFFSET_DATE_TIME format t.timestamp),
      "source" -> BSON.write(t.source),
      "result" -> t.result.fold[BSONValue](BSONNull)(BSON.write(_)),
      "error" -> t.error.fold[BSONValue](BSONNull)(BSON.write(_)),
      "sourceSize" -> BSON.write(t.sourceSize),
      "processingTime" -> BSON.write(t.processingTime)
    )
  }
}
