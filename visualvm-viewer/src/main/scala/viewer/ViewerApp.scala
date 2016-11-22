package viewer

import java.io.File
import visualvm.Snapshot
import org.netbeans.lib.profiler.results.cpu.CPUResultsSnapshot
import model._
import org.netbeans.lib.profiler.results.cpu.PrestimeCPUCCTNodeBacked
import org.netbeans.lib.profiler.results.CCTNode

import scala.collection.mutable
import javax.swing.JFrame
import javax.swing.JTree
import scalaswingcontrib.tree.Tree
import scalaswingcontrib.tree.TreeModel

object ViewerApp extends App {
  val VIEW = CPUResultsSnapshot.METHOD_LEVEL_VIEW

  //val file = new File("snapshot-1473717553762-cpu-calltree.nps")
  val file = new File("visualvm-viewer.nps")
  val s = Snapshot.load(file)
  println(s)

  val cr = s.getCpuResults

  val nMethods = cr.getNInstrMethods
  val methClasses = cr.getInstrMethodClasses(VIEW)
  val methNames = cr.getInstrMethodNames
  val methSigs = cr.getInstrMethodSignatures
  val methods = for (i <- 0 until nMethods)
    yield Method(methClasses(i), methNames(i), if (methSigs(i).isEmpty) None else Some(methSigs(i)))

  //val refs = new mutable.HashMap[Method, mutable.Set[MethodNode]] with mutable.MultiMap[Method, MethodNode]

  val threads =
    for (threadId <- cr.getThreadIds.toList) yield {
      val cont = cr.getContainerForThread(threadId, VIEW)
      val thr = Thread(cont.getThreadId, cont.getThreadName)
      //println(thr)

      val node = cont.getRootNode

      def process(node: CCTNode): Option[MethodNode] = {
        node match {
          case p: PrestimeCPUCCTNodeBacked =>
            val meth = methods(p.getMethodId)

            val chd: List[MethodNode] = {

              val children = p.getChildren
              if (children != null)
                for (child <- children.toList) yield {
                  println("  " + child + " " + child.getClass)
                  process(child)
                }
              else Nil
            }.flatten

            val times = Times(p.getTotalTime0, p.getTotalTime1, p.getSleepTime0, p.getWaitTime0)
            if (times == Times(0, 0, 0, 0))
              None
            else Some {
              val mn = MethodNode(meth, p.isSelfTimeNode, chd, times)
              //refs.addBinding(meth, mn)
              mn
            }
        }
      }

      ThreadNode(thr, process(node).get)

    }

  val tree = new Tree[Node] {
    model = TreeModel[Node](RootNode(List( ThreadsNode(threads), MergedThreadsNode(threads))))(_.children)
    renderer = Tree.Renderer(_.value)
  }

  val f = new JFrame
  f.getContentPane.add(tree.peer)
  f.setVisible(true)
}