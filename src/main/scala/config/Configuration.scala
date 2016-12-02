package config

import com.typesafe.config.{Config, ConfigFactory}

/**
  * @author Maxim Ochenashko
  */
object Configuration {

  import scalaz._
  import Scalaz._

  lazy val config = ConfigFactory.load()

  implicit class ConfigEx(val underlying: Config) extends AnyVal {
    def string(path: String): Option[String] = underlying.hasPath(path) option underlying.getString(path)

    def int(path: String): Option[Int] = underlying.hasPath(path) option underlying.getInt(path)

    def double(path: String): Option[Double] = underlying.hasPath(path) option underlying.getDouble(path)

  }

}
