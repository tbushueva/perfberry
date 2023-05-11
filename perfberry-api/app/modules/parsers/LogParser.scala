package modules.parsers

import models.Reports.{Build, Report}

trait LogParser {

  def toReport(report: Option[Report], build: Option[Build]): Report
}
