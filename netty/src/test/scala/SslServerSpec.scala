package unfiltered.netty

import org.specs._
import unfiltered.spec
import unfiltered.response._
import unfiltered.request._
import unfiltered.request.{Path => UFPath}

object SslServerSpec extends Specification with spec.netty.Served with spec.SecureClient {
  
  import unfiltered.netty.Https
  import org.apache.http.client.ClientProtocolException
  import dispatch._
  
  // generated keystore for localhost
  // keytool -keystore keystore -alias unfiltered -genkey -keyalg RSA
  val keyStorePath = getClass.getResource("/keystore").getPath
  val keyStorePasswd = "unfiltered"
  val securePort = port
  
  doBeforeSpec { 
    System.setProperty("netty.ssl.keyStore", keyStorePath)
    System.setProperty("netty.ssl.keyStorePassword", keyStorePasswd)
  }
  doAfterSpec { 
    System.clearProperty("netty.ssl.keyStore")
    System.clearProperty("netty.ssl.keyStorePassword")
  }
  
  def setup = { port =>
    try {
      val securePlan = new unfiltered.netty.cycle.Plan with Secured {
        import org.jboss.netty.channel.{ChannelHandlerContext, ExceptionEvent}
        
        def intent = { case GET(UFPath("/")) => ResponseString("secret") ~> Ok }
        
        override def exceptionCaught(ctx: ChannelHandlerContext, e: ExceptionEvent) =
          ctx.getChannel.close
      }
      
      Https(port, "localhost").handler(securePlan) 
    } catch { case e => e.printStackTrace 
      throw new RuntimeException(e)
    }
  }
  
  "A Secure Server" should {
    "respond to secure requests" in {
      http(host.secure as_str) must_== "secret"
    }
    "refuse connection to unsecure requests" in {
      http(host as_str) must throwA[ClientProtocolException]
    }
  }
}