package services

import javax.inject.Inject
import models.{AssertionRules, Metrics}
import models.Metrics.{ApdexRule, HistorySettings}
import models.Projects.{Project, Search}
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import play.api.libs.json.{JsValue, Json}
import slick.jdbc.{JdbcProfile, JdbcType}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class ProjectService @Inject() (
    protected val dbConfigProvider: DatabaseConfigProvider
) extends HasDatabaseConfigProvider[JdbcProfile] {

  import MyPostgresDriver.MappedJdbcType
  import MyPostgresDriver.api._

  case class ProjectRow(id: Int, alias: String, name: String) {

    def toProject =
      Project(Some(id), alias, name, None)
  }

  implicit val searchesJdbcMapper: JdbcType[Seq[Search]] =
    MappedJdbcType
      .base[Seq[Search], JsValue](Json.toJson(_), _.as[Seq[Search]])

  implicit val assertionRulesJdbcMapper: JdbcType[AssertionRules] =
    MappedJdbcType
      .base[AssertionRules, JsValue](Json.toJson(_), _.as[AssertionRules])

  implicit val historyJdbcMapper: JdbcType[Seq[HistorySettings]] =
    MappedJdbcType
      .base[Seq[HistorySettings], JsValue](
        Json.toJson(_),
        _.as[Seq[HistorySettings]]
      )

  implicit val graphsJdbcMapper: JdbcType[Seq[Seq[HistorySettings]]] =
    MappedJdbcType
      .base[Seq[Seq[HistorySettings]], JsValue](
        Json.toJson(_),
        _.as[Seq[Seq[HistorySettings]]]
      )

  implicit private val apdexJdbcMapper: JdbcType[Seq[ApdexRule]] =
    MappedJdbcType
      .base[Seq[ApdexRule], JsValue](Json.toJson(_), _.as[Seq[ApdexRule]])

  private class ProjectTable(tag: Tag) extends Table[ProjectRow](tag, "projects") {

    def id = column[Int]("id", O.AutoInc, O.PrimaryKey)

    def alias = column[String]("alias")

    def name = column[String]("name")

    def overview = column[Seq[HistorySettings]]("overview")

    def searches = column[Seq[Search]]("searches")

    def assertions = column[AssertionRules]("assertions")

    def graphs = column[Seq[Seq[HistorySettings]]]("graphs")

    def apdex = column[Seq[ApdexRule]]("apdex")

    def * = (id, alias, name).mapTo[ProjectRow]
  }

  private val projectQuery = TableQuery[ProjectTable]

  def projects(alias: Option[String]): Future[Seq[Project]] = {
    val query = projectQuery
    val queryWithAlias = alias match {
      case Some(a) => query.filter(_.alias === a)
      case None    => query.sortBy(_.name)
    }
    db.run(queryWithAlias.result.map(_.map(_.toProject)))
  }

  def project(id: Int): Future[Option[Project]] =
    db.run(
      projectQuery
        .filter(_.id === id)
        .result
        .headOption
        .map(_.map(_.toProject))
    )

  def updateProject(project: Project): Future[Int] = {
    val newProject =
      ProjectRow(project.id.get, project.alias, project.name)

    val query = projectQuery
      .filter(_.id === newProject.id)
      .map(p => (p.alias, p.name))
      .update(newProject.alias, newProject.name)

    db.run(query)
  }

  def overview(id: Int): Future[Seq[Metrics.HistorySettings]] = {
    val query = projectQuery
      .filter(_.id === id)
      .map(_.overview)
      .result
      .headOption

    db.run(query).map(_.getOrElse(Seq.empty))
  }

  def updateOverview(
      projectId: Int,
      items: Seq[Metrics.HistorySettings]
  ): Future[Unit] = {
    val query = projectQuery
      .filter(_.id === projectId)
      .map(_.overview)
      .update(items)

    db.run(query).map(_ => ())
  }

  def searches(id: Int): Future[Seq[Search]] = {
    val query = projectQuery
      .filter(_.id === id)
      .map(_.searches)
      .result
      .headOption

    db.run(query).map(_.getOrElse(Seq.empty))
  }

  def updateSearches(projectId: Int, items: Seq[Search]): Future[Unit] = {
    val query = projectQuery
      .filter(_.id === projectId)
      .map(_.searches)
      .update(items)

    db.run(query).map(_ => ())
  }

  def assertions(id: Int): Future[AssertionRules] = {
    val query = projectQuery
      .filter(_.id === id)
      .map(_.assertions)
      .result
      .headOption

    db.run(query).map(_.getOrElse(AssertionRules.empty))
  }

  def updateAssertions(projectId: Int, items: AssertionRules): Future[Unit] = {
    val query = projectQuery
      .filter(_.id === projectId)
      .map(_.assertions)
      .update(items)

    db.run(query).map(_ => ())
  }

  def graphs(id: Int): Future[Seq[Seq[HistorySettings]]] = {
    val query = projectQuery
      .filter(_.id === id)
      .map(_.graphs)
      .result
      .headOption

    db.run(query).map(_.getOrElse(Seq.empty))
  }

  def updateGraphs(
      projectId: Int,
      items: Seq[Seq[HistorySettings]]
  ): Future[Unit] = {
    val query = projectQuery
      .filter(_.id === projectId)
      .map(_.graphs)
      .update(items)

    db.run(query).map(_ => ())
  }

  def apdex(id: Int): Future[Seq[ApdexRule]] = {
    val query = projectQuery
      .filter(_.id === id)
      .map(_.apdex)
      .result
      .headOption

    db.run(query).map(_.getOrElse(Seq.empty))
  }

  def updateApdex(projectId: Int, items: Seq[ApdexRule]): Future[Unit] = {
    val query = projectQuery
      .filter(_.id === projectId)
      .map(_.apdex)
      .update(items)

    db.run(query).map(_ => ())
  }
}
