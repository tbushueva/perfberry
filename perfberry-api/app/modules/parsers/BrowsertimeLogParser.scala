package modules.parsers

import java.net.URI
import java.time.LocalDateTime

import models.Metrics._
import models.Reports.{Build, Report, ScmInfo}
import models.{Metrics, Transaction}
import play.api.libs.functional.syntax._
import play.api.libs.json.Reads._
import play.api.libs.json._

case class VisualMetrics(
    speedIndex: Int,
    firstVisualChange: Int,
    lastVisualChange: Int,
    visualComplete95: Int
)

object VisualMetrics {

  implicit val jsonReads: OFormat[VisualMetrics] = (
    (JsPath \ "SpeedIndex").format[Int] and
      (JsPath \ "FirstVisualChange").format[Int] and
      (JsPath \ "LastVisualChange").format[Int] and
      (JsPath \ "VisualComplete95").format[Int]
  )(VisualMetrics.apply, unlift(VisualMetrics.unapply))
}

case class NavigationTiming(domComplete: Int, domInteractive: Int)

object NavigationTiming {

  implicit val jsonReads: Reads[NavigationTiming] =
    Json.reads[NavigationTiming]
}

case class Timings(firstPaint: Double, navigationTiming: NavigationTiming)

object Timings {
  implicit val jsonReads: Reads[Timings] = Json.reads[Timings]
}

case class BrowserScript(timings: Timings, visualMetrics: VisualMetrics)

object BrowserScript {
  implicit val jsonReads: Reads[BrowserScript] = Json.reads[BrowserScript]
}

case class Info(url: String, timestamp: LocalDateTime)

object Info {
  implicit val jsonFormat: Reads[Info] = Json.reads[Info]
}

case class Browsertime(
    timestamps: Seq[LocalDateTime],
    browserScripts: Seq[BrowserScript],
    info: Info
)

object Browsertime {
  implicit val jsonFormat: Reads[Browsertime] = Json.reads[Browsertime]
}

case class BrowsertimeLogParser(
    browsertimes: Seq[Browsertime],
    foo: String = "bar"
) extends LogParser {

  override def toReport(
      providedReport: Option[Report],
      providedBuild: Option[Build]
  ): Report = {
    val transactions = browsertimes.flatMap { browsertime =>
      browsertime.timestamps
        .zip(browsertime.browserScripts)
        .map { ts =>
          val metrics = Map(
            SpeedIndex        -> ts._2.visualMetrics.speedIndex.toDouble,
            TTFP              -> ts._2.timings.firstPaint,
            DOMComplete       -> ts._2.timings.navigationTiming.domComplete.toDouble,
            DOMInteractive    -> ts._2.timings.navigationTiming.domInteractive.toDouble,
            FirstVisualChange -> ts._2.visualMetrics.firstVisualChange.toDouble,
            LastVisualChange  -> ts._2.visualMetrics.lastVisualChange.toDouble,
            VisualComplete95  -> ts._2.visualMetrics.visualComplete95.toDouble
          )
          val group = new URI(browsertime.info.url).getPath match {
            case ""  => "Index"
            case "/" => "Index"
            case p =>
              p.split("/").filter(_.nonEmpty).map(_.capitalize).mkString(" ")
          }
          Transaction(
            Some(group),
            Some(browsertime.info.url),
            None,
            ts._1,
            "OK",
            None,
            None,
            metrics
          )
        }
    }

    val statistics =
      Metrics.statistics(transactions, BrowsertimeLogParser.defaultMetricSet)

    val build = Build(
      None,
      providedBuild.flatMap(_.reportId),
      providedBuild.map(_.env).getOrElse(Build.defaultEnv),
      providedBuild.flatMap(_.label),
      providedBuild.flatMap(_.description),
      providedBuild.map(_.scm).getOrElse(ScmInfo.empty),
      providedBuild.map(_.links).getOrElse(Seq.empty),
      Some(statistics),
      None,
      Some(transactions),
      None,
      providedBuild.flatMap(_.createdAt)
    )

    Report(
      None,
      None,
      providedReport.flatMap(_.label),
      providedReport.flatMap(_.description),
      providedReport.flatMap(_.scm),
      providedReport.map(_.links).getOrElse(Seq.empty),
      providedReport.flatMap(_.createdAt),
      Some(Seq(build)),
      None
    )
  }
}

object BrowsertimeLogParser {

  def apply(data: Seq[String]): BrowsertimeLogParser =
    BrowsertimeLogParser(data.map(Json.parse(_).as[Browsertime]))

  val defaultMetricSet: MetricSelectors = {
    val selectors = Seq(Max, P90, Median, Min, Avg, StdDev)
    MetricSelectors(
      Map(
        SpeedIndex        -> selectors,
        TTFP              -> selectors,
        DOMComplete       -> selectors,
        DOMInteractive    -> selectors,
        FirstVisualChange -> selectors,
        LastVisualChange  -> selectors,
        VisualComplete95  -> selectors
      )
    )
  }
}
