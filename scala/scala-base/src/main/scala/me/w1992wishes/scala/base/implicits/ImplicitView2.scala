package me.w1992wishes.scala.base.implicits

/**
  * @author w1992wishes 2019/7/10 16:01
  */
object ImplicitView2 {

  class Horse {
    def drinking(): Unit = {
      println("I can drinking")
    }
  }

  class Crow {}

  object drinking {
    // 隐式方法调用
    implicit def extendSkill(c: Crow): Horse = new Horse()
  }

  def main(args: Array[String]): Unit = {
    // 隐式转换调用类中不存在的方法
    import drinking._
    val crow = new Crow()
    crow.drinking()
  }

}
