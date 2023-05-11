package controllers

import javax.inject.Inject

import play.api.libs.json.Json
import play.api.mvc.{Action, AnyContent, InjectedController}
import services.ReportsService

import scala.concurrent.ExecutionContext.Implicits.global

class Diffs @Inject() (val reportsService: ReportsService) extends InjectedController {

  def buildStatistics(
      originalBuildId: Long,
      comparedBuildId: Long
  ): Action[AnyContent] =
    Action.async {
      for {
        maybeOriginalStatistics <- reportsService.statistics(originalBuildId)
        maybeComparedStatistics <- reportsService.statistics(comparedBuildId)
      } yield (maybeOriginalStatistics, maybeComparedStatistics) match {
        case (Some(originalStatistics), Some(comparedStatistics)) =>
          Ok(Json.toJson(originalStatistics.diff(comparedStatistics)))
        case (_, _) => NotFound
      }
    }
}
