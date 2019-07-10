package me.w1992wishes.scala.base.implicits

/**
  * @author w1992wishes 2019/7/10 15:54
  */
object ImplicitView1 {

  def main(args: Array[String]): Unit = {

    // 隐式类型转换
    implicit def double2Int(d: Double): Int = d.toInt

    // 变量i申明为Int类型，但是赋值Double类型数据，显然编译通不过。
    // 这个时候可以借助隐式类型转换，定义Double转Int规则，编译器就会自动查找该隐式转换，将3.5转换成3，从而达到编译器自动修复效果。
    val i: Int = 3.5
    println(i)
  }
  
}
