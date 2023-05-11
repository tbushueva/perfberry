package models

import models.Metrics.{MetricCode, StatsSelector}
import play.api.libs.functional.syntax._
import play.api.libs.json.Reads._
import play.api.libs.json._

object Projects {

  case class Search(name: String, query: String)

  object Search {
    implicit val jsonFormat: OFormat[Search] = Json.format[Search]
  }

  case class Settings(
      searches: Seq[Search],
      overview: Seq[Metrics.HistorySettings]
  )

  object Settings {
    implicit val jsonFormat: OFormat[Settings] = Json.format[Settings]
  }

  case class Project(
      id: Option[Int],
      alias: String,
      name: String,
      settings: Option[Settings]
  )

  object Project {

    implicit val jsonFormat: Format[Project] = (
      (JsPath \ "id").formatNullable[Int] and
        (JsPath \ "alias").format[String](minLength[String](2)) and
        (JsPath \ "name").format[String] and
        (JsPath \ "settings").formatNullable[Settings]
    )(Project.apply, unlift(Project.unapply))
  }

  case class ProjectMeta(
      envs: Set[String],
      groups: Set[String],
      metrics: Map[MetricCode, Set[StatsSelector]]
  )

  object ProjectMeta {
    implicit val jsonFormat: OFormat[ProjectMeta] = Json.format[ProjectMeta]
  }
}
