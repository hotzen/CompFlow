package compflow

import CompMsg.Computation

import dataflow._
import io.{Dispatcher, Socket, Acceptor}
import java.net.InetSocketAddress
import java.io.{ObjectOutputStream, ByteArrayOutputStream}
import java.io.{ObjectInputStream, InputStream, ByteArrayInputStream}

class NodeRef(val address: InetSocketAddress)(implicit val scheduler: Scheduler, implicit val dispatcher: Dispatcher) {
  
  val socket = new Socket
  
  def process: SuspendingAwait @dataflow = {
    socket.connect(address)
    socket.process
  }
    
  def send(msg: CompMsg): Unit @dataflow = {
    socket.write << msg.toBytes
  }
        
    
  override def toString = "<NodeRef " + address + ">"
}

class Node(val address: InetSocketAddress)(implicit val scheduler: Scheduler) extends dataflow.util.Logging {
  import scala.collection.mutable.ArrayBuffer
  
  implicit val dispatcher = new Dispatcher
  private val acceptor = new Acceptor
  
  private val computations = Channel.create[Computation]
    
  def process: BlockingAwait = {
    Dispatcher.createThread(dispatcher).start
    acceptor.accept(address)
    
    flow {
      acceptor.connections.foreach(socket => flow {
        socket.read.foreach(processBytes(_))
      })
      computations.<<#
    }
    
    flow {
      computations.foreach(k => {
        log info ("received computation, resuming...")
        k()
      }) 
    }
  }
  
  private val bytes = new ArrayBuffer[Byte]( Socket.ReadBufferSize )
  
  private def processBytes(bs: Array[Byte]): Unit @dataflow = {
    bytes ++= bs
   
    val it = parse.iterator
    while (it.hasNext) {
      computations << it.next()
    }
      
  }
  
  private def parse: List[Computation] = {
    var ks: List[Computation] = Nil
    
    var continue = true
    while (continue) parseNext match {
      case (true, Some(k)) =>
        ks = k :: ks
      case (false, _) =>
        continue = false
    }
   
    ks.reverse
  }
  
  private def parseNext: (Boolean,Option[Computation]) = {
    CompMsg.parseHeader(bytes) match {
      case Some(size) => {
        CompMsg.parseData(bytes, size) match {
          case Some(k) => {
            log trace ("got computation!")
            bytes.remove(0, CompMsg.HeaderSize + size)
            (true, Some(k))
          }
          case None => {
            log trace ("no data (yet)")
            (false, None)
          }
        }
      }
      case None => {
        log error ("no header, bytes left: " + bytes.length)
        (false, None)
      }
    }
  }
  
  def shutdown = {
    acceptor.shutdown
  }
  
  override def toString = "<Node " + address + ">"
}