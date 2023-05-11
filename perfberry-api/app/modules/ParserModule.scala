package modules

import com.tdunning.math.stats.AVLTreeDigest
import models.Metrics.MetricValue
import models.Reports.Report

@Deprecated
trait Parser {

  def parse(body: String, env: Option[String], extended: Boolean): Report
}

class StatCalculator {

  def round(value: MetricValue): MetricValue =
    Math.round(value * 100) / 100.0

  def min(numbers: Seq[MetricValue]): MetricValue = {
    round(numbers.min)
  }

  def max(numbers: Seq[MetricValue]): MetricValue = {
    round(numbers.max)
  }

  def avg(numbers: Seq[MetricValue]): MetricValue = {
    round(numbers.sum / numbers.length)
  }

  def percentile(numbers: Seq[MetricValue], perc: Double): MetricValue = {
    val digest = new AVLTreeDigest(100.0)
    numbers.foreach(digest.add)
    round(digest.quantile(perc / 100))
  }

  /** https://en.wikipedia.org/wiki/Standard_deviation
    */
  def stdDev(numbers: Seq[MetricValue]): MetricValue = {
    val average = avg(numbers)

    val deviations = numbers.map { number =>
      val deviation = number - average
      Math.pow(deviation, 2)
    }

    val variance = avg(deviations)

    round(Math.sqrt(variance))
  }
}
