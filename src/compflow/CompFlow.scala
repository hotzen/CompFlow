package compflow

import dataflow._
import dataflow.Scheduler
import io.{Dispatcher, Socket}
import scala.util.continuations._
import java.net.InetSocketAddress

package object compflow {
  
  type Computation = (Unit => Unit)
    
  def relocate(node: NodeRef)(implicit scheduler: Scheduler): Unit @dataflow = {
    shift { k: Computation =>
       flow { node send ResumeMsg(k) }
       ()
    }
  }
}