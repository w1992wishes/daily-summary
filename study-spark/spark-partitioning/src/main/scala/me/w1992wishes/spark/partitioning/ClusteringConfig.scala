package me.w1992wishes.spark.partitioning

import com.beust.jcommander.Parameter

class ClusteringConfig {

  @Parameter(names = Array("-datanum", "--datanum"), description = "data row number")
  var dataNum: Integer = null

  @Parameter(names = Array("-datadim", "--datadim"), description = "data row dim")
  var dataDim: Integer = null

  @Parameter(names = Array("-datapartitioncount", "--datapartitioncount"), description = "data partition count")
  var dataPartitionCount: Integer = null

  @Parameter(names = Array("-kmenask", "--kmenask"), description = "kmeans k")
  var kmeansk: Integer = null

}