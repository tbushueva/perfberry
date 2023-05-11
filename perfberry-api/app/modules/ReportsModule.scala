package modules

import javax.inject.Inject
import models.AssertionRules
import models.Reports.{Build, Report}
import services.{ProjectService, ReportsService}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class ReportsModule @Inject() (
    projectService: ProjectService,
    reportsService: ReportsService
) {

  def saveReport(
      report: Report,
      assertionRules: AssertionRules
  ): Future[Report] = {
    reportsService
      .createReport(report.copy(builds = None))
      .flatMap { createdReport =>
        if (report.builds.getOrElse(Seq.empty).nonEmpty) {
          saveBuilds(createdReport.id.get, report.builds.get, assertionRules)
        } else Future.successful(createdReport)
      }
  }

  def saveBuilds(
      reportId: Long,
      builds: Seq[Build],
      assertionRules: AssertionRules
  ): Future[Report] = {
    reportsService.report(reportId).flatMap { maybeReport =>
      projectService
        .searches(maybeReport.get.projectId.get)
        .flatMap { searches =>
          projectService
            .apdex(maybeReport.get.projectId.get)
            .flatMap { apdexRules =>
              val futureAssertions = if (assertionRules.items.isEmpty) {
                projectService.assertions(maybeReport.get.projectId.get)
              } else Future.successful(assertionRules)

              futureAssertions.flatMap { selectedAssertionRules =>
                val report = maybeReport.get
                  .copy(builds = Some(builds.map(_.copy(reportId = Some(reportId)))))
                  .withApdex(apdexRules, searches)
                  .withAssertions(selectedAssertionRules, searches)
                reportsService
                  .createBuilds(report.builds.getOrElse(Seq.empty))
                  .map { _ =>
                    if (report.passed.isDefined) {
                      reportsService.updateReport(report)
                    } else Future.successful(())
                  }
                  .map(_ => report.copy(builds = None))
              }
            }
        }
    }
  }
}
