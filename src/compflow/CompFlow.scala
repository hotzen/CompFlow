package compflow

import dataflow._
import dataflow.Scheduler
import io.{Dispatcher, Socket}
import scala.util.continuations._
import java.net.InetSocketAddress

package object compflow {
  
  type Computation = (Unit => Unit)
  
  def startNode(address: InetSocketAddress)(implicit scheduler: Scheduler): Node = {
    val n = new Node(address)
    n.start
    n
  }
  
  def relocate(address: InetSocketAddress)(implicit scheduler: Scheduler, implicit dispatcher: Dispatcher): Unit @dataflow = {
    shift { k: Computation => flow {
      new NodeRef(address) send Message(k)
      ()
    }}
  }
}