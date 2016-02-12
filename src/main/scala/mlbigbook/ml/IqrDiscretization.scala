package mlbigbook.ml

import breeze.linalg.Vector
import fif.Data
import mlbigbook.math.{ NumericConversion, VectorOpsT }

import scala.language.higherKinds
import scala.reflect.ClassTag

object IqrDiscretization extends RuleProducer {

  val below_min = "below_min"
  val min_q1 = "between_min_inclusive_and_q1_exclusive"
  val q1_median = "between_q1_inclusive_and_median_exclusive"
  val median_q2 = "between_median_inclusive_and_q2_exclusive"
  val q2_max = "between_q2_inclusive_and_max_exclusive"
  val above_or_equal_to_max = "above_or_equal_to_max"

  val iqrDiscretizedValueBases = Seq(
    below_min, min_q1, q1_median, median_q2, q2_max, above_or_equal_to_max
  )

  override def apply[D[_]: Data, V[_] <: Vector[_], N: NumericConversion: MathOps: ClassTag](
    data: D[V[N]]
  )(
    implicit
    vops: VectorOpsT[N, V],
    fs:   FeatureSpace
  ): Seq[Rule[N]] = {

    implicit val _ = NumericConversion[N].numeric
    InterQuartileRange(data)
      .map { fns => iqrRule(fns) }
  }

  def iqrRule[N: Numeric](fns: FiveNumSummary[N]): Rule[N] =
    new Rule[N] {

      val lessThan = implicitly[Numeric[N]].lt _

      override def apply(value: N): String =
        if (lessThan(value, fns.min)) below_min
        else if (lessThan(value, fns.q1)) min_q1
        else if (lessThan(value, fns.median)) q1_median
        else if (lessThan(value, fns.q2)) median_q2
        else if (lessThan(value, fns.max)) q2_max
        else above_or_equal_to_max

      override val discretizedValueBases: Seq[String] =
        iqrDiscretizedValueBases
    }
}

//
//def apply =
//
//
//val fiveNumberSummaries = {
//  implicit val _ = NumericConversion[N].numeric
//  InterQuartileRange(data)
//}
//
//
//    if (fiveNumberSummaries isEmpty)
//      (data.map(_ => Seq.empty[String]), FeatureSpace.empty)
//
//    else {
//
//      val discretizedData: D[Seq[String]] = {
//        val lessThan = NumericConversion[N].numeric.lt _
//        data.map { vector =>
//
//          val valAt = vops.valueAt(vector) _
//
//          val res = new Array[String](fs.size)
//          cfor(0)(_ < fs.size, _ + 1) { fIndex =>
//
//            val value = valAt(fIndex)
//            val fns = fiveNumberSummaries(fIndex)
//
//            res(fIndex) =
//              // ordering of if statements below is _important_ !!
//              if (lessThan(value, fns.min)) below_min
//              else if (lessThan(value, fns.q1)) min_q1
//              else if (lessThan(value, fns.median)) q1_median
//              else if (lessThan(value, fns.q2)) median_q2
//              else if (lessThan(value, fns.max)) q2_max
//              else above_or_equal_to_max
//          }
//          res.toSeq
//
//          //
//          // Equivalent to the following FP code
//          //
//          //          vops.toSeq(vector)
//          //            .zip(fiveNumberSummaries)
//          //            .map {
//          //              case (value, fns) =>
//          //                // ordering of if statements below is _important_ !!
//          //                if (lessThan(value, fns.min)) below_min
//          //                else if (lessThan(value, fns.q1)) min_q1
//          //                else if (lessThan(value, fns.median)) q1_median
//          //                else if (lessThan(value, fns.q2)) median_q2
//          //                else if (lessThan(value, fns.max)) q2_max
//          //                else above_or_equal_to_max
//          //            }
//        }
//      }
//
//      (
//        discretizedData,
//        Discretization.newCategoricalFs(iqrDiscretizedValueBases)
//      )
//    }