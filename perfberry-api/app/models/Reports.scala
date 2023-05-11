package models

import java.time.LocalDateTime

import models.AssertionCondition._
import models.Metrics.{ApdexRule, apdex}
import models.Projects.Search
import models.payloads.BuildPayload
import play.api.libs.functional.syntax._
import play.api.libs.json._
import models.Metrics.ApdexCode

object Reports {

  case class Link(url: String, label: String)

  case class VcsInfo(reference: String, revision: String, title: Option[String])

  object VcsInfo {
    implicit val jsonFormat: OFormat[VcsInfo] = Json.format[VcsInfo]
  }

  case class ScmInfo(vcs: Option[VcsInfo], parameters: Map[String, String])

  object ScmInfo {
    implicit val jsonFormat: OFormat[ScmInfo] = Json.format[ScmInfo]

    val empty: ScmInfo = ScmInfo(None, Map.empty)
  }

  case class Build(
      id: Option[Long],
      reportId: Option[Long],
      env: String,
      label: Option[String],
      description: Option[String],
      scm: ScmInfo,
      links: Seq[Link],
      statistics: Option[Metrics.Statistics],
      assertions: Option[Assertions],
      transactions: Option[Seq[Transaction]],
      passed: Option[Boolean],
      createdAt: Option[LocalDateTime]
  )

  case class Report(
      id: Option[Long],
      projectId: Option[Int],
      label: Option[String],
      description: Option[String],
      scm: Option[ScmInfo],
      links: Seq[Link],
      createdAt: Option[LocalDateTime],
      builds: Option[Seq[Build]],
      passed: Option[Boolean]
  ) {

    def withLabel(newLabel: Option[String]): Report = (label, newLabel) match {
      case (_, Some(l)) => this.copy(label = Some(l))
      case (_, _)       => this
    }

    def withApdex(rules: Seq[ApdexRule], searches: Seq[Search]): Report = {
      val apdexRules = rules.filter { rule =>
        rule.searchName.forall { searchName =>
          searches.find(_.name == searchName).exists { search =>
            this.matches(SearchQuery(search.query))
          }
        }
      }

      val buildsWithApdex = builds.map { inBuilds =>
        inBuilds.map { build =>
          val statisticsWithApdex = build.statistics.map { statistics =>
            val globalApdex = apdexRules
              .find(_.group.isEmpty)
              .map(apdex(_, build.transactions.getOrElse(Seq.empty)))
            val globalStatistics = statistics.global.copy(apdex = globalApdex)

            val groupStatistics = statistics.groups.map { group =>
              val groupApdex = apdexRules
                .find(_.group.exists(_ == group.name))
                .map(apdex(_, build.transactions.getOrElse(Seq.empty)))
              group.copy(apdex = groupApdex)
            }

            statistics.copy(globalStatistics, groupStatistics)
          }

          build.copy(statistics = statisticsWithApdex)
        }
      }

      this.copy(builds = buildsWithApdex)
    }

    def withAssertions(rules: AssertionRules, searches: Seq[Search]): Report = {
      val assertionRules = rules.items
        .filter { rule =>
          rule.searchName.forall { searchName =>
            searches.find(_.name == searchName).exists { search =>
              this.matches(SearchQuery(search.query))
            }
          }
        }

      val buildsWithAssertions = builds.getOrElse(Seq.empty).map { build =>
        val statistics = build.statistics.get

        val assertions = assertionRules
          .filter { rule =>
            rule.buildName match {
              case Some(buildName) if buildName != build.env => false
              case _                                         => true
            }
          }
          .map { rule =>
            val maybeActual = (rule.group, rule.metric, rule.selector) match {
              case (None, ApdexCode, None) =>
                statistics.global.apdex.map(_.value)

              case (None, metric, Some(selector)) =>
                statistics.global.findStat(metric, selector)

              case (Some(groupName), ApdexCode, None) =>
                statistics.groups
                  .find(_.name == groupName)
                  .flatMap(_.apdex)
                  .map(_.value)

              case (Some(groupName), metric, Some(selector)) =>
                statistics.groups
                  .find(_.name == groupName)
                  .flatMap(_.findStat(metric, selector))

              case _ => None
            }

            val result = maybeActual.map { actual =>
              val expected = rule.expected

              val passed = rule.condition match {
                case Eq  => actual == expected
                case Lt  => actual < expected
                case Lte => actual <= expected
                case Gt  => actual > expected
                case Gte => actual >= expected
              }
              AssertionResult(actual, passed)
            }

            Assertion(
              rule.group,
              rule.metric,
              rule.selector,
              rule.condition,
              rule.expected,
              result
            )
          }
        val asserts = Assertions(assertions)
        build.copy(assertions = Some(asserts), passed = asserts.status)
      }

      // TODO this don't check that same build has been false and report false
      // and in build update to true report keep false
      val reportPassed = buildsWithAssertions
        .map(_.passed)
        .foldLeft(this.passed) { (res, cur) =>
          (res, cur) match {
            case (None, _)             => cur
            case (Some(true), Some(_)) => cur
            case (Some(true), None)    => res
            case (Some(false), _)      => res
          }
        }

      this.copy(builds = Some(buildsWithAssertions), passed = reportPassed)
    }

    def withBuildPassed(passed: Option[Boolean]): Report = {
      val newPassed = (this.passed, passed) match {
        case (None, _)             => passed
        case (Some(true), Some(_)) => passed
        case (Some(true), None)    => this.passed
        case (Some(false), _)      => this.passed
      }
      this.copy(passed = newPassed)
    }

    def matches(q: SearchQuery): Boolean = {

      def pattern(p: String): String = p.replace("_", ".").replace("%", ".*")

      def mtchs(s: String, part: String): Boolean = s.matches(pattern(part))

      q.parts.forall {
        case LabelSearchQueryPart(incl, part) =>
          this.label.map(mtchs(_, part) == incl).getOrElse(!incl)

        case DescriptionSearchQueryPart(incl, part) =>
          this.description.map(mtchs(_, part) == incl).getOrElse(!incl)

        case VcsReferenceSearchQueryPart(incl, part) =>
          this.scm
            .flatMap(_.vcs.map(_.reference))
            .map(mtchs(_, part) == incl)
            .getOrElse(!incl)

        case VcsRevisionSearchQueryPart(incl, part) =>
          this.scm
            .flatMap(_.vcs.map(_.revision))
            .map(mtchs(_, part) == incl)
            .getOrElse(!incl)

        case VcsTitleSearchQueryPart(incl, part) =>
          this.scm
            .flatMap(_.vcs.map(_.title))
            .map(_.exists(mtchs(_, part)) == incl)
            .getOrElse(!incl)
      }
    }
  }

