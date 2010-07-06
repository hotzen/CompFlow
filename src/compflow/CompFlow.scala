package compflow

import dataflow._
import dataflow.Scheduler
import io.{Dispatcher, Socket}
import scala.util.continuations._
import java.net.InetSocketAddress

package object compflow {
    
  def relocate(address: InetSocketAddress)(implicit scheduler: Scheduler, dispatcher: Dispatcher): Unit @dataflow = {
    val node = new NodeRef(address)
    node.process
    relocate(node)
  }
  
  def relocate(node: NodeRef)(implicit scheduler: Scheduler): Unit @dataflow = {
    shift { k: CompMsg.Computation =>
       flow {
         println("relocating...")
         node send CompMsg(k)
         println("relocated.")
       }
       ()
    }
  }
}