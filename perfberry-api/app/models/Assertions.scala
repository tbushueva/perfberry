package models

import enumeratum.EnumEntry.Lowercase
import enumeratum.{EnumEntry, PlayEnum}
import models.Metrics.{MetricCode, MetricValue, StatsSelector}
import play.api.libs.json.JsonConfiguration.Aux
import play.api.libs.json.JsonNaming.SnakeCase
import play.api.libs.json.Reads._
import play.api.libs.json._

import scala.collection.immutable

sealed trait AssertionCondition extends EnumEntry

object AssertionCondition extends PlayEnum[AssertionCondition] {

  case object Eq extends AssertionCondition with Lowercase

  case object Lt extends AssertionCondition with Lowercase

  case object Lte extends AssertionCondition with Lowercase

  case object Gt extends AssertionCondition with Lowercase

  case object Gte extends AssertionCondition with Lowercase

  val values: immutable.IndexedSeq[AssertionCondition] = findValues

  val info: immutable.IndexedSeq[ConditionInfo] = values.map {
    case c @ Eq =>
      ConditionInfo(c, "Equal", "Check that actual value equal expected")
    case c @ Lt =>
      ConditionInfo(
        c,
        "Less than",
        "Check that actual value less than expected"
      )
    case c @ Lte =>
      ConditionInfo(
        c,
        "Less than or equal",
        "Check that actual value less than expected or equal"
      )
    case c @ Gt =>
      ConditionInfo(
        c,
        "Greater than",
        "Check that actual value greater than expected"
      )
    case c @ Gte =>
      ConditionInfo(
        c,
        "Greater than or equal",
        "Check that actual value greater than expected or equal"
      )
  }
}

case class ConditionInfo(
    condition: AssertionCondition,
    label: String,
    name: String
)

object ConditionInfo {
  implicit val jsWrt: OWrites[ConditionInfo] = Json.writes[ConditionInfo]
}

trait AssertionBase {

  def group: Option[String]

  def metric: MetricCode

  def selector: Option[StatsSelector]

  def condition: AssertionCondition

  def expected: MetricValue
}

case class AssertionResult(actual: MetricValue, passed: Boolean)

object AssertionResult {
  implicit val jsFmt: Format[AssertionResult] = Json.format[AssertionResult]
}

case class Assertion(
    group: Option[String],
    metric: MetricCode,
    selector: Option[StatsSelector],
    condition: AssertionCondition,
    expected: MetricValue,
    result: Option[AssertionResult]
) extends AssertionBase

object Assertion {
  implicit val jsFmt: Format[Assertion] = Json.format[Assertion]
}

case class Assertions(items: Seq[Assertion]) extends AnyVal {

  def status: Option[Boolean] = items match {
    case Nil => None
    case _ =>
      val failedCount = items.filterNot(_.result.exists(_.passed)).length
      Some(failedCount == 0)
  }

  def find(
      group: Option[String],
      metric: MetricCode,
      selector: Option[StatsSelector]
  ): Option[Assertion] =
    items.find { a =>
      a.group == group && a.metric == metric && a.selector == selector
    }
}

object Assertions {
  val empty = Assertions(Seq.empty)

  implicit val jsFmt: Format[Assertions] = new Format[Assertions] {

    override def reads(json: JsValue): JsResult[Assertions] =
      json.validate[Seq[Assertion]].map(Assertions(_))

    override def writes(a: Assertions): JsValue = Json.toJson(a.items)
  }
}

case class AssertionRule(
    buildName: Option[String],
    searchName: Option[String],
    group: Option[String],
    metric: MetricCode,
    selector: Option[StatsSelector],
    condition: AssertionCondition,
    expected: MetricValue
) extends AssertionBase

object AssertionRule {
  implicit val jsCfg: Aux[Json.MacroOptions] = JsonConfiguration(SnakeCase)
  implicit val jsFmt: Format[AssertionRule]  = Json.format[AssertionRule]
}

case class AssertionRules(items: Seq[AssertionRule]) extends AnyVal

object AssertionRules {
  val empty = AssertionRules(Seq.empty)

  implicit val jsFmt: Format[AssertionRules] = new Format[AssertionRules] {

    override def reads(json: JsValue): JsResult[AssertionRules] =
      json.validate[Seq[AssertionRule]].map(AssertionRules(_))

    override def writes(r: AssertionRules): JsValue = Json.toJson(r.items)
  }

  def apply(data: String): AssertionRules = Json.parse(data).as[AssertionRules]
}
