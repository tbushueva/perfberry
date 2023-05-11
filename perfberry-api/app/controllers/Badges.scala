package controllers

import javax.inject.Inject
import models.Metrics.{MetricsInfoMap, StatsSelectorsInfoMap}
import models.SearchQuery
import play.api.mvc.{Action, AnyContent, InjectedController}
import services.ReportsService

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class Badges @Inject() (val reportsService: ReportsService) extends InjectedController {

  private def badge(label: String, value: String, color: String): String = {
    val charWidth  = 6.8
    val labelWidth = label.length * charWidth
    val valueWidth = value.length * charWidth
    val width      = labelWidth + valueWidth + charWidth * 3
    val valueBg = color match {
      case "blue"  => "#0d8ce4"
      case "green" => "#4ca733"
      case "red"   => "#c3654b"
    }

    s"""
      <svg xmlns="http://www.w3.org/2000/svg" width="$width" height="20">
        <linearGradient id="b" x2="0" y2="100%">
          <stop offset="0" stop-color="#bbb" stop-opacity=".1"/>
          <stop offset="1" stop-opacity=".1"/>
        </linearGradient>

        <mask id="a">
          <rect width="$width" height="20" rx="3" fill="#fff"/>
        </mask>

        <g mask="url(#a)">
          <path fill="#555" d="M0 0 h${labelWidth + charWidth} v20 H0 z"/>
          <path fill="$valueBg" d="M${labelWidth + charWidth} 0 h${valueWidth + charWidth * 2} v20 H${labelWidth + charWidth} z"/>
          <path fill="url(#b)" d="M0 0 h$width v20 H0 z"/>
        </g>

        <g fill="#fff" text-anchor="middle">
          <g font-family="DejaVu Sans,Verdana,Geneva,sans-serif" font-size="11">
            <text x="$charWidth" y="15" fill="#010101" fill-opacity=".3" text-anchor="start">
              $label
            </text>
            <text x="$charWidth" y="14" text-anchor="start">
              $label
            </text>
            <text x="${labelWidth + charWidth * 2}" y="15" fill="#010101" fill-opacity=".3" text-anchor="start">
              $value
            </text>
            <text x="${labelWidth + charWidth * 2}" y="14" text-anchor="start">
              $value
            </text>
          </g>
        </g>
      </svg>
    """.stripMargin
  }

  def get(
      projectId: Int,
      badgeType: String,
      query: Option[SearchQuery]
  ): Action[AnyContent] =
    Action.async {
      val reportsBadge      = badge("perfberry", "reports", "blue")
      val passedReportBadge = badge("perfberry", "passed", "green")
      val failedReportBadge = badge("perfberry", "failed", "red")

      badgeType match {
        case "static" =>
          Future.successful(Ok(reportsBadge).as("image/svg+xml"))
        case "status" =>
          reportsService
            .list(projectId, query, None, None, 1)
            .map { reports =>
              if (reports.isEmpty) {
                Ok(reportsBadge).as("image/svg+xml")
              } else {
                reports.head.passed match {
                  case Some(true)  => Ok(passedReportBadge).as("image/svg+xml")
                  case Some(false) => Ok(failedReportBadge).as("image/svg+xml")
                  case None        => Ok(reportsBadge).as("image/svg+xml")
                }
              }
            }
      }
    }

  def stats(
      projectId: Int,
      query: Option[SearchQuery],
      env: Option[String],
      group: Option[String],
      metric: Option[String],
      selector: Option[String]
  ) = Action.async {
    for {
      reports    <- reportsService.list(projectId, query, None, None, 1)
      builds     <- reportsService.builds(reports.head.id.get)
      statistics <- reportsService.statistics(builds.flatMap(_.id))
    } yield reports match {
      case Nil => NotFound
      case r =>
        val maybeBuild = env match {
          case Some(e) => builds.find(_.env == e)
          case None    => builds.headOption
        }
        maybeBuild match {
          case None => NotFound
          case Some(build) =>
            val buildStatistics =
              statistics.find(_._1 == build.id.get).get._2
            val stats = group match {
              case Some(g) =>
                buildStatistics.groups.find(_.name == g).get.items
              case None =>
                if (buildStatistics.global.items.nonEmpty) {
                  buildStatistics.global.items
                } else {
                  buildStatistics.groups.head.items
                }
            }

            val metricCode   = metric.getOrElse(stats.head._1)
            val selectorCode = selector.getOrElse(stats(metricCode).head._1)

            val maybeStat =
              stats.get(metricCode).flatMap(_.get(selectorCode))

            maybeStat match {
              case None => NotFound
              case Some(stat) =>
                val metricInfo = MetricsInfoMap(metricCode)
                val delimiter = metricInfo.spaced
                  .map(s => if (s) " " else "")
                  .getOrElse(" ")
                val selectorInfo = StatsSelectorsInfoMap(selectorCode)
                val label        = metricInfo.label + " " + selectorInfo.label
                val value =
                  f"$stat%.2f$delimiter${metricInfo.unit.getOrElse("")}"
                Ok(badge(label, value, "blue")).as("image/svg+xml")
            }
        }
    }
  }
}