  object Link {

    implicit val jsonFormat: Format[Link] = (
      (JsPath \ "url").format[String] and
        (JsPath \ "label").format[String]
    )(Link.apply, unlift(Link.unapply))
  }

  object Build {

    implicit val jsonFormat: Format[Build] = (
      (JsPath \ "id").formatNullable[Long] and
        (JsPath \ "report_id").formatNullable[Long] and
        (JsPath \ "env").format[String] and
        (JsPath \ "label").formatNullable[String] and
        (JsPath \ "description").formatNullable[String] and
        (JsPath \ "scm").format[ScmInfo] and
        (JsPath \ "links").format[Seq[Link]] and
        (JsPath \ "statistics").formatNullable[Metrics.Statistics] and
        (JsPath \ "assertions").formatNullable[Assertions] and
        (JsPath \ "transactions").formatNullable[Seq[Transaction]] and
        (JsPath \ "passed").formatNullable[Boolean] and
        (JsPath \ "created_at").formatNullable[LocalDateTime]
    )(Build.apply, unlift(Build.unapply))

    def apply(data: String): Build = Json.parse(data).as[BuildPayload].toBuild

    val defaultEnv = "default"
  }

  object Report {

    implicit val jsonFormat: Format[Report] = (
      (JsPath \ "id").formatNullable[Long] and
        (JsPath \ "project_id").formatNullable[Int] and
        (JsPath \ "label").formatNullable[String] and
        (JsPath \ "description").formatNullable[String] and
        (JsPath \ "scm").formatNullable[ScmInfo] and
        (JsPath \ "links").format[Seq[Link]] and
        (JsPath \ "created_at").formatNullable[LocalDateTime] and
        (JsPath \ "builds").formatNullable[Seq[Build]] and
        (JsPath \ "passed").formatNullable[Boolean]
    )(Report.apply, unlift(Report.unapply))

    def apply(data: String): Report = Json.parse(data).as[Report]

    val empty =
      Report(None, None, None, None, None, Seq.empty, None, None, None)
  }
}
