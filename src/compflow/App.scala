package compflow

import compflow._
import dataflow._

object App {

  def main(args: Array[String]): Unit = {
    implicit val scheduler = new ForkJoinScheduler
    implicit val dispatcher = dataflow.io.Dispatcher.start
  
    val remote = AppNode.address 
    
    val f = flow {
      println("A * B = ?")
      print("A: ")
      val a = readInt

//      relocate(remote)
      scala.util.continuations.shift { k: (Unit => Unit) =>
        val bs = CompMsg(k).toBytes
        println("bytes: " + bs.length)
        
        CompMsg.parseHeader(bs) match {
          case Some(size) => {
            CompMsg.parseData(bs, size) match {
              case Some(k) => {
                println("resuming...")
                k.apply( () )
              }
              case None => println("no K")
            }
          }
          case None => println("no header")
        }
      }
           
      print("B: ")
      val b = readInt
      println
      
      println(a + " * " + b + " = " + (a*b))
      
      42
    }
    
    println("flow: " + f.get)
    Thread.sleep(10000)
  }
}