package unfiltered.server

import org.eclipse.jetty.server.{Server => JettyServer, Connector}
import org.eclipse.jetty.server.handler.ContextHandlerCollection
import org.eclipse.jetty.servlet.{FilterHolder, ServletContextHandler}
import org.eclipse.jetty.server.bio.SocketConnector

object Http {
  def apply[T <: javax.servlet.Filter](port: Int)(filter: javax.servlet.Filter) {
    val server = new JettyServer()

    val conn = new SocketConnector()
    conn.setPort(port)
    server.addConnector(conn)

    val context = new ServletContextHandler(server,"/")
    context.addFilter(new FilterHolder(filter), "/*", 
      ServletContextHandler.NO_SESSIONS|ServletContextHandler.NO_SECURITY)
    context.addServlet(classOf[org.eclipse.jetty.servlet.DefaultServlet], "/")
    server.setHandler(context)

    // enter wait loop if not in main thread, e.g. running inside sbt
    Thread.currentThread.getName match {
      case "main" => 
        server.setStopAtShutdown(true)
        server.start()
        server.join()
      case _ => 
        server.start()
        println("Embedded server running. Press any key to stop.")
        def doWait() {
          try { Thread.sleep(1000) } catch { case _: InterruptedException => () }
          if(System.in.available() <= 0)
            doWait()
        }
        doWait()
        server.stop()
    }
  }
}
