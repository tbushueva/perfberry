package modules.parsers

import java.time.{LocalDateTime, ZoneOffset}

import models.Metrics._
import models.Reports.{Build, Report, ScmInfo}
import models.{Metrics, Transaction}

/** Format:
  * time, tag, interval_real, connect_time, send_time, latency, receive_time, interval_event, size_out, size_in, net_code, proto_code
  *
  * Successful request example row:
  * 1505186790.135	#6764	13704	9917	123	3373	291	5207	81	838	0	200
  *
  * Failed request example row:
  * 1505186780.095	#6262	11002041	11002041	0	0	0	11000000	0	0	110	0
  */
case class PhantomRow(
    time: String,
    tag: String,
    intervalReal: Long,
    connectTime: Long,
    sendTime: Long,
    latency: Long,
    receiveTime: Long,
    sizeOut: Long,
    sizeIn: Long,
    netCode: Int,
    protoCode: Int
)

object PhantomRow {

  def apply(row: String): PhantomRow = {
    val parts = row.split("\t")

    PhantomRow(
      parts(0),
      parts(1),
      parts(2).toLong,
      parts(3).toLong,
      parts(4).toLong,
      parts(5).toLong,
      parts(6).toLong,
      parts(8).toLong,
      parts(9).toLong,
      parts(10).toInt,
      parts(11).toInt
    )
  }
}

case class PhantomLogParser(files: Seq[String]) extends LogParser {

  override def toReport(
      providedReport: Option[Report],
      providedBuild: Option[Build]
  ): Report = {
    val bodyLines = files.mkString.split("\n")

    val rows = bodyLines.map(PhantomRow(_))

    val transactions = rows.map { row =>
      val metrics = Map(
        Metrics.ResponseTime -> row.intervalReal / 1000.0,
        Metrics.ConnectTime  -> row.connectTime / 1000.0,
        Metrics.SendTime     -> row.sendTime / 1000.0,
        Metrics.Latency      -> row.latency / 1000.0,
        Metrics.ReceiveTime  -> row.receiveTime / 1000.0,
        Metrics.Egress       -> row.sizeOut / 1048576.0,
        Metrics.Ingress      -> row.sizeIn / 1048576.0
      )

      val (status, error) = (row.netCode, row.protoCode) match {
        case (n, 0)             => ("KO", Some(s"Net code $n"))
        case (n, p) if p >= 500 => ("KO", Some(s"Net code $n"))
        case (n, _) if n != 0   => ("KO", Some(s"Net code $n"))
        case _                  => ("OK", None)
      }

      val group = row.tag match {
        // skip auto tags with incremented number of requests such as #1..#N
        case s if s.startsWith("#") => None
        case s if s.trim.isEmpty    => None
        case s                      => Some(s)
      }

      val timeParts = row.time.split("\\.")

      Transaction(
        group,
        None,
        None,
        LocalDateTime.ofEpochSecond(
          timeParts(0).toLong,
          timeParts(1).toInt * 1000000,
          ZoneOffset.UTC
        ),
        status,
        Some(row.protoCode),
        error,
        metrics
      )
    }.toSeq

    val statistics =
      Metrics.statistics(transactions, PhantomLogParser.defaultMetricSet)

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

object PhantomLogParser {

  val defaultMetricSet: MetricSelectors = MetricSelectors(
    Map(
      Transactions -> Seq(Sum),
      Throughput   -> Seq(Avg),
      Errors       -> Seq(Sum),
      ErrorRatio   -> Seq(Sum),
      ResponseTime -> Seq(Max, P99, P95, P75, Median, Min, Avg, StdDev),
      ConnectTime  -> Seq(Avg),
      SendTime     -> Seq(Avg),
      Latency      -> Seq(Avg),
      ReceiveTime  -> Seq(Avg),
      Egress       -> Seq(Sum),
      Ingress      -> Seq(Sum)
    )
  )
}
