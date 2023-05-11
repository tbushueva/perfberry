package models

import java.time.LocalDateTime

import models.Metrics.{MetricCode, MetricItems}
import play.api.libs.json.{Json, OFormat}

case class Transaction(
    group: Option[String],
    query: Option[String],
    payload: Option[String],
    datetime: LocalDateTime,
    status: String,
    code: Option[Int],
    error: Option[String],
    metrics: MetricItems
)

object Transaction {
  implicit val jsonFormat: OFormat[Transaction] = Json.format[Transaction]
}

case class TransactionFilters(
    groups: Seq[String],
    statuses: Seq[String],
    codes: Seq[Int]
)

object TransactionFilters {

  implicit val jsonFormat: OFormat[TransactionFilters] =
    Json.format[TransactionFilters]
}

case class TransactionSorters(metrics: Seq[MetricCode])

object TransactionSorters {

  implicit val jsonFormat: OFormat[TransactionSorters] =
    Json.format[TransactionSorters]
}

case class TransactionList(
    filters: TransactionFilters,
    sorters: TransactionSorters,
    items: Seq[Transaction]
)

object TransactionList {

  implicit val jsonFormat: OFormat[TransactionList] =
    Json.format[TransactionList]
}
