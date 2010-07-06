package compflow

import dataflow._

object App {

  def main(args: Array[String]): Unit = {
    implicit val scheduler = new ForkJoinScheduler
    
    val f = flow {
      println("A * B = ?")
      print("A: ")
      val a = readInt
      println
      
      print("B: ")
      val b = readInt
      println
      
      println(a + " * " + b + " = " + (a*b))
    }
    f.await
  }
}