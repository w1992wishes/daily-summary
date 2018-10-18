package me.w1992wishes.spark.etl.util

import me.w1992wishes.spark.etl.job2.XPose

/**
  * pose utils
  *
  * @author w1992wishes 2018/10/16 16:56
  */
object XPoseUtils {

  private val DEFAULT_PITCH_ANGLE = 30f

  private val DEFAULT_ROLL_ANGLE = 60f

  private val DEFAULT_YAW_ANGLE = 30f

  /**
    * 判断角度是否在默认阈值内
    *
    * @param facePose
    * @return
    */
  def isFineAngle(facePose: XPose): Boolean =
    isFineAngle(facePose, DEFAULT_PITCH_ANGLE, DEFAULT_ROLL_ANGLE, DEFAULT_YAW_ANGLE)

  /**
    * 判断角度是否在阈值内
    *
    * @param facePose
    * @param pitchAngle
    * @param rollAngle
    * @param yawAngle
    * @return
    */
  def isFineAngle(facePose: XPose, pitchAngle: Float, rollAngle: Float, yawAngle: Float): Boolean =
    Math.abs(facePose.getPitchAngle) < pitchAngle &&
      Math.abs(facePose.getRollAngle) < rollAngle &&
      Math.abs(facePose.getYawAngle) < yawAngle &&
      !isAllZeroAngle(facePose)

  /**
    * 判断角度是否不在默认阈值内
    *
    * @param facePose
    * @return
    */
  def isBadAngle(facePose: XPose): Boolean =
    isBadAngle(facePose, DEFAULT_PITCH_ANGLE, DEFAULT_ROLL_ANGLE, DEFAULT_YAW_ANGLE)

  /**
    * 判断角度是否不在阈值内
    *
    * @param facePose
    * @param pitchAngle
    * @param rollAngle
    * @param yawAngle
    * @return
    */
  def isBadAngle(facePose: XPose, pitchAngle: Float, rollAngle: Float, yawAngle: Float): Boolean =
    !isFineAngle(facePose, pitchAngle, rollAngle, yawAngle)

  /**
    * 判断是否都是 0 角度， 都为 0 角度识别特别差
    *
    * @param facePose
    * @return
    */
  def isAllZeroAngle(facePose: XPose): Boolean =
    (facePose.getYawAngle.toInt == 0) &&
      (facePose.getPitchAngle.toInt == 0) &&
      (facePose.getRollAngle.toInt == 0)

}
