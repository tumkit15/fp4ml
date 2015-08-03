package mlbigbook.ml

import org.scalatest.FunSuite

class CountingNaiveBayesTest extends FunSuite {

  import CountingNaiveBayesTest._

  test("test using small vocabulary, words") {

    val nb = NaiveBayesModule(CountingNaiveBayes.Int.produce(training))

    smallVocabData
      .map { x =>
        println(x)
        nb.estimate(x)
      }
      .foreach(println)

  }

}

object CountingNaiveBayesTest {

  import mlbigbook.data.Data
  import Data._

  val smallVocabData: Data[Feature.Vector[String]] =
    Seq(
      "tom hello world how are you today".split(" "),
      "how hello today you are world tom".split(" ")
    )
      .map(array2Data)

  val smallVocabLabels: Data[String] =
    Seq(
      "positive",
      "negative"
    )

  val training: Learning[Feature.Vector[String], String]#TrainingData =
    smallVocabData.zip(smallVocabLabels)

}