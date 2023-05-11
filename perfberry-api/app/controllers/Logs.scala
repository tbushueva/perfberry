package controllers

import javax.inject.Inject
import models.AssertionRules
import models.Reports.{Build, Link, Report}
import modules._
import play.api.libs.json.Json
import play.api.mvc._
import services.{ProjectService, ReportsService}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.io.Source

case class LogBody(
    files: Seq[String],
    report: Option[Report],
    build: Option[Build],
    assertionRules: AssertionRules
)

class Logs @Inject() (
    val gatlingParser: GatlingParser,
    val phantomParser: PhantomParser,
    val projectService: ProjectService,
    val reportsService: ReportsService,
    val reportsModule: ReportsModule,
    val logsModule: LogsModule,
    val bodyParsers: PlayBodyParsers
) extends InjectedController {

  private def parseLogs: BodyParser[LogBody] = {
    bodyParsers.multipartFormData.map { form =>
      val files = form.files.map { file =>
        val bufferedSource = Source.fromFile(file.ref)
        val data           = bufferedSource.mkString

        bufferedSource.close()
        data
      }

      val report = form.dataParts
        .get("report")
        .map(_.mkString)
        .map(Report(_))

      val build = form.dataParts
        .get("build")
        .map(_.mkString)
        .map(Build(_).copy(reportId = report.flatMap(_.id)))

      val assertionRules = form.dataParts
        .get("assertions")
        .map(_.mkString)
        .map(AssertionRules(_))
        .getOrElse(AssertionRules.empty)

      LogBody(files, report, build, assertionRules)
    }
  }

  def parseLogV2(
      projectId: Int,
      logType: LogType,
      extended: Option[Boolean],
      reportId: Option[Long]
  ): Action[LogBody] =
    Action(parseLogs).async { request =>
      logsModule
        .parse(
          projectId,
          logType,
          extended.getOrElse(false),
          reportId,
          request.body.files,
          request.body.report,
          request.body.build,
          request.body.assertionRules
        )
        .map { createdReport =>
          val location =
            Locations.report(createdReport.projectId.get, createdReport.id.get)
          Created(Json.toJson(createdReport))
            .withHeaders("Location" -> location)
        }
    }

  def parseLog(
      projectId: Int,
      logType: String,
      label: Option[String],
      env: Option[String],
      extended: Option[Boolean],
      links: Seq[String]
  ): Action[AnyContent] = Action.async { request =>
    val reportLinks = links.map { linkString =>
      val parts = linkString.split(",", 2)
      Link(parts(1), parts(0))
    }
    val body = request.body.asText

    body match {
      case None =>
        Future.successful(BadRequest(Json.obj("error" -> "body is empty")))
      case Some(b) =>
        projectService.searches(projectId).flatMap { searches =>
          projectService.apdex(projectId).flatMap { apdexRules =>
            val maybeFullReport = logType match {
              case "gatling" =>
                Some(
                  gatlingParser
                    .parse(b, env, extended.getOrElse(false))
                )
              case "phantom" =>
                Some(
                  phantomParser
                    .parse(b, env, extended.getOrElse(false))
                )
              case _ => None
            }

            maybeFullReport match {
              case Some(fullReport) =>
                val fullReachedReport = fullReport
                  .copy(projectId = Some(projectId), links = reportLinks)
                  .withLabel(label)

                val report = fullReachedReport
                  .copy(builds = None)

                val build =
                  fullReachedReport
                    .withApdex(apdexRules, searches)
                    .builds
                    .get
                    .head

                for {
                  createdReport <- reportsService.createReport(report)
                  createdBuild <- reportsService
                    .createBuild(build.copy(reportId = createdReport.id))
                  _ <- reportsService.createTransactions(
                    createdBuild.id.get,
                    build.transactions.getOrElse(Seq.empty)
                  )
                } yield {
                  Created(Json.toJson(createdReport))
                    .withHeaders(
                      "Location" ->
                        controllers.routes.Reports
                          .report(
                            createdReport.projectId.get,
                            createdReport.id.get
                          )
                          .url
                    )
                }
              case None =>
                Future.successful(
                  BadRequest(Json.obj("error" -> "incorrect or empty log type"))
                )
            }
          }
        }
    }
  }
}
