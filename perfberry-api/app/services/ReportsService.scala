package services

import java.time.{LocalDateTime, ZoneOffset}

import javax.inject.Inject
import models.Metrics.{GlobalStatistics, Statistics}
import models.Reports._
import models._
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import slick.jdbc.JdbcProfile

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class ReportsService @Inject() (
    protected val dbConfigProvider: DatabaseConfigProvider
) extends HasDatabaseConfigProvider[JdbcProfile]
    with QueryOps {

  import MyPostgresDriver.api._

  private case class ReportRow(
      id: Long,
      projectId: Int,
      label: Option[String],
      description: Option[String],
      scm: ScmInfo,
      links: Seq[Link],
      passed: Option[Boolean],
      createdAt: LocalDateTime
  ) {

    def toReport =
      Report(
        Some(id),
        Some(projectId),
        label,
        description,
        Some(scm),
        links,
        Some(createdAt),
        None,
        passed
      )
  }

  private class ReportTable(tag: Tag) extends Table[ReportRow](tag, "reports") {

    def id: Rep[Long] = column[Long]("id", O.AutoInc, O.PrimaryKey)

    def projectId: Rep[Int] = column[Int]("project_id")

    def label: Rep[Option[String]] = column[String]("label").?

    def description: Rep[Option[String]] = column[String]("description").?

    def scm: Rep[ScmInfo] = column[ScmInfo]("scm")

    def links: Rep[Seq[Link]] = column[Seq[Link]]("links")

    def passed: Rep[Option[Boolean]] = column[Boolean]("passed").?

    def createdAt: Rep[LocalDateTime] = column[LocalDateTime]("created_at")

    def * =
      (id, projectId, label, description, scm, links, passed, createdAt)
        .mapTo[ReportRow]
  }

  private lazy val reportQuery = TableQuery[ReportTable]

  private implicit class SearchQueryFilter(
      q: Query[ReportTable, ReportRow, Seq]
  ) {

    def filterBy(sq: SearchQuery): Query[ReportTable, ReportRow, Seq] = {
      sq.parts.foldLeft(q) { (query, filter) =>
        filter match {
          case LabelSearchQueryPart(true, label) =>
            query.filter(_.label like label)

          case LabelSearchQueryPart(false, label) =>
            query.filter(t => !(t.label like label) || t.label.isEmpty)

          case DescriptionSearchQueryPart(true, description) =>
            query.filter(_.description like description)

          case DescriptionSearchQueryPart(false, description) =>
            query.filter(t => !(t.description like description) || t.description.isEmpty)

          case VcsReferenceSearchQueryPart(true, reference) =>
            query.filter(_.scm #>> List("vcs", "reference") like reference)

          case VcsReferenceSearchQueryPart(false, reference) =>
            query.filter(t =>
              !(t.scm #>> List(
                "vcs",
                "reference"
              ) like reference) || !(t.scm ?? "vcs")
            )

          case VcsRevisionSearchQueryPart(true, revision) =>
            query.filter(_.scm #>> List("vcs", "revision") like revision)

          case VcsRevisionSearchQueryPart(false, revision) =>
            query.filter(t =>
              !(t.scm #>> List(
                "vcs",
                "revision"
              ) like revision) || !(t.scm ?? "vcs")
            )

          case VcsTitleSearchQueryPart(true, title) =>
            query.filter(_.scm #>> List("vcs", "title") like title)

          case VcsTitleSearchQueryPart(false, title) =>
            query.filter(t =>
              !(t.scm #>> List(
                "vcs",
                "title"
              ) like title) || !(t.scm ?? "vcs") || !(t.scm #> List(
                "vcs"
              ) ?? "title")
            )
        }
      }
    }
  }

  def list(
      projectId: Int,
      searchQuery: Option[SearchQuery],
      from: Option[LocalDateTime],
      to: Option[LocalDateTime],
      limit: Int = 50,
      offset: Int = 0
  ): Future[Seq[Report]] = {

    val query = reportQuery
      .filter(_.projectId === projectId)
      .filterOpt(from)(_.createdAt >= _)
      .filterOpt(to)(_.createdAt < _)
      .filterBy(searchQuery.getOrElse(SearchQuery.empty))
      .sortBy(_.createdAt.desc)
      .drop(offset)
      .take(limit)

    db.run(query.result.map(_.map(_.toReport)))
  }

  def report(id: Long): Future[Option[Report]] = {
    db.run(
      reportQuery
        .filter(_.id === id)
        .result
        .headOption
        .map(_.map(_.toReport))
    )
  }

  def createReport(report: Report): Future[Report] = {
    val reportWithId =
      (reportQuery returning reportQuery.map(_.id) into ((newReport, id) =>
        newReport.copy(id = id)
      )) += ReportRow(
        0,
        report.projectId.get,
        report.label,
        report.description,
        report.scm.getOrElse(ScmInfo.empty),
        report.links,
        None,
        report.createdAt.getOrElse(LocalDateTime.now(ZoneOffset.UTC))
      )

    db.run(reportWithId)
      .map(_.toReport)
      .flatMap { createdReport =>
        createBuilds(
          report.builds
            .getOrElse(Seq.empty)
            .map(_.copy(reportId = createdReport.id))
        )
          .map(_ => createdReport)
      }
  }

  def updateReport(report: Report): Future[Unit] = {
    val query = reportQuery
      .filter(_.id === report.id.get)
      .map(r => (r.label, r.links, r.passed))
      .update((report.label, report.links, report.passed))

    db.run(query).map(_ => ())
  }

  def deleteReport(reportId: Long): Future[Unit] = {
    val query = reportQuery
      .filter(_.id === reportId)
      .delete

    db.run(query).map(_ => ())
  }

  private case class BuildRow(
      id: Long,
      reportId: Long,
      env: String,
      label: Option[String],
      description: Option[String],
      scm: ScmInfo,
      links: Seq[Link],
      statistics: Statistics,
      assertions: Assertions,
      passed: Option[Boolean],
      transactions: Seq[Transaction],
      createdAt: LocalDateTime
  ) {

    def toBuild: Build =
      Build(
        Some(id),
        Some(reportId),
        env,
        label,
        description,
        scm,
        links,
        Some(statistics),
        Some(assertions),
        None,
        passed,
        Some(createdAt)
      )
  }

  private class BuildTable(tag: Tag) extends Table[BuildRow](tag, "builds") {

    def id = column[Long]("id", O.AutoInc, O.PrimaryKey)

    def reportId = column[Long]("report_id")

    def env = column[String]("env")

    def label: Rep[String] = column[String]("label")

    def description: Rep[String] = column[String]("description")

    def scm: Rep[ScmInfo] = column[ScmInfo]("scm")

    def links = column[Seq[Link]]("links")

    def statistics = column[Statistics]("statistics")

    def assertions = column[Assertions]("assertions")

    def passed = column[Boolean]("passed")

    def transactions = column[Seq[Transaction]]("transactions")

    def createdAt = column[LocalDateTime]("created_at")

    def * =
      (
        id,
        reportId,
        env,
        label.?,
        description.?,
        scm,
        links,
        statistics,
        assertions,
        passed.?,
        transactions,
        createdAt
      ).mapTo[BuildRow]
  }

  private val buildQuery = TableQuery[BuildTable]

  def buildsByIds(buildIds: Seq[Long]): Future[Seq[Build]] = {
    val query = buildQuery
      .filter(_.id inSet buildIds)
      .map(b =>
        (
          b.id,
          b.reportId,
          b.env,
          b.label.?,
          b.description.?,
          b.scm,
          b.links,
          b.passed.?,
          b.createdAt
        )
      )
    db.run(
      query.result.map(
        _.map(b =>
          Build(
            Some(b._1),
            Some(b._2),
            b._3,
            b._4,
            b._5,
            b._6,
            b._7,
            None,
            None,
            None,
            b._8,
            Some(b._9)
          )
        )
      )
    )
  }

  def builds(reportIds: Seq[Long]): Future[Seq[Build]] = {
    val query = buildQuery
      .filter(_.reportId inSet reportIds)
      .sortBy(_.createdAt)
      .map(b =>
        (
          b.id,
          b.reportId,
          b.env,
          b.label.?,
          b.description.?,
          b.scm,
          b.links,
          b.passed.?,
          b.createdAt
        )
      )
    db.run(
      query.result.map(
        _.map(b =>
          Build(
            Some(b._1),
            Some(b._2),
            b._3,
            b._4,
            b._5,
            b._6,
            b._7,
            None,
            None,
            None,
            b._8,
            Some(b._9)
          )
        )
      )
    )
  }

  def builds(reportId: Long): Future[Seq[Build]] = builds(Seq(reportId))

  def build(id: Long): Future[Option[Build]] = {
    db.run(
      buildQuery
        .filter(_.id === id)
        .map(b =>
          (
            b.id,
            b.reportId,
            b.env,
            b.label.?,
            b.description.?,
            b.scm,
            b.links,
            b.passed.?,
            b.createdAt
          )
        )
        .result
        .headOption
        .map(
          _.map(b =>
            Build(
              Some(b._1),
              Some(b._2),
              b._3,
              b._4,
              b._5,
              b._6,
              b._7,
              None,
              None,
              None,
              b._8,
              Some(b._9)
            )
          )
        )
    )
  }

  def createBuild(build: Build): Future[Build] = {
    val sortedStatistics = build.statistics
      .map { statistics =>
        statistics.copy(groups = statistics.groups.sortBy(_.name))
      }
      .getOrElse(Statistics(GlobalStatistics(None, Map.empty), Seq.empty))

    val buildWithId =
      (buildQuery returning buildQuery.map(_.id) into ((newBuild, id) =>
        newBuild.copy(id = id)
      )) += BuildRow(
        0,
        build.reportId.get,
        build.env,
        build.label,
        build.description,
        build.scm,
        build.links,
        sortedStatistics,
        build.assertions.getOrElse(Assertions.empty),
        build.passed,
        Seq.empty,
        build.createdAt.getOrElse(LocalDateTime.now(ZoneOffset.UTC))
      )

    report(build.reportId.get).flatMap { maybeReport =>
      db.run(buildWithId).map(_.toBuild).map { buildCreated =>
        if (build.transactions.isDefined) {
          createTransactions(
            buildCreated.id.get,
            build.transactions.getOrElse(Seq.empty)
          )
        }
        buildCreated
      }
    }
  }

  def createBuilds(builds: Seq[Build]): Future[Seq[Build]] =
    Future.sequence(builds.map(createBuild))

  def updateBuild(build: Build): Future[Unit] = {
    val query = buildQuery
      .filter(_.id === build.id.get)
      .map(r => (r.label.?, r.links, r.passed.?))
      .update((build.label, build.links, build.passed))

    db.run(query).map(_ => ())
  }

  def statistics(buildIds: Seq[Long]): Future[Seq[(Long, Statistics)]] = {
    val query = buildQuery
      .filter(_.id inSet buildIds)
      .map(b => (b.id, b.statistics))
      .result

    db.run(query)
  }

  def statistics(buildId: Long): Future[Option[Statistics]] =
    statistics(Seq(buildId)).map(_.headOption.map(_._2))

  def createStatistics(buildId: Long, item: Statistics): Future[Unit] = {
    val query = buildQuery
      .filter(_.id === buildId)
      .map(_.statistics)
      .update(item.withSorting)

    db.run(query).map(_ => ())
  }

  def assertions(buildId: Long): Future[Assertions] = {
    val query = buildQuery
      .filter(_.id === buildId)
      .map(_.assertions)
      .result
      .headOption

    db.run(query).map(_.getOrElse(Assertions.empty))
  }

  def assertions(buildIds: Seq[Long]): Future[Seq[(Long, Assertions)]] = {
    val query = buildQuery
      .filter(_.id inSet buildIds)
      .map(b => (b.id, b.assertions))
      .result

    db.run(query)
  }

  def createAssertions(buildId: Long, items: Assertions): Future[Unit] = {
    val query = buildQuery
      .filter(_.id === buildId)
      .map(_.assertions)
      .update(items)

    db.run(query).map(_ => ())
  }

  def transactions(buildId: Long): Future[Seq[Transaction]] = {
    val query = buildQuery
      .filter(_.id === buildId)
      .map(_.transactions)
      .result
      .headOption

    db.run(query).map(_.getOrElse(Seq.empty))
  }

  def createTransactions(
      buildId: Long,
      items: Seq[Transaction]
  ): Future[Unit] = {
    val query = buildQuery
      .filter(_.id === buildId)
      .map(_.transactions)
      .update(items)

    db.run(query).map(_ => ())
  }
}
