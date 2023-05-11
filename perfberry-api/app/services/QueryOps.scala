package services

import play.api.db.slick.HasDatabaseConfigProvider
import slick.jdbc.JdbcProfile
import slick.lifted.CanBeQueryCondition

import scala.language.higherKinds

/** Will be in Slick after release this PR (slick > 3.2.3, play-slick > 3.0.3).
  * https://github.com/slick/slick/pull/1909
  */
trait QueryOps { databaseConfig: HasDatabaseConfigProvider[JdbcProfile] =>

  import MyPostgresDriver.api._

  implicit class ConditionalQueryFilter[E, U, Seq[_]](q: Query[E, U, Seq]) {

    def filterOpt[V, T <: Rep[_]: CanBeQueryCondition](optValue: Option[V])(
        f: (E, V) => T
    ): Query[E, U, Seq] =
      optValue.map(v => q.filter(e => f(e, v))).getOrElse(q)

    def filterIf(p: Boolean)(f: E => Rep[Boolean]): Query[E, U, Seq] =
      if (p) q.filter(f) else q
  }
}
