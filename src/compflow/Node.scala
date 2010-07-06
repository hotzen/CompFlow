package compflow

import dataflow._
import io.{Dispatcher, Socket, Acceptor}
import java.net.InetSocketAddress
import java.io.{ObjectOutputStream, ByteArrayOutputStream}
import java.io.{ObjectInputStream, InputStream, ByteArrayInputStream}

class NodeRef(val address: InetSocketAddress)(implicit val scheduler: Scheduler, implicit val dispatcher: Dispatcher) {
  
  val socket = new Socket
  
  def start: SuspendingAwait @dataflow = {
    socket.connect(address)
    socket.process
  }
    
  def send(msg: Message): Unit @dataflow =
    socket.write << msg.toBytes    
}

class Node(val address: InetSocketAddress)(implicit val scheduler: Scheduler) extends dataflow.util.Logging {
  import scala.collection.mutable.ArrayBuffer
  
  implicit val dispatcher = new Dispatcher
  private val acceptor = new Acceptor
  
  private val msgs = Channel.create[Message]
  
  def start: SuspendingAwait = {
    Dispatcher.createThread(dispatcher).start
    acceptor.accept(address)
  }
  
  def process: BlockingAwait = {
    start
    
    flow {
      acceptor.connections.foreach(socket => flow {
        socket.read.foreach(processBytes(_))
        msgs.<<#
      })
    }
    
    flow {
      msgs.foreach(msg => msg match {
        case ResumeMsg(k) => {
          log info ("Resume-Msg, resuming...")
          k()
        }
        case x => {
          log warn ("unknown message " + x)
        }
      })
    }
  }
  
  
  
  private val bytes = new ArrayBuffer[Byte]( Socket.ReadBufferSize )
  
  // Left(true):  Parser is able to parse it but data is missing
  // Left(false): Parser is not able to parse it
  private val parsers = List[() => Either[Boolean,Message]](
    parseResumeMsg _   
  )
  
  private def processBytes(bs: Array[Byte]): Unit @dataflow = {
    bytes ++= bs
    
    val msgs = parse()
    println("processBytes: " + msgs)
    
    ()
  }
  
  def parse(): List[Message] = {
    var msgs: List[Message] = Nil
    
    var continue = true
    while (bytes.length > 0 && continue) {
      log trace ("bytes=" + bytes.length + ", parsing roud...")
      var handled = false
      val it = parsers.iterator
      
      while (!handled && it.hasNext) {
        val parser = it.next
        parser.apply match {
          case Left(true) => {
            log trace ("parser is able to handle but data is missing, suspending")
            handled  = true
            continue = false
          }
          case Left(false) => {
            log trace ("parser is NOT able to handle data")
          }
          case Right(msg) => {
            msgs ::= msg
            handled = true
          }
        }
      }
      
      if (!handled) {
        log trace ("no parser was able to handle data, suspending")
        continue = false
      }
    }
    
    log trace ("parse finished/suspended, bytes=" + bytes.length)
    msgs.reverse
  }
  
  def parseResumeMsg: Either[Boolean,Message] = {
    if (bytes.length < 5)
      return Left(true)
    
    if (bytes(0) != ResumeMsg.ID)
      return Left(false)
    
    val size = ProtoUtil.BytesToInt( bytes.view(1,5) )
    val totalMsgSize = 1 + 4 + size
    
    if (bytes.length < totalMsgSize)
      return Left(true)
    
    val dataBytes = bytes.view(5, totalMsgSize).toArray
    bytes.remove(0, totalMsgSize)
    
    val bis = new java.io.ByteArrayInputStream(dataBytes)
    val ois = new java.io.ObjectInputStream(bis)
        
    val o = ois.readObject 
    Right( o.asInstanceOf[ResumeMsg] )
  }
  
  def shutdown = {
    acceptor.shutdown
  }
}