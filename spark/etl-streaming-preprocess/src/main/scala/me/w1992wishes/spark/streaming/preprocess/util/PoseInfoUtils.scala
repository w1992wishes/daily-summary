package me.w1992wishes.spark.streaming.preprocess.util

import me.w1992wishes.spark.streaming.preprocess.model.PoseInfo


/**
  * pose utils
  *
  * @author w1992wishes 2018/10/16 16:56
  */
object PoseInfoUtils {

  private val DEFAULT_PITCH_ANGLE = 8f

  private val DEFAULT_ROLL_ANGLE = 16f

  private val DEFAULT_YAW_ANGLE = 8f

  /**
    * 判断角度是否在默认阈值内
    *
    * @param poseInfo 角度
    * @return
    */
  def inAngle(poseInfo: PoseInfo): Boolean =
    inAngle(poseInfo, DEFAULT_PITCH_ANGLE, DEFAULT_ROLL_ANGLE, DEFAULT_YAW_ANGLE)

  /**
    * 判断角度是否在阈值内
    *
    * @param poseInfo 角度属性
    * @param pitchAngle pitch angle
    * @param rollAngle roll angle
    * @param yawAngle yow angle
    * @return
    */
  def inAngle(poseInfo: PoseInfo, pitchAngle: Float, rollAngle: Float, yawAngle: Float): Boolean =
    Math.abs(poseInfo.getPitch) <= pitchAngle &&
      Math.abs(poseInfo.getRoll) <= rollAngle &&
      Math.abs(poseInfo.getYaw) <= yawAngle &&
      !isAllZeroAngle(poseInfo)

  /**
    * 判断角度是否不在默认阈值内
    *
    * @param poseInfo 角度属性
    * @return
    */
  def notInAngle(poseInfo: PoseInfo): Boolean =
    notInAngle(poseInfo, DEFAULT_PITCH_ANGLE, DEFAULT_ROLL_ANGLE, DEFAULT_YAW_ANGLE)

  /**
    * 判断角度是否不在阈值内
    *
    * @param poseInfo 角度属性
    * @param pitchAngle pitch angle
    * @param rollAngle roll angle
    * @param yawAngle yow angle
    * @return
    */
  def notInAngle(poseInfo: PoseInfo, pitchAngle: Float, rollAngle: Float, yawAngle: Float): Boolean =
    !inAngle(poseInfo, pitchAngle, rollAngle, yawAngle)

  /**
    * 判断是否都是 0 角度， 都为 0 角度识别特别差
    *
    * @param poseInfo 角度属性
    * @return
    */
  def isAllZeroAngle(poseInfo: PoseInfo): Boolean =
    (poseInfo.getYaw.toInt == 0) &&
      (poseInfo.getPitch.toInt == 0) &&
      (poseInfo.getRoll.toInt == 0)
}
