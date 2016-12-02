package api

import akka.http.scaladsl.server.Directives

/**
  * @author iRevThis
  */
trait IndexRoutes {

  import Directives._

  val indexRoutes =
    pathSingleSlash {
      getFromResource("index.html")
    }

}
