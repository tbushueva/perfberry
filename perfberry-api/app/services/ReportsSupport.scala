package services

import com.github.tminglei.slickpg.{ExPostgresProfile, PgPlayJsonSupport}
import com.github.tminglei.slickpg.json.PgJsonExtensions
import models.Metrics.Statistics
import models.Reports.{Link, ScmInfo}
import models.{Assertions, Transaction}
import slick.jdbc.JdbcType
import slick.lifted.Rep

trait ReportsSupport extends PgJsonExtensions {
  driver: ExPostgresProfile with PgPlayJsonSupport =>

  import play.api.libs.json._

  trait ReportsImplicits extends JsonImplicits {

    implicit val linksJdbcType: JdbcType[Seq[Link]] =
      MappedJdbcType.base[Seq[Link], JsValue](Json.toJson, _.as[Seq[Link]])

    implicit val scmJdbcType: JdbcType[ScmInfo] =
      MappedJdbcType.base[ScmInfo, JsValue](Json.toJson, _.as[ScmInfo])

    implicit def scmMethods(c: Rep[ScmInfo])(implicit
        tm: JdbcType[ScmInfo]
    ): JsonColumnExtensionMethods[ScmInfo, ScmInfo] =
      new JsonColumnExtensionMethods[ScmInfo, ScmInfo](c)(tm)

    implicit val statisticsJdbcType: JdbcType[Statistics] =
      MappedJdbcType
        .base[Statistics, JsValue](Json.toJson, _.as[Statistics])

    implicit val assertionsJdbcMapper: JdbcType[Assertions] =
      MappedJdbcType
        .base[Assertions, JsValue](Json.toJson(_), _.as[Assertions])

    implicit val transactionsJdbcType: JdbcType[Seq[Transaction]] =
      MappedJdbcType
        .base[Seq[Transaction], JsValue](Json.toJson, _.as[Seq[Transaction]])
  }
}
