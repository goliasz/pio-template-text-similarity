package org.template.similarity

import io.prediction.controller.PDataSource
//import io.prediction.controller.EmptyEvaluationInfo
//import io.prediction.controller.EmptyActualResult
import io.prediction.controller.Params
import io.prediction.data.store.PEventStore
import org.apache.spark.mllib.linalg.Vector
import org.apache.spark.mllib.linalg.Vectors
import org.apache.spark.SparkContext
import org.apache.spark.SparkContext._
import org.apache.spark.rdd.RDD

import grizzled.slf4j.Logger

case class DataSourceParams(appName: String) extends Params

class DataSource(val dsp: DataSourceParams)
  extends PDataSource[TrainingData, EmptyEvaluationInfo, Query, EmptyActualResult] {

  @transient lazy val logger = Logger[this.type]

  override
  def readTraining(sc: SparkContext): TrainingData = {
    println("Gathering data from event server.")
    val docsRDD: RDD[(String,String)] = PEventStore.aggregateProperties(
      appName = dsp.appName,
      entityType = "doc",
      required = Some(List("id","text")))(sc).map { case (entityId, properties) =>
        try {
	  (properties.get[String]("id"), properties.get[String]("text"))
        } catch {
          case e: Exception => {
            logger.error(s"Failed to get properties ${properties} of" +
              s" ${entityId}. Exception: ${e}.")
            throw e
          }
        }
      }
		
    new TrainingData(docsRDD)
  }
}

class TrainingData(
  val docs: RDD[(String, String)]
) extends Serializable
