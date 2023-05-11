package modules

import enumeratum.EnumEntry.Lowercase
import enumeratum.{EnumEntry, PlayEnum}
import javax.inject.Inject
import models.AssertionRules
import models.Reports.{Build, Report}
import modules.parsers.{BrowsertimeLogParser, GatlingLogParser, PhantomLogParser}

import scala.collection.immutable
import scala.concurrent.Future

sealed trait LogType extends EnumEntry

object LogType extends PlayEnum[LogType] {

  case object Browsertime extends LogType with Lowercase

  case object Gatling extends LogType with Lowercase

  case object Phantom extends LogType with Lowercase

  val values: immutable.IndexedSeq[LogType] = findValues
}

class LogsModule @Inject() (val reportsModule: ReportsModule) {

  def parse(
      projectId: Int,
      logType: LogType,
      extended: Boolean,
      reportId: Option[Long],
      data: Seq[String],
      providedReport: Option[Report],
      providedBuild: Option[Build],
      assertionRules: AssertionRules
  ): Future[Report] = {
    val parser = logType match {
      case LogType.Browsertime => BrowsertimeLogParser(data)
      case LogType.Gatling     => GatlingLogParser(data, extended)
      case LogType.Phantom     => PhantomLogParser(data)
    }

    val report = parser
      .toReport(providedReport, providedBuild)
      .copy(id = providedReport.flatMap(_.id), projectId = Some(projectId))

    reportId
      .map { rid =>
        reportsModule.saveBuilds(
          rid,
          report.builds.getOrElse(Seq.empty),
          assertionRules: AssertionRules
        )
      }
      .getOrElse(
        reportsModule.saveReport(report, assertionRules: AssertionRules)
      )
  }
}
