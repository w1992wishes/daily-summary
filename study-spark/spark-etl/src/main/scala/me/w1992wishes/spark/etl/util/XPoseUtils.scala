package me.w1992wishes.spark.etl.util

/**
  * pose utils
  *
  * @author w1992wishes 2018/10/16 16:56
  */
object XPoseUtils {

  private val DEFAULT_PITCH_ANGLE = 8f

  private val DEFAULT_ROLL_ANGLE = 16f

  private val DEFAULT_YAW_ANGLE = 8f

  /**
    * 判断角度是否在默认阈值内
    *
    * @param facePose 角度
    * @return
    */
  def inAngle(facePose: XPose): Boolean =
    inAngle(facePose, DEFAULT_PITCH_ANGLE, DEFAULT_ROLL_ANGLE, DEFAULT_YAW_ANGLE)

  /**
    * 判断角度是否在阈值内
    *
    * @param facePose 角度属性
    * @param pitchAngle pitch angle
    * @param rollAngle roll angle
    * @param yawAngle yow angle
    * @return
    */
  def inAngle(facePose: XPose, pitchAngle: Float, rollAngle: Float, yawAngle: Float): Boolean =
    Math.abs(facePose.getPitch) <= pitchAngle &&
      Math.abs(facePose.getRoll) <= rollAngle &&
      Math.abs(facePose.getYaw) <= yawAngle &&
      !isAllZeroAngle(facePose)

  /**
    * 判断角度是否不在默认阈值内
    *
    * @param facePose 角度属性
    * @return
    */
  def notInAngle(facePose: XPose): Boolean =
    notInAngle(facePose, DEFAULT_PITCH_ANGLE, DEFAULT_ROLL_ANGLE, DEFAULT_YAW_ANGLE)

  /**
    * 判断角度是否不在阈值内
    *
    * @param facePose 角度属性
    * @param pitchAngle pitch angle
    * @param rollAngle roll angle
    * @param yawAngle yow angle
    * @return
    */
  def notInAngle(facePose: XPose, pitchAngle: Float, rollAngle: Float, yawAngle: Float): Boolean =
    !inAngle(facePose, pitchAngle, rollAngle, yawAngle)

  /**
    * 判断是否都是 0 角度， 都为 0 角度识别特别差
    *
    * @param facePose 角度属性
    * @return
    */
  def isAllZeroAngle(facePose: XPose): Boolean =
    (facePose.getYaw.toInt == 0) &&
      (facePose.getPitch.toInt == 0) &&
      (facePose.getRoll.toInt == 0)
}
