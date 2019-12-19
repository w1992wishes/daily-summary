package me.w1992wishes.spark.streaming.example.intellif.http

import javax.servlet.http.{HttpServletRequest, HttpServletResponse}
import org.apache.spark.streaming.StreamingContext
import org.spark_project.jetty.server.{Request, Server}
import org.spark_project.jetty.server.handler.{AbstractHandler, ContextHandler}

/**
  * @author w1992wishes 2019/4/20 14:42
  */
object CloseHttpServer {

  /** **
    * 负责启动守护的jetty服务
    *
    * @param port 对外暴露的端口号
    * @param ssc  Stream上下文
    */
  def daemonHttpServer(port: Int, ssc: StreamingContext): Unit = {
    val server = new Server(port)
    val context = new ContextHandler()
    context.setContextPath("/close")
    context.setHandler(new CloseStreamHandler(ssc))
    server.setHandler(context)
    server.start()
  }

  /** * 负责接受http请求来优雅的关闭流
    *
    * @param ssc Stream上下文
    */
  class CloseStreamHandler(ssc: StreamingContext) extends AbstractHandler {
    override def handle(s: String, baseRequest: Request, req: HttpServletRequest, response: HttpServletResponse): Unit = {
      println("开始关闭......")
      ssc.stop(stopSparkContext = true, stopGracefully = true) //优雅的关闭
      response.setContentType("text/html; charset=utf-8")
      response.setStatus(HttpServletResponse.SC_OK)
      val out = response.getWriter
      out.println("close success")
      baseRequest.setHandled(true)
      println("关闭成功.....")
    }
  }

}
