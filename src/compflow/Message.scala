package compflow

import compflow.Computation
import java.io.{ObjectOutputStream, ByteArrayOutputStream}

case class Message(k: Computation) {

  // HEADER:  BYTE
  // LENGTH:  INT
  // PAYLOAD: BYTE, BYTE, ...
  val HEADER: Byte = 1
    
  def toBytes: Array[Byte] = {
    val d = serialize(k)
    val dlen = d.length
    
    val msgSize = 1 + 4 + dlen
    
    val bs = new Array[Byte](msgSize)
    bs(0) = HEADER
    bs(1) = (dlen >>> 24).toByte
    bs(2) = (dlen >>> 16).toByte
    bs(3) = (dlen >>> 8).toByte
    bs(4) = (dlen >>> 0).toByte
    
    var i = 5
    d.foreach(b => {
      bs(i) = b
      i = i + 1
    })
    
    bs
  }
  
  def serialize(k: Computation): Array[Byte] = {
    val bos = new ByteArrayOutputStream(10)
    val oos = new ObjectOutputStream(bos)
    oos.writeObject(k)
    bos.toByteArray
  }
}