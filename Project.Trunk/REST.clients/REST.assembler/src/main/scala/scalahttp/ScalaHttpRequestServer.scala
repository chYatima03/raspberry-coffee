package scalahttp

import http.HTTPServer

object ScalaHttpRequestServer {

  class ScalaServer {
    var httpServer: HTTPServer = null
    var httpPort = 9998

    private def doIt: Unit = {
      println("Initializing...")
      val port = System.getProperty("http.port")
      if (port != null) {
        httpPort = port.toInt
      }
      println(s"Starting server on port $httpPort")
      httpServer = startHttpServer(httpPort, new ScalaHttpRequestManager(this))
    }

    doIt

    def startHttpServer(port: Int, requestManager: ScalaHttpRequestManager): HTTPServer = {
      var newHttpServer: HTTPServer = null
      try {
        newHttpServer = new HTTPServer(port, requestManager) {
          override def onExit(): Unit = {
            requestManager.onExit()
          }
        }
        newHttpServer.startServer()
      } catch {
        case e: Exception =>
          e.printStackTrace()
      }
      newHttpServer
    }
  }

  def main(args: Array[String]): Unit = {
    println("Starting the Scala server")
    System.setProperty("http.verbose", "true")
    val scalaHttpServer = new ScalaServer
  }
}
