package org.template.similarity

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
  val vectorSize: Int
) extends Params

class TSModel(
  val word2VecModel: Word2VecModel,
  val docPairs: List[(String, breeze.linalg.DenseVector[Double])],
  val vectorSize: Int
) extends Serializable {

  //override def toString(): String = {
  //  val s = docPairs.take(1)
  //  s.mkString(" ")
  //}
}

class TextSimilarityAlgorithm(val ap: AlgorithmParams) extends P2LAlgorithm[PreparedData, TSModel, Query, PredictedResult] {

  @transient lazy val logger = Logger[this.type]

  def train(sc: SparkContext, data: PreparedData): TSModel = {
    println("Training text similarity model.")

    val art1 = data.docs.map(x=>(x._2.toLowerCase.replace(".","").split(" ").filter(k => !stopwords.contains(k)).map(normalizet).filter(_.trim.length>2).toSeq, x._1))
    
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

    new TSModel(model, art_pairsb.collect.toList, ap.vectorSize)
  }

  def predict(model: TSModel, query: Query): PredictedResult = {
    //Prepare query vector
    val td02 = query.doc.split(" ").filter(k => !stopwords.contains(k)).map(normalizet).filter(_.trim.length>2).toSeq
    val td02w2v = new DenseVector(divArray(td02.map(m => wordToVector(m, model.word2VecModel, model.vectorSize).toArray).reduceLeft(sumArray),td02.length)).asInstanceOf[Vector]
    val normalizer1 = new Normalizer()
    val td02w2vn = normalizer1.transform(td02w2v)
    val td02bv = new breeze.linalg.DenseVector(td02w2vn.toArray)
        
    val r = model.docPairs.map(x=>(td02bv.dot(x._2),x._1)).sortWith(_._1>_._1).take(query.limit).map(x=>{new DocScore(x._1, x._2, "")})
    //val result = Array(new DocScore(0.5, "a", r.count.toString))   

 
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


  val regex = """[^0-9]*""".r

  val stopwords = Array("a", "about", "above", "above", "across", "after", "afterwards", "again", "against", "all", "almost", "alone", "along", "already", "also","although","always","am","among", "amongst", "amoungst", "amount",  "an", "and", "another", "any","anyhow","anyone","anything","anyway", "anywhere", "are", "around", "as",  "at", "back","be","became", "because","become","becomes", "becoming", "been", "before", "beforehand", "behind", "being", "below", "beside", "besides", "between", "beyond", "bill", "both", "bottom","but", "by", "call", "can", "cannot", "cant", "co", "con", "could", "couldnt", "cry", "de", "describe", "detail", "do", "done", "down", "due", "during", "each", "eg", "eight", "either", "eleven","else", "elsewhere", "empty", "enough", "etc", "even", "ever", "every", "everyone", "everything", "everywhere", "except", "few", "fifteen", "fify", "fill", "find", "fire", "first", "five", "for", "former", "formerly", "forty", "found", "four", "from", "front", "full", "further", "get", "give", "go", "had", "has", "hasnt", "have", "he", "hence", "her", "here", "hereafter", "hereby", "herein", "hereupon", "hers", "herself", "him", "himself", "his", "how", "however", "hundred", "ie", "if", "in", "inc", "indeed", "interest", "into", "is", "it", "its", "itself", "keep", "last", "latter", "latterly", "least", "less", "ltd", "made", "many", "may", "me", "meanwhile", "might", "mill", "mine", "more", "moreover", "most", "mostly", "move", "much", "must", "my", "myself", "name", "namely", "neither", "never", "nevertheless", "next", "nine", "no", "nobody", "none", "noone", "nor", "not", "nothing", "now", "nowhere", "of", "off", "often", "on", "once", "one", "only", "onto", "or", "other", "others", "otherwise", "our", "ours", "ourselves", "out", "over", "own","part", "per", "perhaps", "please", "put", "rather", "re", "same", "see", "seem", "seemed", "seeming", "seems", "serious", "several", "she", "should", "show", "side", "since", "sincere", "six", "sixty", "so", "some", "somehow", "someone", "something", "sometime", "sometimes", "somewhere", "still", "such", "system", "take", "ten", "than", "that", "the", "their", "them", "themselves", "then", "thence", "there", "thereafter", "thereby", "therefore", "therein", "thereupon", "these", "they", "thickv", "thin", "third", "this", "those", "though", "three", "through", "throughout", "thru", "thus", "to", "together", "too", "top", "toward", "towards", "twelve", "twenty", "two", "un", "under", "until", "up", "upon", "us", "very", "via", "was", "we", "well", "were", "what", "whatever", "when", "whence", "whenever", "where", "whereafter", "whereas", "whereby", "wherein", "whereupon", "wherever", "whether", "which", "while", "whither", "who", "whoever", "whole", "whom", "whose", "why", "will", "with", "within", "without", "would", "yet", "you", "your", "yours", "yourself", "yourselves", "the").toSet

}
