package modules

import java.time.{LocalDateTime, ZoneOffset}

import models.Metrics.MetricSelectors
import models.Reports.{Build, Report, ScmInfo}
import models.{Metrics, Transaction}

@Deprecated
class GatlingParser extends Parser {

  override def parse(
      body: String,
      env: Option[String],
      extended: Boolean
  ): Report = {
    val bodyLines = body.split("\n")

    /** Run simulation example row:
      * RUN\tMySim\t\tmysim\t1476850558062\tMy description\t2.0
      */
    val runDescription = bodyLines.find(_.startsWith("RUN")).get.split("\t")(5)
    val label          = if (runDescription != " ") Some(runDescription) else None

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
      label,
      None,
      None,
      Seq.empty,
      None,
      Some(Seq(build)),
      None
    )
  }
}
