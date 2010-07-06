package compflow

import java.io.{ObjectInputStream,  ByteArrayInputStream}
import java.io.{ObjectOutputStream, ByteArrayOutputStream}
// Binary:
// ID:   Byte
// SIZE: Int
// DATA: Byte, Byte, ...

object CompMsg {
  type Computation = (Unit => Unit)
  
  val ID: Byte = 0x2A
  
  val HeaderSize: Int = 1 + 4 // ID + SIZE
  
  def parseHeader(bs: IndexedSeq[Byte]): Option[Int] = {
    if (bs.length < HeaderSize)
      None
    else if (bs(0) != ID)
      None
    else {
      val size = (bs(1) << 24) +
        ((bs(2) & 0xFF) << 16) +
        ((bs(3) & 0xFF) << 8 ) +
        ((bs(4) & 0xFF) << 0 )
      println("parseHeader: " + size)
      if (size > 0) Some(size)
      else          None
    }
  }
  
  def parseData(bs: IndexedSeq[Byte], size: Int): Option[Computation] = {
    val totalSize = HeaderSize + size
    println("totalSize = " + totalSize + " = " + HeaderSize + " + " + size)
    
    if (bs.length < totalSize)
      None
    else {
      val bis = new ByteArrayInputStream( bs.view(HeaderSize, HeaderSize+size).toArray )
      val ois = new ObjectInputStream(bis)
      val o = ois.readObject
      
      Some(o.asInstanceOf[Computation])
    }
  }
}

import CompMsg.Computation

case class CompMsg(val k: Computation) {

  def toBytes: Array[Byte] = {
    val data = serializeSelf
    val size = data.length 
    val bs = new Array[Byte](CompMsg.HeaderSize + size)
    
    // ID
    bs(0) = CompMsg.ID
    
    // SIZE
    bs(1) = (size >>> 24).toByte
    bs(2) = (size >>> 16).toByte
    bs(3) = (size >>>  8).toByte
    bs(4) = (size).toByte
    
    // DATA
    for ((b,i) <- data zip Stream.from(5))
      bs(i) = b
        
    bs
  }
  
  def serializeSelf: Array[Byte] = {
    val bos = new ByteArrayOutputStream
    val oos = new ObjectOutputStream(bos)
    oos.writeObject(k)
    bos.toByteArray
  }
}