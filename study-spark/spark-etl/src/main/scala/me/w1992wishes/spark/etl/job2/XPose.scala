package me.w1992wishes.spark.etl.job2

import com.alibaba.fastjson.annotation.JSONField

/**
  * 抓拍角度
  *
  * @author w1992wishes 2018/10/16 16:43
  */
class XPose {
  /**
    * PitchAngle，上下偏离角度
    */
  private var PitchAngle = .0f

  /**
    * 不详
    */
  private var RollAngle = .0f

  /**
    * YawAngle，左右偏离角度
    */
  private var YawAngle = .0f

  @JSONField(name = "PitchAngle") def getPitchAngle: Float = PitchAngle

  def setPitchAngle(pitchAngle: Float): Unit = {
    PitchAngle = pitchAngle
  }

  @JSONField(name = "RollAngle") def getRollAngle: Float = RollAngle

  def setRollAngle(rollAngle: Float): Unit = {
    RollAngle = rollAngle
  }

  @JSONField(name = "YawAngle") def getYawAngle: Float = YawAngle

  def setYawAngle(yawAngle: Float): Unit = {
    YawAngle = yawAngle
  }

  override def toString: String = "FacePose(PitchAngle:" + PitchAngle + ", RollAngle:" + RollAngle + ", YawAngle:" + YawAngle + ")"
}
