package org.template.similarity

import io.prediction.controller.{Engine,EngineFactory}

case class Query(
  val doc: String,
  val limit: Int
) extends Serializable

case class PredictedResult(
  docScores: Array[DocScore]
) extends Serializable {
  override def toString: String = docScores.mkString(",")
}

case class DocScore(
  score: Double,
  id: String,
  text: String	
) extends Serializable

object TextSimilarityEngine extends EngineFactory {
  def apply() = {
    new Engine(
      classOf[DataSource],
      classOf[Preparator],
      Map("tsimilarity" -> classOf[TextSimilarityAlgorithm]),
      	classOf[Serving])
  }
}
