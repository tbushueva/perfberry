package modules

import java.time.{LocalDateTime, ZoneOffset}

import models.Metrics.MetricSelectors
import models.Reports.{Build, Report, ScmInfo}
import models.{Metrics, Transaction}

@Deprecated
class PhantomParser extends Parser {

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

  override def parse(
      body: String,
      env: Option[String],
      extended: Boolean
  ): Report = {
    val bodyLines = body.split("\n")

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

    val statistics = Metrics.statistics(transactions, MetricSelectors.empty)

    val build =
      Build(
        None,
        None,
        env.getOrElse("default"),
        None,
        None,
        ScmInfo.empty,
        Seq.empty,
        Some(statistics),
        None,
        Some(transactions),
        None,
        None
      )

    Report(
      None,
      None,
      None,
      None,
      None,
      Seq.empty,
      None,
      Some(Seq(build)),
      None
    )
  }
}
