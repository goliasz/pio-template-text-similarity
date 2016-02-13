package org.template.similarity

/*
 * Copyright KOLIBERO under one or more contributor license agreements.  
 * KOLIBERO licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import io.prediction.controller.P2LAlgorithm
import io.prediction.controller.Params
import org.apache.spark.SparkContext
import org.apache.spark.SparkContext._
import org.apache.spark.rdd.RDD
import org.apache.spark.mllib.linalg.Vector
import org.apache.spark.mllib.linalg.Vectors
import org.apache.spark.mllib.feature.{Word2Vec,Word2VecModel}
import grizzled.slf4j.Logger
import org.apache.spark.mllib.feature.Normalizer
import org.apache.spark.mllib.linalg.DenseVector

case class AlgorithmParams(
  val seed: Int,
  val minCount: Int,
  val learningRate: Double,
  val numIterations: Int,
  val vectorSize: Int,
  val minTokenSize: Int,
  val showText: Boolean,
  val showDesc: Boolean,
  val useExtTrainWords: Boolean
) extends Params

class TSModel(
  val word2VecModel: Word2VecModel,
  val docPairs: List[((String,String,String,String), breeze.linalg.DenseVector[Double])],
  val vectorSize: Int,
  val showText: Boolean,
  val showDesc: Boolean
) extends Serializable {}

class TextSimilarityAlgorithm(val ap: AlgorithmParams) extends P2LAlgorithm[PreparedData, TSModel, Query, PredictedResult] {

  @transient lazy val logger = Logger[this.type]

  def train(sc: SparkContext, data: PreparedData): TSModel = {
    println("Training text similarity model.")

    val art1 = data.docs.map(x=>((x._2+{if (useExtTrainWords) " "+x._3 else ""}).toLowerCase.replace("."," ").split(" ").filter(k => !stopwords.contains(k)).map(normalizet).filter(_.trim.length>=ap.minTokenSize).toSeq, (x._1,x._2,x._3,x._4))).filter(_._1.size>0)
    
    val word2vec = new Word2Vec()
    word2vec.setSeed(ap.seed)
    word2vec.setMinCount(ap.minCount)
    word2vec.setLearningRate(ap.learningRate)
    word2vec.setNumIterations(ap.numIterations)
    word2vec.setVectorSize(ap.vectorSize)	
	
    val model = word2vec.fit(art1.map(_._1).cache)

    val art_pairs = art1.map(x => (x._2, new DenseVector(divArray(x._1.map(m => wordToVector(m, model, ap.vectorSize).toArray).reduceLeft(sumArray),x._1.length)).asInstanceOf[Vector]))	

    val normalizer1 = new Normalizer()
    val art_pairsb = art_pairs.map(x=>(x._1, normalizer1.transform(x._2))).map(x=>(x._1,{new breeze.linalg.DenseVector(x._2.toArray)}))	

    new TSModel(model, art_pairsb.collect.toList, ap.vectorSize, ap.showText, ap.showDesc)
  }

  def predict(model: TSModel, query: Query): PredictedResult = {
    //Prepare query vector
    val td02 = query.doc.split(" ").filter(k => !stopwords.contains(k)).map(normalizet).filter(_.trim.length>1).toSeq
    val td02w2v = new DenseVector(divArray(td02.map(m => wordToVector(m, model.word2VecModel, model.vectorSize).toArray).reduceLeft(sumArray),td02.length)).asInstanceOf[Vector]
    val normalizer1 = new Normalizer()
    val td02w2vn = normalizer1.transform(td02w2v)
    val td02bv = new breeze.linalg.DenseVector(td02w2vn.toArray)
        
    val r = model.docPairs.map(x=>(td02bv.dot(x._2),x._1)).sortWith(_._1>_._1).take(query.limit).map(x=>{new DocScore(x._1, x._2._1, if(model.showText) x._2._2 else "", if (model.showDesc) x._2._4 else "")})
 
    PredictedResult(docScores = r.toArray)
  }

  def sumArray (m: Array[Double], n: Array[Double]): Array[Double] = {
    for (i <- 0 until m.length) {m(i) += n(i)}
    return m
  }

  def divArray (m: Array[Double], divisor: Double) : Array[Double] = {
    for (i <- 0 until m.length) {m(i) /= divisor}
    return m
  }

  def wordToVector (w:String, m: Word2VecModel, s: Int): Vector = {
    try {
      return m.transform(w)
    } catch {
      case e: Exception => return Vectors.zeros(s)
    }  
  }

  def normalizet(line: String) = java.text.Normalizer.normalize(line,java.text.Normalizer.Form.NFKD).replaceAll("\\p{InCombiningDiacriticalMarks}+","").toLowerCase

  val stopwords = Array("foo").toSet
}
