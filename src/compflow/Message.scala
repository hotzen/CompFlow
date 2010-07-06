package compflow

import compflow.Computation
import java.io.{ObjectOutputStream, ByteArrayOutputStream}

object ProtoUtil {
  
  def IntToBytes(i: Int): Array[Byte] =
    Array[Byte](
     (i >>> 24).toByte,
     (i >>> 16).toByte,
     (i >>> 8).toByte,
     (i >>> 0).toByte
    )
    
  def BytesToInt(bs: Seq[Byte]): Int = {
    assert(bs.length >= 4)
    
      (bs(0)         << 24) +
    ( (bs(1) & 0xFF) << 16) +
    ( (bs(2) & 0xFF) << 8 ) +
      (bs(3) & 0xFF)
  }
}

trait Message {
  def toBytes: Array[Byte]
}

object ResumeMsg {
  val ID: Byte = 1
}
case class ResumeMsg(k: Computation) extends Message {
  // HEADER:  BYTE
  // SIZE:    INT
  // PAYLOAD: BYTE, BYTE, ...
  def toBytes: Array[Byte] = {
    val data    = serialize(k)
    val datalen = data.length
    
    val msgSize = 1 + 4 + datalen
    
    val bs = new Array[Byte](msgSize)
    bs(0) = ResumeMsg.ID
    
    val size = ProtoUtil.IntToBytes(msgSize)
    var i = 1
    size.foreach(b => {
      bs(i) = b
      i = i + 1
    })
    
    var j = 5
    data.foreach(b => {
      bs(j) = b
      j = j + j
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