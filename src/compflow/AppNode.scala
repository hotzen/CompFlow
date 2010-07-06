package compflow

import dataflow._

object AppNode {
  
  implicit val scheduler = new ForkJoinScheduler
  val address = new java.net.InetSocketAddress("localhost", 30002)
  
  def main(args: Array[String]): Unit = {
    println("creating Node...")
    val n = new Node(address)
    n.log.level = dataflow.util.LogLevel.Trace

    println("processing " + n + "...")
    n.process.await
  }
}