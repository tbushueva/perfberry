package controllers

import java.time.{LocalDateTime, ZoneOffset}

import javax.inject.Inject
import models.Metrics.{
  Graph,
  GraphFilters,
  HistorySettings,
  MetricSelectors,
  Series,
  Statistics,
  findMetric
}
import models.Projects.Settings
import models.Reports.Report
import models._
import models.payloads.BuildPayload
import modules.{HistoryModule, StatCalculator}
import play.api.libs.json._
import play.api.mvc._
import services.{ProjectService, ReportsService}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class Reports @Inject() (
    val projectsService: ProjectService,
    val reportsService: ReportsService,
    val historyModule: HistoryModule
) extends InjectedController {

  def list(
      projectId: Int,
      query: Option[SearchQuery],
      from: Option[String],
      to: Option[String],
      limit: Option[Int],
      offset: Option[Int]
  ): Action[AnyContent] =
    Action.async {
      val maybeToDate   = to.map(LocalDateTime.parse)
      val maybeFromDate = from.map(LocalDateTime.parse)
      for {
        reports <- reportsService.list(
          projectId,
          query,
          maybeFromDate,
          maybeToDate,
          limit.getOrElse(50),
          offset.getOrElse(0)
        )
      } yield Ok(Json.toJson(reports))
    }

  def history(
      projectId: Int,
      lines: Seq[String],
      from: Option[String],
      to: Option[String],
      limit: Option[Int],
      offset: Option[Int]
  ) =
    Action.async {
      projectsService
        .overview(projectId)
        .flatMap { overview =>
          projectsService.searches(projectId).map { searches =>
            Settings(searches, overview)
          }
        }
        .flatMap { settings =>
          val lineSettings = if (lines.isEmpty) {
            settings.overview
          } else {
            lines
              .map { line =>
                val parts = line.split(",")

                val (metric, selector) =
                  if (parts.length == 4) (parts(3), None)
                  else (parts(3), Some(parts(4)))

                val searchName = if (parts(0).isEmpty) {
                  None
                } else {
                  Some(parts(0))
                }

                val group = if (parts(2).isEmpty) {
                  None
                } else {
                  Some(parts(2))
                }

                HistorySettings(group, metric, selector, parts(1), searchName)
              }
          }
          val linesRequests = lineSettings.map { hsettings =>
            reportsService
              .list(
                projectId,
                hsettings.searchName.flatMap(n =>
                  settings.searches
                    .find(_.name == n)
                    .map(s => SearchQuery(s.query))
                ),
                from.map(LocalDateTime.parse),
                to.map(LocalDateTime.parse),
                limit.getOrElse(75),
                offset.getOrElse(0)
              )
              .flatMap { reports =>
                reportsService
                  .builds(reports.flatMap(_.id))
                  .flatMap { builds =>
                    reportsService
                      .statistics(builds.flatMap(_.id))
                      .flatMap { statistics =>
                        reportsService
                          .assertions(builds.flatMap(_.id))
                          .map { assertions =>
                            val reportsWithBuilds = reports.map { report =>
                              val newBuilds = builds
                                .filter(_.reportId == report.id)
                                .map { build =>
                                  val stats = statistics
                                    .find(_._1 == build.id.get)
                                    .map(_._2)
                                  val asserts = assertions
                                    .find(_._1 == build.id.get)
                                    .map(_._2)
                                  build.copy(
                                    statistics = stats,
                                    assertions = asserts
                                  )
                                }
                              report.copy(builds = Some(newBuilds))
                            }
                            (reportsWithBuilds, hsettings)
                          }
                      }
                  }
              }
          }
          Future.sequence(linesRequests).map { requests =>
            requests.map { reportsAndSettings =>
              historyModule.calculate(
                reportsAndSettings._1,
                settings,
                Some(reportsAndSettings._2)
              )
            }
          }
        }
        .map(histories => Ok(Json.toJson(histories)))
    }

  def report(projectId: Int, reportId: Long): Action[AnyContent] =
    Action.async {
      for {
        maybeReport <- reportsService.report(reportId)
      } yield maybeReport match {
        case Some(report) => Ok(Json.toJson(report))
        case None         => NotFound
      }
    }

  def createReport(projectId: Int): Action[Report] =
    Action.async(parse.json[Report]) { request =>
      val reportWithProjectId = request.body.copy(projectId = Some(projectId))
      reportsService.createReport(reportWithProjectId).map { createdReport =>
        val location =
          Locations.report(createdReport.projectId.get, createdReport.id.get)
        Created(Json.toJson(createdReport))
          .withHeaders("Location" -> location)
      }
    }

  def updateReport(projectId: Int, reportId: Long): Action[Report] =
    Action.async(parse.json[Report]) { request =>
      val newReport = request.body
      for {
        maybeReport <- reportsService.report(reportId)
        _ <- reportsService.updateReport(
          maybeReport.get.copy(label = newReport.label, links = newReport.links)
        )
      } yield NoContent
    }

  def createBuild(projectId: Int, reportId: Long): Action[JsValue] =
    Action.async(parse.json) { request =>
      request.body
        .validate[BuildPayload]
        .fold(
          errors => Future(BadRequest(Json.obj("errors" -> JsError.toJson(errors)))),
          buildPayload => {
            val build = buildPayload.toBuild.copy(reportId = Some(reportId))
            reportsService.createBuild(build).map { createdBuild =>
              val location = controllers.routes.Reports
                .build(projectId, reportId, createdBuild.id.get)
                .url
              Created(Json.toJson(createdBuild))
                .withHeaders("Location" -> location)
            }
          }
        )
    }

  def remove(projectId: Int, reportId: Long): Action[AnyContent] =
    Action.async {
      for {
        _ <- reportsService.deleteReport(reportId)
      } yield NoContent
    }

  def builds(projectId: Int, reportId: Long): Action[AnyContent] =
    Action.async {
      for {
        builds <- reportsService.builds(reportId)
      } yield Ok(Json.toJson(builds.map(BuildPayload(_))))
    }

  def build(projectId: Int, reportId: Long, buildId: Long): Action[AnyContent] =
    Action.async {
      for {
        maybeBuild <- reportsService.build(buildId)
      } yield maybeBuild match {
        case Some(build) => Ok(Json.toJson(BuildPayload(build)))
        case None        => NotFound
      }
    }

  def statistics(
      projectId: Int,
      reportId: Long,
      buildId: Long
  ): Action[AnyContent] =
    Action.async {
      reportsService.statistics(buildId).map {
        case Some(statistics) => Ok(Json.toJson(statistics))
        case None             => NotFound
      }
    }

  def createStatistics(
      projectId: Int,
      reportId: Long,
      buildId: Long
  ): Action[JsValue] =
    Action.async(parse.json) { request =>
      val assertionsJsResult = request.body.validate[Statistics]
      assertionsJsResult.fold(
        errors => Future(BadRequest(Json.obj("errors" -> JsError.toJson(errors)))),
        statistics =>
          for {
            _ <- reportsService.createStatistics(buildId, statistics)
          } yield {
            NoContent
          }
      )
    }

  def assertions(
      projectId: Int,
      reportId: Long,
      buildId: Long
  ): Action[AnyContent] =
    Action.async {
      reportsService.assertions(buildId).map { assertions =>
        Ok(Json.toJson(assertions))
      }
    }

  def createAssertions(
      projectId: Int,
      reportId: Long,
      buildId: Long
  ): Action[JsValue] =
    Action.async(parse.json) { request =>
      val assertionsJsResult = request.body.validate[Assertions]
      assertionsJsResult.fold(
        errors => Future(BadRequest(Json.obj("errors" -> JsError.toJson(errors)))),
        assertions =>
          for {
            _ <- reportsService.createAssertions(buildId, assertions)
          } yield {
            NoContent
          }
      )
    }

  def transactions(
      projectId: Int,
      reportId: Long,
      buildId: Long,
      limit: Option[Int],
      group: Option[String],
      query: Option[String],
      status: Option[String],
      code: Option[Int],
      metric: Option[String]
  ): Action[AnyContent] =
    Action.async {
      val size = limit.getOrElse(100)

      reportsService.transactions(buildId).map { fetchedTransactions =>
        val groups   = fetchedTransactions.flatMap(_.group).distinct.sorted
        val statuses = fetchedTransactions.map(_.status).distinct.sorted
        val codes    = fetchedTransactions.flatMap(_.code).distinct.sorted
        val filters  = TransactionFilters(groups, statuses, codes)

        val metrics = fetchedTransactions.headOption
          .map(_.metrics.keys.toSeq)
          .getOrElse(Seq.empty)
        val sorters = TransactionSorters(metrics)

        val sortMetric = (metric, metrics) match {
          case (Some(_), _) => metric
          case (_, x) if x.contains(Metrics.ResponseTime) =>
            Some(Metrics.ResponseTime)
          case (_, a :: _) => Some(a)
          case (_, _)      => None
        }

        val transactions = fetchedTransactions.view
          .filter(t => group.forall(g => t.group.isDefined && t.group.get == g))
          .filter(t => query.forall(q => t.query.exists(_.contains(q))))
          .filter(t => status.forall(_ == t.status))
          .filter(t => code.forall(c => t.code.isDefined && t.code.get == c))
          .toSeq
          .sortBy(t => sortMetric.flatMap(m => t.metrics.get(m)))(
            Ordering[Option[Metrics.MetricValue]].reverse
          )
          .take(size)

        Ok(Json.toJson(TransactionList(filters, sorters, transactions)))
      }
    }

  def createTransactions(
      projectId: Int,
      reportId: Long,
      buildId: Long,
      putStatistics: Option[Boolean],
      putApdex: Option[Boolean],
      putAssertions: Option[Boolean]
  ): Action[Seq[Transaction]] =
    Action.async(parse.json[Seq[Transaction]]) { request =>
      reportsService.createTransactions(buildId, request.body).flatMap { _ =>
        if (putStatistics.orElse(putApdex).orElse(putAssertions).getOrElse(false)) {
          reportsService.report(reportId).flatMap { maybeReport =>
            reportsService.build(buildId).flatMap { maybeBuild =>
              (maybeReport, maybeBuild) match {
                case (Some(report), Some(build)) =>
                  val statistics = if (putStatistics.getOrElse(false)) {
                    Metrics.statistics(request.body, MetricSelectors.empty)
                  } else build.statistics.get
                  val reportWithStatistics = report.copy(builds =
                    Some(
                      Seq(
                        build.copy(
                          statistics = Some(statistics),
                          assertions = Some(Assertions.empty),
                          transactions = Some(request.body)
                        )
                      )
                    )
                  )

                  val reportWithApdex = if (putApdex.getOrElse(false)) {
                    projectsService.searches(projectId).flatMap { searches =>
                      projectsService.apdex(projectId).map { apdexRules =>
                        reportWithStatistics.withApdex(apdexRules, searches)
                      }
                    }
                  } else Future.successful(reportWithStatistics)

                  val reportWithAssertions =
                    if (putAssertions.getOrElse(false)) {
                      projectsService.searches(projectId).flatMap { searches =>
                        projectsService.assertions(projectId).map { assertionRules =>
                          reportWithApdex
                            .map(_.withAssertions(assertionRules, searches))
                        }
                      }
                    } else Future.successful(reportWithApdex)

                  reportWithAssertions.flatMap { futureReport =>
                    futureReport.flatMap { processedReport =>
                      reportsService
                        .createStatistics(
                          buildId,
                          processedReport.builds.get.head.statistics.get
                        )
                        .flatMap { _ =>
                          reportsService
                            .createAssertions(
                              buildId,
                              processedReport.builds.get.head.assertions.get
                            )
                            .flatMap { _ =>
                              reportsService
                                .updateBuild(processedReport.builds.get.head)
                                .flatMap { _ =>
                                  reportsService
                                    .updateReport(processedReport)
                                    .map(_ => NoContent)
                                }
                            }
                        }
                    }
                  }

                case (_, _) => Future.successful(NotFound)
              }
            }
          }
        } else Future.successful(NoContent)
      }
    }

  def graphs(
      projectId: Int,
      reportId: Long,
      buildId: Long,
      group: Option[String],
      hasError: Option[Boolean]
  ): Action[AnyContent] =
    Action.async {
      reportsService.transactions(buildId).map { fetchedTransactions =>
        val groups  = fetchedTransactions.flatMap(_.group).distinct.sorted
        val filters = GraphFilters(groups)

        val transactions = fetchedTransactions.view
          .filter(t => group.forall(g => t.group.contains(g)))
          .filter(t => hasError.forall(c => t.error.isDefined == c))
          .toSeq
          .sortBy(_.datetime.toEpochSecond(ZoneOffset.UTC))

        val transactionsByTimestamp = transactions
          .groupBy(_.datetime.toEpochSecond(ZoneOffset.UTC))
          .toSeq
          .sortBy(_._1)

        val requestsSeries =
          Series(
            Metrics.Sum,
            Some("requests"),
            transactionsByTimestamp.map(_._1.toString),
            transactionsByTimestamp.map(_._2.length.toDouble)
          )

        val transactionsByTimestampAndRtime = transactions
          .groupBy { t =>
            val responseTime = findMetric(t.metrics, Metrics.ResponseTime)
              .orElse(findMetric(t.metrics, Metrics.CalculationTime))
              .map(_ * 1000000)
              .getOrElse(0.0)
              .toLong
            t.datetime.plusNanos(responseTime).toEpochSecond(ZoneOffset.UTC)
          }
          .toSeq
          .sortBy(_._1)
        val responsesSeries =
          Series(
            Metrics.Sum,
            Some("responses"),
            transactionsByTimestampAndRtime.map(_._1.toString),
            transactionsByTimestampAndRtime.map(_._2.length.toDouble)
          )
        val transactionsGraph = Graph(
          Metrics.Transactions,
          group,
          filters,
          Seq(requestsSeries, responsesSeries),
          None
        )

        val successfulSeries =
          Series(
            Metrics.Sum,
            Some("successful"),
            transactionsByTimestampAndRtime.map(_._1.toString),
            transactionsByTimestampAndRtime
              .map(_._2.count(_.error.isEmpty).toDouble)
          )
        val failedSeries =
          Series(
            Metrics.Sum,
            Some("failed"),
            transactionsByTimestampAndRtime.map(_._1.toString),
            transactionsByTimestampAndRtime
              .map(_._2.count(_.error.isDefined).toDouble)
          )
        val statusesGraph = Graph(
          Metrics.Transactions,
          group,
          filters,
          Seq(successfulSeries, failedSeries),
          Some("by status")
        )

        val codeSeries = transactions
          .map(_.code)
          .distinct
          .sorted
          .map { code =>
            Series(
              Metrics.Sum,
              Some(code.map(_.toString).getOrElse("n/a")),
              transactionsByTimestampAndRtime.map(_._1.toString),
              transactionsByTimestampAndRtime
                .map(_._2.count(_.code == code).toDouble)
            )
          }
        val codesGraph = Graph(
          Metrics.Transactions,
          group,
          filters,
          codeSeries,
          Some("by code")
        )

        val calc = new StatCalculator()
        val metricGraphs = transactions.headOption
          .map { transaction =>
            transaction.metrics.toSeq
              .sortBy(m => Metrics.MetricsInfoMap(m._1).priority)
              .map { metric =>
                val valuesOverTime = transactionsByTimestamp.map { item =>
                  item._1 -> item._2
                    .flatMap(t => findMetric(t.metrics, metric._1))
                }

                val series = Metrics.MetricsInfoMap(metric._1).kind match {
                  case Metrics.TotalMetric =>
                    val avgSeries =
                      Series(
                        Metrics.Avg,
                        None,
                        valuesOverTime.map(_._1.toString),
                        valuesOverTime.map(v => calc.avg(v._2))
                      )
                    val sumSeries =
                      Series(
                        Metrics.Sum,
                        None,
                        valuesOverTime.map(_._1.toString),
                        valuesOverTime.map(v => v._2.sum)
                      )
                    Seq(avgSeries, sumSeries)
                  case _ =>
                    val maxSeries =
                      Series(
                        Metrics.Max,
                        None,
                        valuesOverTime.map(_._1.toString),
                        valuesOverTime.map(v => calc.max(v._2))
                      )
                    val p99Series =
                      Series(
                        Metrics.P99,
                        None,
                        valuesOverTime.map(_._1.toString),
                        valuesOverTime.map(v => calc.percentile(v._2, 99))
                      )
                    val p95Series =
                      Series(
                        Metrics.P95,
                        None,
                        valuesOverTime.map(_._1.toString),
                        valuesOverTime.map(v => calc.percentile(v._2, 95))
                      )
                    val p75Series =
                      Series(
                        Metrics.P75,
                        None,
                        valuesOverTime.map(_._1.toString),
                        valuesOverTime.map(v => calc.percentile(v._2, 75))
                      )
                    val medSeries =
                      Series(
                        Metrics.Median,
                        None,
                        valuesOverTime.map(_._1.toString),
                        valuesOverTime.map(v => calc.percentile(v._2, 50))
                      )

                    Seq(maxSeries, p99Series, p95Series, p75Series, medSeries)
                }

                Graph(metric._1, group, filters, series, None)
              }
          }
          .getOrElse(Seq.empty)

        Ok(
          Json.toJson(
            transactionsGraph +: statusesGraph +: codesGraph +: metricGraphs
          )
        )
      }
    }
}
