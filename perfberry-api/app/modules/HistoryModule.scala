package modules

import models.Metrics.{
  History,
  HistoryFilters,
  HistoryItem,
  HistorySettings,
  MetricFilter,
  MetricHistory,
  MetricStat
}
import models.Projects.Settings
import models.Reports.Report
import models.Metrics.ApdexCode

class HistoryModule {

  def calculate(
      reversedReports: Seq[Report],
      settings: Settings,
      hsettings: Option[HistorySettings]
  ): History = {

    val reports = reversedReports.reverse.filter(_.builds.isDefined)
    val globalMetrics = reports.flatMap { r =>
      r.builds.get
        .flatMap { build =>
          build.statistics.get.global.items.flatMap(m =>
            m._2.map { stat =>
              MetricStat(m._1, stat._1)
            }
          )
        }
    }.distinct
    val hasGlobalApdex = reports.exists { report =>
      report.builds.exists(
        _.exists(_.statistics.exists(_.global.apdex.isDefined))
      )
    }

    val globalFilter = (hasGlobalApdex, globalMetrics) match {
      case (a, m) if a || m.nonEmpty =>
        Seq(MetricFilter(None, hasGlobalApdex, globalMetrics).withSorting)
      case (_, _) => Seq.empty
    }

    val buildsWithGroups =
      reports.flatMap(_.builds.get).filter(_.statistics.get.groups.nonEmpty)
    val groupsFilters = buildsWithGroups
      .flatMap { build =>
        build.statistics.get.groups.map(g => g.name)
      }
      .distinct
      .map { groupName =>
        val hasGroupApdex = buildsWithGroups.exists(
          _.statistics.exists(
            _.groups.find(_.name == groupName).exists(_.apdex.isDefined)
          )
        )
        val metrics = buildsWithGroups
          .flatMap { build =>
            build.statistics.get.groups
              .find(g => g.name == groupName)
              .map(
                _.items.flatMap(metrics =>
                  metrics._2
                    .map(selectors => MetricStat(metrics._1, selectors._1))
                )
              )
          }
          .flatten
          .distinct
        MetricFilter(Some(groupName), hasGroupApdex, metrics).withSorting
      }

    val metricFilters = globalFilter ++ groupsFilters
      .sortBy(_.group)
    val envFilters = reports.flatMap { r =>
      r.builds.get.map(_.env)
    }.distinct

    val env = hsettings
      .map(_.env)
      .getOrElse(
        settings.overview.headOption.map(_.env).getOrElse(envFilters.head)
      )
    val group = hsettings.map(_.group) match {
      case Some(Some(g)) => Some(g)
      case Some(None)    => None
      case None =>
        settings.overview.headOption.flatMap(_.group) match {
          case Some(g) => Some(g)
          case None =>
            metricFilters.head.group match {
              case Some(g) => Some(g)
              case None    => None
            }
        }
    }
    val metricCode = hsettings
      .map(_.metric)
      .getOrElse(
        settings.overview.headOption
          .map(_.metric)
          .getOrElse(
            metricFilters.find(f => f.group == group).get.stats.head.metric
          )
      )
    val apdexRequired = metricCode == ApdexCode
    val selector = if (!apdexRequired) {
      hsettings
        .flatMap(_.selector)
        .orElse(settings.overview.headOption.flatMap(_.selector))
        .orElse(
          metricFilters.find(f => f.group == group).map(_.stats.head.selector)
        )
    } else None

    val reportsWithScope = group match {
      case None =>
        reports.filter(r => r.builds.get.exists(build => build.env == env))
      case Some(g) =>
        reports.filter(r =>
          r.builds.get.exists(build =>
            build.env == env && build.statistics.get.groups
              .exists(_.name == g)
          )
        )
    }
    val historyValues = reportsWithScope
      .flatMap { report =>
        report.builds.get
          .find(_.env == env)
          .flatMap { build =>
            val metricValue = if (apdexRequired) {
              group match {
                case None =>
                  build.statistics.get.global.apdex.map(_.value)
                case Some(g) =>
                  build.statistics.get.groups
                    .find(_.name == g)
                    .flatMap(_.apdex.map(_.value))
              }
            } else {
              group match {
                case None =>
                  build.statistics.get.global
                    .findStat(metricCode, selector.get)
                case Some(g) =>
                  build.statistics.get.groups
                    .find(_.name == g)
                    .flatMap(_.findStat(metricCode, selector.get))
              }
            }

            val expectedValue = build.assertions.flatMap { assertions =>
              assertions.find(group, metricCode, selector).map(_.expected)
            }

            (metricValue, expectedValue) match {
              case (Some(v), e) =>
                Some(
                  HistoryItem(
                    v,
                    report.id.get,
                    report.label,
                    report.createdAt.get,
                    e
                  )
                )
              case _ => None
            }
          }
      }

    val metricHistory =
      MetricHistory(
        hsettings.getOrElse(
          HistorySettings(group, metricCode, selector, env, None)
        ),
        historyValues
      )

    History(
      HistoryFilters(metricFilters, envFilters, Some(settings.searches)),
      metricHistory
    )
  }
}
