package services

import com.github.tminglei.slickpg._
import slick.basic.Capability
import slick.jdbc.JdbcCapabilities

trait MyPostgresDriver
    extends ExPostgresProfile
    with ReportsSupport
    with PgArraySupport
    with PgDateSupport
    with PgDate2Support
    with PgPlayJsonSupport {

  override def pgjson = "jsonb"

  override protected def computeCapabilities: Set[Capability] =
    super.computeCapabilities + JdbcCapabilities.insertOrUpdate

  override val api = new API
    with ArrayImplicits
    with DateTimeImplicits
    with JsonImplicits
    with ReportsImplicits
}

object MyPostgresDriver extends MyPostgresDriver
