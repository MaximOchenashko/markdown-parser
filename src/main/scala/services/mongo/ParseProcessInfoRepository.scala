package services.mongo

import domain.ParseProcessInfo
import reactivemongo.api.DefaultDB
import reactivemongo.api.collections.bson.BSONCollection

import scala.concurrent.{ExecutionContext => EC, Future}
import scalaz.Scalaz._

/**
  * @author Maxim Ochenashko
  */
trait ParseProcessInfoRepository {

  import ParseProcessInfoRepository._

  def db: Future[DefaultDB]

  def save(p: ParseProcessInfo)(implicit ec: EC): Future[MongoMaybeError] =
    for {
      coll <- collection
      r <- coll insert p
    } yield r.errmsg.<\/(unitInstance.zero).leftMap(SaveError(_, p.some))

  private[this] def collection(implicit ec: EC): Future[BSONCollection] =
    for {
      database <- db
    } yield database[BSONCollection](ParseProcessInfoCollection)

}

object ParseProcessInfoRepository {
  private val ParseProcessInfoCollection = "parseProcessInfos"
}
