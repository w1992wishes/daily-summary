package me.w1992wishes.scala.base.implicits

/**
  * @author w1992wishes 2019/7/10 16:21
  */
object ImplicitClass {

  class Crow {}

  object crow_eval {

    implicit class Parrot(animal: Crow) {
      def say(say: String): Unit = {
        println(s"I have the skill of Parrot: $say")
      }
    }

  }

  def main(args: Array[String]): Unit = {
    // 隐式类
    import crow_eval._
    val crow: Crow = new Crow
    crow.say("balabala")
  }

}
