package compflow

import dataflow._

object AppNode {
  
  implicit val scheduler = new ForkJoinScheduler
  val address = new java.net.InetSocketAddress("localhost", 30001)
  
  def main(args: Array[String]): Unit = {
    val n = new Node(address)
    n.log.level = dataflow.util.LogLevel.Trace
    
    n.process.await
  }
}