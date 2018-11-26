package me.w1992wishes.spark.etl.job2

import com.alibaba.fastjson.annotation.JSONField

/**
  * 抓拍角度
  *
  * @author w1992wishes 2018/10/16 16:43
  */
class XPose {
  /**
    * Pitch，上下俯仰角度
    */
  private var Pitch = .0f

  /**
    * 人脸平面旋转
    */
  private var Roll = .0f

  /**
    * Yaw，左右偏离角度
    */
  private var Yaw = .0f

  @JSONField(name = "Pitch") def getPitch: Float = Pitch

  def setPitch(pitchAngle: Float): Unit = {
    Pitch = pitchAngle
  }

  @JSONField(name = "Roll") def getRoll: Float = Roll

  def setRoll(rollAngle: Float): Unit = {
    Roll = rollAngle
  }

  @JSONField(name = "Yaw") def getYaw: Float = Yaw

  def setYaw(yawAngle: Float): Unit = {
    Yaw = yawAngle
  }

  override def toString: String = "FacePose(Pitch:" + Pitch + ", Roll:" + Roll + ", Yaw:" + Yaw + ")"
}
