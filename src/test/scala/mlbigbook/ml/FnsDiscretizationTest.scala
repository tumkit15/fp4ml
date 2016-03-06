package mlbigbook.ml

import breeze.linalg.{ QuasiTensor, DenseVector }
import mlbigbook.math.{ NumericConversion, MathVectorOps }
import org.scalatest.FunSuite

class FnsDiscretizationTest extends FunSuite {

  import FnsDiscretizationTest._
  import fif.ImplicitCollectionsData._
  import MathVectorOps.Implicits._
  import NumericConversion.Implicits._
  import MathOps.Implicits._

  test("testing that five number summary computation is correct") {
    val newFiveNumSums = FiveNumSummary(dataForDiscretization)
    assert(newFiveNumSums.size === 3)
    assert(newFiveNumSums.head === dim0_expectedFiveNumSum)
    assert(newFiveNumSums(1) === dim1_expectedFiveNumSum)
    assert(newFiveNumSums.last === dim2_expectedFiveNumSum)

    (0 until 3)
      .foreach { index =>
        checkDataWithIqr(
          newFiveNumSums(index),
          dataForDiscretization.map((v: QuasiTensor[Int, Int]) => v(index))
        )
      }
  }

  def checkDataWithIqr(fns: FiveNumSummary[Int], data: Seq[Int]) = {

    val (belowMin, minQ1, q1Median, medianQ2, q2Max, aboveMax) =
      data
        .foldLeft((0, 0, 0, 0, 0, 0)) {
          case (counts @ (nBMin, nMinQ1, nQ1Median, nMedianQ2, nQ2Max, nAMax), value) =>
            if (value < fns.min)
              counts.copy(_1 = nBMin + 1)
            else if (value < fns.q1)
              counts.copy(_2 = nMinQ1 + 1)
            else if (value < fns.median)
              counts.copy(_3 = nQ1Median + 1)
            else if (value < fns.q3)
              counts.copy(_4 = nMedianQ2 + 1)
            else if (value < fns.max)
              counts.copy(_5 = nQ2Max + 1)
            else
              counts.copy(_6 = nAMax + 1)
        }

    val expected = data.size / 4

    assert(belowMin === 0, ": below min wrong")
    assert(minQ1 === expected, ": min-q1 wrong")
    assert(q1Median === expected, ": q1-median wrong")
    assert(medianQ2 === expected, ": median-q2 wrong")
    assert(q2Max === expected, ": q2-max wrong")
    assert(aboveMax === data.size / 100, ": above or equal to max wrong")

  }

  test("Testing five number summary based discretization") {

    val (newData, newFs) = {
      implicit val _ = oldFs
      Discretization(dataForDiscretization, FnsDiscretization.ruleProducer[Int])
    }

    assert(newFs.isCategorical.forall(identity))
    assert(newFs.categorical2values.keys.toSet === newFs.features.toSet)

    val newDiscretizedFeatureValues =
      newFs.categorical2values.toSeq
        .map {
          case (featureName, newValues) =>
            (newFs.feat2index(featureName), newValues)
        }
        .sortBy { case (featIndex, _) => featIndex }
        .map { case (_, newValues) => newValues }

    assert(newData.size === dataForDiscretization.size)
    newData.foreach { values =>
      assert(values.size == newDiscretizedFeatureValues.size)
    }
    verifyNewDiscretizedValueIqr(newDiscretizedFeatureValues)
    verifyDataIqr(newData)
  }

  def verifyNewDiscretizedValueIqr(newValuesPerFeature: Seq[Seq[String]]) =
    newValuesPerFeature
      .zipWithIndex
      .foreach {
        case (grouped, index) =>
          assert(grouped.head === s"${FnsDiscretization.below_min}--dimension_$index")
          assert(grouped(1) === s"${FnsDiscretization.min_q1}--dimension_$index")
          assert(grouped(2) === s"${FnsDiscretization.q1_median}--dimension_$index")
          assert(grouped(3) === s"${FnsDiscretization.median_q3}--dimension_$index")
          assert(grouped(4) === s"${FnsDiscretization.q3_max}--dimension_$index")
          assert(grouped.last === s"${FnsDiscretization.above_or_equal_to_max}--dimension_$index")
      }

  // check data
  def verifyDataIqr(data: Seq[Seq[String]]) = {

    val mult = data.head.size
    val expected = (data.size / 4) * mult

    val (belowMin, minQ1, q1Median, medianQ2, q2Max, aboveMax) =
      data
        .foldLeft((0, 0, 0, 0, 0, 0)) {
          case (c, values) =>
            values.foldLeft(c) {
              case (counts @ (nBMin, nMinQ1, nQ1Median, nMedianQ2, nQ2Max, nAMax), value) =>
                if (value.startsWith(FnsDiscretization.below_min))
                  counts.copy(_1 = nBMin + 1)
                else if (value.startsWith(FnsDiscretization.min_q1))
                  counts.copy(_2 = nMinQ1 + 1)
                else if (value.startsWith(FnsDiscretization.q1_median))
                  counts.copy(_3 = nQ1Median + 1)
                else if (value.startsWith(FnsDiscretization.median_q3))
                  counts.copy(_4 = nMedianQ2 + 1)
                else if (value.startsWith(FnsDiscretization.q3_max))
                  counts.copy(_5 = nQ2Max + 1)
                else
                  counts.copy(_6 = nAMax + 1)
            }
        }

    assert(belowMin === 0, s": ${FnsDiscretization.below_min} wrong")
    assert(minQ1 === expected, s": ${FnsDiscretization.min_q1}  wrong")
    assert(q1Median === expected, s": ${FnsDiscretization.q1_median}  wrong")
    assert(medianQ2 === expected, s": ${FnsDiscretization.median_q3}  wrong")
    assert(q2Max === expected, s": ${FnsDiscretization.q3_max}  wrong")
    assert(aboveMax === (data.size / 100) * mult, s": ${FnsDiscretization.above_or_equal_to_max}  wrong")
  }

}

object FnsDiscretizationTest {

  val dim0_expectedFiveNumSum = FiveNumSummary(
    min = -50,
    q1 = -25,
    median = 0,
    q3 = 25,
    max = 50
  )

  val dim1_expectedFiveNumSum = FiveNumSummary(
    min = 0,
    q1 = 25,
    median = 50,
    q3 = 75,
    max = 100
  )

  val dim2_expectedFiveNumSum = FiveNumSummary(
    min = 0,
    q1 = 250,
    median = 500,
    q3 = 750,
    max = 1000
  )

  val dataForDiscretization: Seq[DenseVector[Int]] = {
    (0 to 100)
      .flatMap { value =>
        Seq(
          DenseVector(value - 50, value, value * 10),
          DenseVector(value - 50, value, value * 10),
          DenseVector(value - 50, value, value * 10)
        )
      }
      .map { vector => (vector, math.random) }
      .sortBy { _._2 }
      .map { _._1 }
      .toSeq
  }

  val oldFs = RealFeatureSpace(
    Seq("dimension_0", "dimension_1", "dimension_2")
  )

}