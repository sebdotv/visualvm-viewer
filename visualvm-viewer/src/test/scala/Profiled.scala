import Thread.sleep

object Profiled extends App {

  val runnable = new Runnable {
    override def run = {
      for (j <- 1 to 100) a
    }
    private def a = {
      sleep(150)
      b1
      b2
    }
    private def b1 = {
      sleep(200)
      c
    }
    private def b2 = {
      sleep(400)
      c
    }
    private def c = {
      sleep(1000)
    }
  }

  for (i <- 1 to 10) {
    new Thread(runnable, "Thread-" + i).start
  }
}