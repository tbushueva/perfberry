package controllers

import models.AssertionCondition
import models.Metrics.{MetricsInfo, StatsSelectorsInfo}
import play.api.libs.json.Json
import play.api.mvc.InjectedController

class Info extends InjectedController {

  def info() = Action {
    val info =
      models.Metrics.Info(
        MetricsInfo,
        StatsSelectorsInfo,
        AssertionCondition.info
      )
    Ok(Json.toJson(info))
  }
}
