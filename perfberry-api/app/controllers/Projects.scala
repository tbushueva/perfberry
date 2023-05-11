package controllers

import javax.inject.Inject
import models.{AssertionRules, Metrics}
import models.Metrics.{ApdexRule, HistorySettings, MetricCode, StatsSelector}
import models.Projects.{Project, ProjectMeta, Search}
import play.api.libs.json.{JsError, JsValue, Json}
import play.api.mvc.{Action, AnyContent, InjectedController}
import services.{ProjectService, ReportsService}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class Projects @Inject() (
    val projectsService: ProjectService,
    val reportsService: ReportsService
) extends InjectedController {

  def list(alias: Option[String]): Action[AnyContent] = Action.async {
    for {
      projects <- projectsService.projects(alias)
    } yield Ok(Json.toJson(projects))
  }

  def item(id: Int): Action[AnyContent] = Action.async {
    for {
      maybeProject <- projectsService.project(id)
    } yield maybeProject match {
      case Some(project) => Ok(Json.toJson(project))
      case None          => NotFound
    }
  }

  def meta(id: Int): Action[AnyContent] = Action.async {
    for {
      reports    <- reportsService.list(id, None, None, None, 5)
      builds     <- reportsService.builds(reports.flatMap(_.id))
      statistics <- reportsService.statistics(builds.flatMap(_.id))
    } yield {
      val envs = builds.map(_.env).toSet
      val groups = statistics
        .flatMap(_._2.groups.map(_.name))
        .toSet
      val metrics = statistics
        .map { case (_, stats) =>
          stats.global.items.mapValues(_.keySet)
        }
        .foldLeft(Map.empty[MetricCode, Set[StatsSelector]]) { (result, current) =>
          result ++ current.map { case (metric, selectors) =>
            metric -> result
              .get(metric)
              .map(_ ++ selectors)
              .getOrElse(selectors)
          }
        }

      Ok(Json.toJson(ProjectMeta(envs, groups, metrics)))
    }
  }

  def updateProject(projectId: Int): Action[JsValue] =
    Action.async(parse.json) { request =>
      val projectResult = request.body.validate[Project]
      projectResult.fold(
        errors => Future(BadRequest(Json.obj("errors" -> JsError.toJson(errors)))),
        project => {
          val projectWithId = project.copy(id = Some(projectId))
          projectsService
            .updateProject(projectWithId)
            .map {
              case count: Int if count == 1 => NoContent
              case count: Int if count == 0 => NotFound
              case _                        => InternalServerError
            }
        }
      )
    }

  def overview(projectId: Int) = Action.async {
    projectsService.overview(projectId).map { items =>
      Ok(Json.toJson(items))
    }
  }

  def updateOverview(projectId: Int): Action[JsValue] =
    Action.async(parse.json) { request =>
      val result = request.body.validate[Seq[Metrics.HistorySettings]]
      result.fold(
        errors => Future(BadRequest(Json.obj("errors" -> JsError.toJson(errors)))),
        items => {
          projectsService.updateOverview(projectId, items).map { _ =>
            NoContent
          }
        }
      )
    }

  def searches(projectId: Int): Action[AnyContent] = Action.async {
    projectsService.searches(projectId).map { items =>
      Ok(Json.toJson(items))
    }
  }

  def updateSearches(projectId: Int): Action[JsValue] =
    Action.async(parse.json) { request =>
      val result = request.body.validate[Seq[Search]]
      result.fold(
        errors => Future(BadRequest(Json.obj("errors" -> JsError.toJson(errors)))),
        items => {
          projectsService.updateSearches(projectId, items).map { _ =>
            NoContent
          }
        }
      )
    }

  def assertions(projectId: Int): Action[AnyContent] = Action.async {
    projectsService.assertions(projectId).map { items =>
      Ok(Json.toJson(items))
    }
  }

  def updateAssertions(projectId: Int): Action[JsValue] =
    Action.async(parse.json) { request =>
      val result = request.body.validate[AssertionRules]
      result.fold(
        errors => Future(BadRequest(Json.obj("errors" -> JsError.toJson(errors)))),
        items => {
          projectsService.updateAssertions(projectId, items).map { _ =>
            NoContent
          }
        }
      )
    }

  def graphs(projectId: Int): Action[AnyContent] = Action.async {
    projectsService.graphs(projectId).map { items =>
      Ok(Json.toJson(items))
    }
  }

  def updateGraphs(projectId: Int): Action[JsValue] =
    Action.async(parse.json) { request =>
      val result = request.body.validate[Seq[Seq[HistorySettings]]]
      result.fold(
        errors => Future(BadRequest(Json.obj("errors" -> JsError.toJson(errors)))),
        items => {
          projectsService.updateGraphs(projectId, items).map { _ =>
            NoContent
          }
        }
      )
    }

  def apdex(projectId: Int): Action[AnyContent] = Action.async {
    projectsService.apdex(projectId).map { items =>
      Ok(Json.toJson(items))
    }
  }

  def updateApdex(projectId: Int): Action[JsValue] =
    Action.async(parse.json) { request =>
      val result = request.body.validate[Seq[ApdexRule]]
      result.fold(
        errors => Future(BadRequest(Json.obj("errors" -> JsError.toJson(errors)))),
        items => {
          projectsService.updateApdex(projectId, items).map { _ =>
            NoContent
          }
        }
      )
    }
}
