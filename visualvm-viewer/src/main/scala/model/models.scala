package model

case class Thread(id: Long, name: String)
case class Method(className: String, name: String, sig: Option[String])

sealed trait Node {
  def value: String
  def children: List[Node]
}

case class RootNode(children: List[Node]) extends Node {
  override def value = "root"
}

case class ThreadsNode(children: List[ThreadNode]) extends Node {
  override def value = s"${children.size} threads"
}

case class ThreadNode(thread: Thread, root: MethodNode) extends Node {
  override def value = s"${thread.id} ${thread.name}"
  override def children = root.children
}

trait MethodNodeTrait extends Node {
  def selfTime: Boolean
  def method: Method
  def times: Times
  override def children: List[MethodNodeTrait]
}

case class Times(totalTime0: Long, totalTime1: Long, sleepTime0: Long, waitTime0: Long) {
  override def toString = {
    assert(sleepTime0==0);
    assert(waitTime0==0);
    val ms = totalTime0/1000
    val cpuPct = 100*totalTime1/totalTime0
    s"$ms ms ($cpuPct %CPU)"
  }
}
object Times {
  def merged(values: List[Times]) = {
    var totalTime0 = 0L
    var totalTime1 = 0L
    var sleepTime0 = 0L
    var waitTime0 = 0L
    values.foreach { v =>
      totalTime0 += v.totalTime0
      totalTime1 += v.totalTime1
      sleepTime0 += v.sleepTime0
      waitTime0 += v.waitTime0
    }
    Times(totalTime0, totalTime1, sleepTime0, waitTime0)
  }
}

case class MethodNode(method: Method, selfTime: Boolean, children: List[MethodNode], times: Times) extends Node with MethodNodeTrait {
  override def value = (if (selfTime) "self time" else s"${method.className}.${method.name}(${method.sig.getOrElse("")})") +
    s" $times"
}

case class MergedThreadsNode(threads: List[ThreadNode]) extends Node {
  override def value = s"${threads.size} threads merged"
  override def children = MergedMethodNodes.from(threads.flatMap(_.children))
}

case class MergedMethodNodes(method: Method, selfTime: Boolean, mergedNodes: List[MethodNodeTrait]) extends Node with MethodNodeTrait {
  override def value = (if (selfTime) "self time" else s"${method.className}.${method.name}(${method.sig.getOrElse("")})") +
    s" (${mergedNodes.size} threads)" +
    s" $times"
  override def children =
    MergedMethodNodes.from(mergedNodes.flatMap(_.children))
  override def times = Times.merged(mergedNodes.map(_.times))
}
object MergedMethodNodes {
  def from(methodNodes: List[MethodNodeTrait]): List[MergedMethodNodes] = {
    methodNodes
      .groupBy(mn => (mn.method, mn.selfTime))
      .map { case ((method, selfTime), nodes) => MergedMethodNodes(method, selfTime, nodes) }
      .toList
  }
}