package models.payloads

import java.time.LocalDateTime

import models.Reports.{Build, Link, ScmInfo}
import play.api.libs.json.JsonConfiguration.Aux
import play.api.libs.json.JsonNaming.SnakeCase
import play.api.libs.json.{Json, JsonConfiguration, OFormat}

case class BuildPayload(
    id: Option[Long],
    env: String,
    label: Option[String],
    description: Option[String],
    scm: Option[ScmInfo],
    links: Option[Seq[Link]],
    passed: Option[Boolean],
    createdAt: Option[LocalDateTime]
) {

  def toBuild: Build =
    Build(
      id,
      None,
      env,
      label,
      description,
      scm.getOrElse(ScmInfo.empty),
      links.getOrElse(Seq.empty),
      None,
      None,
      None,
      passed,
      createdAt
    )
}

object BuildPayload {
  implicit val jsCfg: Aux[Json.MacroOptions] = JsonConfiguration(SnakeCase)
  implicit val jsFmt: OFormat[BuildPayload]  = Json.format[BuildPayload]

  def apply(b: Build): BuildPayload =
    BuildPayload(
      b.id,
      b.env,
      b.label,
      b.description,
      Some(b.scm),
      Some(b.links),
      b.passed,
      b.createdAt
    )
}
