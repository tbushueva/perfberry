package modules.parsers

import java.time.{LocalDateTime, ZoneOffset}

import models.Reports.{Build, Report, ScmInfo}
import models.{Metrics, Transaction}

case class GatlingLogParser(files: Seq[String], extended: Boolean) extends LogParser {

  override def toReport(
      providedReport: Option[Report],
      providedBuild: Option[Build]
  ): Report = {
    val bodyLines = files.mkString.split("\n")

    /** Run simulation example row:
      * RUN\tMySim\t\tmysim\t1476850558062\tMy description\t2.0
      */
    val runDescription = bodyLines
      .find(_.startsWith("RUN"))
      .flatMap { row =>
        row.split("\t") match {
          case parts if parts.isDefinedAt(5) => Some(parts(5).trim)
          case _                             => None
        }
      }
    val label = providedReport.flatMap(_.label).orElse(runDescription)

    val requests = bodyLines.filter(_.startsWith("REQUEST")).map { line =>
      models.Gatling.Request(line, extended)
    }

    val transactions = requests.map { request =>
      val metrics =
        Map(Metrics.ResponseTime -> (request.end - request.start).toDouble)

      val requestStartSeconds = request.start / 1000.0
      val requestStartNanoSeconds = ("%.3f"
        .format(requestStartSeconds % 1)
        .replace(",", ".")
        .toDouble * 1000000000).toInt

      Transaction(
        Some(request.name),
        request.url,
        request.payload,
        LocalDateTime.ofEpochSecond(
          requestStartSeconds.toLong,
          requestStartNanoSeconds,
          ZoneOffset.UTC
        ),
        request.status,
        request.code,
        request.error,
        metrics
      )
    }.toSeq

    val statistics =
      Metrics.statistics(transactions, GatlingLogParser.defaultMetricSet)

    val build =
      Build(
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
      label,
      providedReport.flatMap(_.description),
      providedReport.flatMap(_.scm),
      providedReport.map(_.links).getOrElse(Seq.empty),
      providedReport.flatMap(_.createdAt),
      Some(Seq(build)),
      None
    )
  }
}

object GatlingLogParser {
  import models.Metrics._

  val defaultMetricSet: MetricSelectors = MetricSelectors(
    Map(
      Transactions -> Seq(Sum),
      Throughput   -> Seq(Avg),
      Errors       -> Seq(Sum),
      ErrorRatio   -> Seq(Sum),
      ResponseTime -> Seq(Max, P99, P95, P75, Median, Min, Avg, StdDev)
    )
  )
}
