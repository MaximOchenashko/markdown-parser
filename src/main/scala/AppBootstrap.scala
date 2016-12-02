import actors.ParseProcessInfoSaver
import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.stream.ActorMaterializer
import api.{IndexRoutes, AuthRoutes, ParserRoutes}
import config.Configuration
import domain.ParseProcessInfo
import reactivemongo.api.{DefaultDB, MongoConnection, MongoDriver}
import redis.RedisClient
import services.mongo.{ParseProcessInfoRepository, UsersRepository}
import services.redis.RedisService
import services.security.AuthService

import scala.concurrent.Future

/**
  * @author Maxim Ochenashko
  */
object AppBootstrap extends App with AuthRoutes with ParserRoutes with IndexRoutes {
  self =>

  import Configuration._

  implicit val actorSystem = ActorSystem()
  implicit val ec = actorSystem.dispatcher
  implicit val mat = ActorMaterializer()

  lazy val redisService = new RedisService {
    override def client: RedisClient = new RedisClient(
      config.getString("server.redis.host"),
      config.getInt("server.redis.port"),
      config.string("server.redis.password"),
      config.int("server.redis.db")
    )
  }

  lazy val mongoDB: Future[DefaultDB] = {
    lazy val mongoDriver = new MongoDriver()
    lazy val mongoUri = MongoConnection.parseURI(config.getString("server.mongo.connection-uri")).get
    mongoDriver.connection(mongoUri).database("markdown-parser")
  }

  lazy val usersRepositoryService = new UsersRepository {
    lazy val db: Future[DefaultDB] = mongoDB
  }

  lazy val authService = new AuthService {
    def redisService: RedisService = self.redisService

    def usersRepository: UsersRepository = usersRepositoryService
  }

  lazy val parseInfoRepository = new ParseProcessInfoRepository {
    override def db: Future[DefaultDB] = mongoDB
  }

  import akka.http.scaladsl.server.Directives._

  val ref = actorSystem.actorOf(ParseProcessInfoSaver.props(parseInfoRepository))
  actorSystem.eventStream.subscribe(ref, classOf[ParseProcessInfo])

  Http().bindAndHandle(
    indexRoutes ~ authRoutes(ec, mat) ~ parserRoutes(ec, mat, actorSystem),
    config.getString("server.http.host"),
    config.getInt("server.http.port")
  )

}
