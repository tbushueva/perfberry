package controllers

object Locations {

  def report(projectId: Int, reportId: Long): String =
    routes.Reports
      .report(projectId, reportId)
      .url
}
