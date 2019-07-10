package me.w1992wishes.scala.base.implicits

/**
  * @author w1992wishes 2019/7/10 14:41
  */
object ImplicitParam {

  def foo(amount: Float)(implicit rate: Float): Unit = {
    println(amount * rate)
  }

  def main(args: Array[String]): Unit = {
    // 隐式参数
    implicit val r: Float = 0.13F // 定义隐式变量
    foo(10) // 输出1.3
  }

  trait Adder[T] {
    def add(x: T, y: T): T
  }

  implicit val a: Adder[Int] = new Adder[Int] {
    override def add(x: Int, y: Int): Int = {
      println(x + y)
      x + y
    }
  }

  def addTest(x: Int, y: Int)(implicit adder: Adder[Int]): Int = {
    adder.add(x, y)
  }

  addTest(1, 2) // 正确, = 3
  addTest(1, 2)(a) // 正确, = 3
  addTest(1, 2)(new Adder[Int] {
    override def add(x: Int, y: Int): Int = {
      println(x - y)
      x - y
    }
  }) // 同样正确, = -1
}
