package compflow

import dataflow._
import io.{Dispatcher, Socket, Acceptor}
import java.net.InetSocketAddress
import java.io.{ObjectOutputStream, ByteArrayOutputStream}
import java.io.{ObjectInputStream, InputStream, ByteArrayInputStream}


class NodeRef(val address: InetSocketAddress)(implicit val scheduler: Scheduler, implicit val dispatcher: Dispatcher) {
  
  lazy val socket = {
    val s = new Socket()
    s.connect(address)
    s.process
    s
  }
    
  def send(msg: Message): Unit @dataflow =
    socket.write << msg.toBytes    
}

class Node(val address: InetSocketAddress)(implicit val scheduler: Scheduler) {

  implicit val dispatcher = new Dispatcher
  val acceptor = new Acceptor
  
  def start = {
    Dispatcher.createThread(dispatcher).start
    acceptor.bind(address)
    acceptor.accept
    flow {
      acceptor.connections.foreach(socket => flow {
        socket.read.foreach(processBytes(_))
      })
    }
  }
  
  private def processBytes(bs: Array[Byte]): Unit @dataflow = {
    
  }
  
  def shutdown = {
    acceptor.shutdown
  }
}