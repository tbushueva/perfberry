package models

import java.time.{LocalDateTime, ZoneOffset}

import models.Projects.Search
import modules.StatCalculator
import play.api.libs.functional.syntax._
import play.api.libs.json.JsonNaming.SnakeCase
import play.api.libs.json._
import services.MyPostgresDriver.MappedJdbcType
import services.MyPostgresDriver.api.playJsonTypeMapper
import slick.jdbc.JdbcType

object Metrics {

  case class ApdexZones(t: Double, f: Double)

  object ApdexZones {
    implicit val jsonFormat: OFormat[ApdexZones] = Json.format[ApdexZones]
  }

  case class Apdex(
      value: Double,
      metric: MetricCode,
      samples: Long,
      zones: ApdexZones
  ) {

    def -(b: Apdex): Option[Apdex] = {
      if (
        metric == b.metric && samples != 0 && b.samples != 0 && zones.t == b.zones.t && zones.f == b.zones.f
      ) {
        val zonesDiff = ApdexZones(zones.t - b.zones.t, zones.f - b.zones.f)
        val valueDiff = Math.round((value - b.value) * 100) / 100.0
        Some(Apdex(valueDiff, metric, samples - b.samples, zonesDiff))
      } else None
    }
  }

  object Apdex {
    implicit val jsonFormat: OFormat[Apdex] = Json.format[Apdex]
  }

  case class ApdexRule(
      searchName: Option[String],
      group: Option[String],
      metric: MetricCode,
      t: Double,
      f: Double
  )

  object ApdexRule {
    implicit val jsonConfig                     = JsonConfiguration(SnakeCase)
    implicit val jsonFormat: OFormat[ApdexRule] = Json.format[ApdexRule]
  }

  type MetricCode    = String
  type MetricValue   = Double
  type StatsSelector = String

  type SelectorItems  = Map[StatsSelector, MetricValue]
  type StatisticItems = Map[MetricCode, SelectorItems]

  def findStatistic(
      items: StatisticItems,
      metric: MetricCode,
      selector: StatsSelector
  ): Option[MetricValue] =
    items.get(metric).flatMap(_.get(selector))

  type MetricItems = Map[MetricCode, MetricValue]

  def findMetric(items: MetricItems, code: MetricCode): Option[MetricValue] =
    items.get(code)

  case class MetricSelectors(items: Map[MetricCode, Seq[StatsSelector]]) extends AnyVal {

    def contains(metric: MetricCode, selector: StatsSelector): Boolean =
      items.get(metric).exists(_.contains(selector))

    def find(metric: MetricCode): Seq[StatsSelector] =
      items.getOrElse(metric, Seq.empty)

    def isEmpty: Boolean = items.isEmpty
  }

  object MetricSelectors {
    val empty: MetricSelectors = MetricSelectors(Map.empty)
  }

  abstract class BaseStatistics(apdex: Option[Apdex], items: StatisticItems) {

    protected def diffApdex(compared: Option[Apdex]): Option[Apdex] =
      (this.apdex, compared) match {
        case (Some(o), Some(c)) => o - c
        case _                  => None
      }

    protected def diffItems(compared: StatisticItems): StatisticItems =
      this.items.map { metrics =>
        metrics._1 -> metrics._2.flatMap { selectors =>
          compared
            .get(metrics._1)
            .flatMap(_.get(selectors._1)) //@TODO findStat?
            .map { comparedValue =>
              selectors._1 -> (selectors._2 - comparedValue)
            }
        }
      }

    def findStat(
        metric: MetricCode,
        selector: StatsSelector
    ): Option[MetricValue] =
      this.items.get(metric).flatMap(_.get(selector))
  }

  case class GlobalStatistics(apdex: Option[Apdex], items: StatisticItems)
      extends BaseStatistics(apdex, items) {

    def diff(compared: GlobalStatistics): GlobalStatistics =
      this.copy(
        apdex = this.diffApdex(compared.apdex),
        items = this.diffItems(compared.items)
      )
  }

  implicit val globalMetricsJsonFormat: Format[GlobalStatistics] =
    Json.format[GlobalStatistics]

  case class GroupStatistics(
      name: String,
      apdex: Option[Apdex],
      items: StatisticItems
  ) extends BaseStatistics(apdex, items) {

    def diff(compared: GroupStatistics): GroupStatistics =
      this.copy(
        apdex = this.diffApdex(compared.apdex),
        items = this.diffItems(compared.items)
      )
  }

  implicit val groupMetricsJsonFormat: Format[GroupStatistics] =
    Json.format[GroupStatistics]

  case class Statistics(
      global: GlobalStatistics,
      groups: Seq[GroupStatistics]
  ) {

    def withSorting: Statistics =
      this.copy(groups = groups.sortBy(_.name))

    def diff(compared: Statistics): Statistics = {
      val groupDiffs = this.groups.flatMap { group =>
        compared.groups.find(_.name == group.name).map(group.diff)
      }
      this
        .copy(global = this.global.diff(compared.global), groups = groupDiffs)
    }
  }

  implicit val metricsJsonFormat: Format[Statistics] = (
    (JsPath \ "global").format[GlobalStatistics] and
      (JsPath \ "groups").format[Seq[GroupStatistics]]
  )(Statistics.apply, unlift(Statistics.unapply))

  case class MetricStat(metric: MetricCode, selector: StatsSelector)

  implicit val metricStatJsonWrites = Json.writes[MetricStat]

  case class MetricFilter(
      group: Option[String],
      hasApdex: Boolean,
      stats: Seq[MetricStat]
  ) {

    def withSorting: MetricFilter =
      this.copy(
        stats = stats.sortBy(metricCode =>
          (
            MetricsInfoMap(metricCode.metric).priority,
            StatsSelectorsInfoMap(metricCode.selector).priority
          )
        )
      )
  }

  implicit val metricFilterJsonWrites = Json.writes[MetricFilter]

  case class HistoryFilters(
      metrics: Seq[MetricFilter],
      envs: Seq[String],
      searches: Option[Seq[Search]]
  )

  implicit val historyFiltersJsonWrites = Json.writes[HistoryFilters]

  case class HistoryItem(
      value: MetricValue,
      reportId: Long,
      label: Option[String],
      createdAt: LocalDateTime,
      expected: Option[MetricValue]
  )

  implicit val historyItemJsonFormat: Format[HistoryItem] = (
    (JsPath \ "value").format[MetricValue] and
      (JsPath \ "report_id").format[Long] and
      (JsPath \ "label").formatNullable[String] and
      (JsPath \ "created_at").format[LocalDateTime] and
      (JsPath \ "expected").formatNullable[MetricValue]
  )(HistoryItem.apply, unlift(HistoryItem.unapply))

  case class HistorySettings(
      group: Option[String],
      metric: MetricCode,
      selector: Option[StatsSelector],
      env: String,
      searchName: Option[String]
  )

  object HistorySettings {

    implicit val jsonConfig = JsonConfiguration(SnakeCase)
    implicit val jsonFormat = Json.format[HistorySettings]

    implicit val jdbcMapper: JdbcType[HistorySettings] =
      MappedJdbcType
        .base[HistorySettings, JsValue](Json.toJson(_), _.as[HistorySettings])
  }

  case class MetricHistory(settings: HistorySettings, values: Seq[HistoryItem])

  implicit val metricHistoryJsonWrites = Json.writes[MetricHistory]

  case class History(filters: HistoryFilters, history: MetricHistory)

  implicit val historyJsonWrites = Json.writes[History]

  val ApdexCode: MetricCode         = "apx"
  val SpeedIndex: MetricCode        = "si"
  val Transactions: MetricCode      = "ts"
  val Throughput: MetricCode        = "tt"
  val Errors: MetricCode            = "err"
  val ErrorRatio: MetricCode        = "erat"
  val ResponseTime: MetricCode      = "rt"
  val CalculationTime: MetricCode   = "ct"
  val ConnectTime: MetricCode       = "cot"
  val SendTime: MetricCode          = "st"
  val Latency: MetricCode           = "lt"
  val ReceiveTime: MetricCode       = "ret"
  val JobTime: MetricCode           = "jt"
  val ProcessingTime: MetricCode    = "pt"
  val TTFB: MetricCode              = "ttfb"
  val TTFP: MetricCode              = "ttfp"
  val DOMLoaded: MetricCode         = "doml"
  val DOMInteractive: MetricCode    = "domi"
  val DOMComplete: MetricCode       = "domc"
  val FirstVisualChange: MetricCode = "fvc"
  val LastVisualChange: MetricCode  = "lvc"
  val VisualComplete95: MetricCode  = "vc95"
  val ContentSize: MetricCode       = "cs"
  val Egress: MetricCode            = "eg"
  val Ingress: MetricCode           = "ig"
  val CpuUsed: MetricCode           = "cpu"
  val AppCpuUsed: MetricCode        = "acpu"
  val GpuUsed: MetricCode           = "gpu"
  val AppGpuUsed: MetricCode        = "agpu"
  val MemoryUsed: MetricCode        = "mu"
  val AppMemoryUsed: MetricCode     = "amu"
  val GpuMemoryUsed: MetricCode     = "gmu"
  val AppGpuMemoryUsed: MetricCode  = "agmu"
  val FileSystemUsed: MetricCode    = "fsu"
  val FrameRate: MetricCode         = "fr"
  val FrameTime: MetricCode         = "ft"
  val DrawCalls: MetricCode         = "dc"
  val Objects: MetricCode           = "obs"
  val Triangles: MetricCode         = "trs"
  val Vertices: MetricCode          = "vrs"

  type MetricKind = String
  val PercentilesMetric: MetricKind = "percentiles"
  val AverageMetric: MetricKind     = "average"
  val TotalMetric: MetricKind       = "total"

  case class MetricInfo(
      metric: MetricCode,
      priority: Int,
      label: String,
      unit: Option[String],
      spaced: Option[Boolean],
      inverted: Boolean,
      kind: MetricKind,
      name: String
  )

  object MetricInfo {
    implicit val jsonConfig           = JsonConfiguration(SnakeCase)
    implicit val metricInfoJsonWrites = Json.writes[MetricInfo]
  }

  val MetricsInfo = Seq(
    MetricInfo(
      ApdexCode,
      0,
      "Apdex",
      None,
      spaced = None,
      inverted = false,
      TotalMetric,
      "Apdex"
    ),
    MetricInfo(
      SpeedIndex,
      1,
      "SpeedIndex",
      Some("ms"),
      spaced = Some(true),
      inverted = true,
      PercentilesMetric,
      "Speed Index"
    ),
    MetricInfo(
      Transactions,
      2,
      "Transactions",
      None,
      None,
      inverted = false,
      TotalMetric,
      "Transactions (it may be HTTP request, query to database, etc)"
    ),
    MetricInfo(
      Throughput,
      3,
      "Throughput",
      Some("TPS"),
      spaced = Some(true),
      inverted = false,
      AverageMetric,
      "Rate of transactions per seconds"
    ),
    MetricInfo(
      Errors,
      4,
      "Errors",
      None,
      None,
      inverted = true,
      TotalMetric,
      "Failed transactions"
    ),
    MetricInfo(
      ErrorRatio,
      5,
      "Error ratio",
      Some("%"),
      spaced = Some(false),
      inverted = true,
      TotalMetric,
      "Failed transactions ratio"
    ),
    MetricInfo(
      ResponseTime,
      6,
      "RT",
      Some("ms"),
      spaced = Some(true),
      inverted = true,
      PercentilesMetric,
      "Response time"
    ),
    MetricInfo(
      CalculationTime,
      7,
      "CT",
      Some("ms"),
      spaced = Some(true),
      inverted = true,
      PercentilesMetric,
      "Calculation time"
    ),
    MetricInfo(
      ConnectTime,
      8,
      "Connect",
      Some("ms"),
      spaced = Some(true),
      inverted = true,
      AverageMetric,
      "Connect time"
    ),
    MetricInfo(
      SendTime,
      9,
      "Send",
      Some("ms"),
      spaced = Some(true),
      inverted = true,
      AverageMetric,
      "Send time"
    ),
    MetricInfo(
      Latency,
      10,
      "Latency",
      Some("ms"),
      spaced = Some(true),
      inverted = true,
      AverageMetric,
      "Latency"
    ),
    MetricInfo(
      ReceiveTime,
      11,
      "Receive",
      Some("ms"),
      spaced = Some(true),
      inverted = true,
      AverageMetric,
      "Receive time"
    ),
    MetricInfo(
      JobTime,
      12,
      "Job Time",
      Some("s"),
      spaced = Some(true),
      inverted = true,
      AverageMetric,
      "Job time"
    ),
    MetricInfo(
      ProcessingTime,
      13,
      "Processing Time",
      Some("s"),
      spaced = Some(true),
      inverted = true,
      AverageMetric,
      "Processing time"
    ),
    MetricInfo(
      TTFB,
      14,
      "TTFB",
      Some("ms"),
      spaced = Some(true),
      inverted = true,
      PercentilesMetric,
      "Time to first byte"
    ),
    MetricInfo(
      TTFP,
      15,
      "TTFP",
      Some("ms"),
      spaced = Some(true),
      inverted = true,
      PercentilesMetric,
      "Time to first paint"
    ),
    MetricInfo(
      DOMLoaded,
      16,
      "DOMLoaded",
      Some("ms"),
      spaced = Some(true),
      inverted = true,
      PercentilesMetric,
      "DOM content loaded time"
    ),
    MetricInfo(
      DOMInteractive,
      17,
      "DOMInteractive",
      Some("ms"),
      spaced = Some(true),
      inverted = true,
      PercentilesMetric,
      "DOM interactive time"
    ),
    MetricInfo(
      DOMComplete,
      18,
      "DOMComplete",
      Some("ms"),
      spaced = Some(true),
      inverted = true,
      PercentilesMetric,
      "DOM complete time"
    ),
    MetricInfo(
      FirstVisualChange,
      19,
      "FirstVisual",
      Some("ms"),
      spaced = Some(true),
      inverted = true,
      PercentilesMetric,
      "First visual change time"
    ),
    MetricInfo(
      LastVisualChange,
      20,
      "LastVisual",
      Some("ms"),
      spaced = Some(true),
      inverted = true,
      PercentilesMetric,
      "Last visual change time"
    ),
    MetricInfo(
      VisualComplete95,
      21,
      "Visual95",
      Some("ms"),
      spaced = Some(true),
      inverted = true,
      PercentilesMetric,
      "Visual complete 95% time"
    ),
    MetricInfo(
      ContentSize,
      22,
      "CSize",
      Some("kB"),
      spaced = Some(true),
      inverted = true,
      TotalMetric,
      "Content size"
    ),
    MetricInfo(
      Egress,
      23,
      "Egress",
      Some("MB"),
      spaced = Some(true),
      inverted = false,
      TotalMetric,
      "Output bandwidth"
    ),
    MetricInfo(
      Ingress,
      24,
      "Ingress",
      Some("MB"),
      spaced = Some(true),
      inverted = false,
      TotalMetric,
      "Input bandwidth"
    ),
    MetricInfo(
      CpuUsed,
      25,
      "CPU",
      Some("%"),
      spaced = Some(false),
      inverted = true,
      AverageMetric,
      "CPU utilization"
    ),
    MetricInfo(
      AppCpuUsed,
      26,
      "App CPU",
      Some("%"),
      spaced = Some(false),
      inverted = true,
      AverageMetric,
      "Application CPU utilization"
    ),
    MetricInfo(
      GpuUsed,
      27,
      "GPU",
      Some("%"),
      spaced = Some(false),
      inverted = true,
      AverageMetric,
      "GPU utilization"
    ),
    MetricInfo(
      AppGpuUsed,
      28,
      "App GPU",
      Some("%"),
      spaced = Some(false),
      inverted = true,
      AverageMetric,
      "Application GPU utilization"
    ),
    MetricInfo(
      MemoryUsed,
      29,
      "Memory",
      Some("MB"),
      spaced = Some(true),
      inverted = true,
      AverageMetric,
      "Used memory"
    ),
    MetricInfo(
      AppMemoryUsed,
      30,
      "App Memory",
      Some("MB"),
      spaced = Some(true),
      inverted = true,
      AverageMetric,
      "Application used memory"
    ),
    MetricInfo(
      GpuMemoryUsed,
      31,
      "GPU Memory",
      Some("MB"),
      spaced = Some(true),
      inverted = true,
      AverageMetric,
      "GPU used memory"
    ),
    MetricInfo(
      AppGpuMemoryUsed,
      32,
      "App GPU Memory",
      Some("MB"),
      spaced = Some(true),
      inverted = true,
      AverageMetric,
      "Application GPU used memory"
    ),
    MetricInfo(
      FileSystemUsed,
      33,
      "File System",
      Some("MB"),
      spaced = Some(true),
      inverted = true,
      AverageMetric,
      "File system usage"
    ),
    MetricInfo(
      FrameRate,
      34,
      "Frame Rate",
      Some("FPS"),
      spaced = Some(true),
      inverted = false,
      AverageMetric,
      "Frame rate"
    ),
    MetricInfo(
      FrameTime,
      35,
      "Frame Time",
      Some("ms"),
      spaced = Some(true),
      inverted = true,
      AverageMetric,
      "Frame time"
    ),
    MetricInfo(
      DrawCalls,
      36,
      "Draws",
      None,
      spaced = None,
      inverted = true,
      TotalMetric,
      "Draw calls"
    ),
    MetricInfo(
      Objects,
      37,
      "Objects",
      None,
      spaced = None,
      inverted = true,
      TotalMetric,
      "Objects in scene"
    ),
    MetricInfo(
      Triangles,
      38,
      "Triangles",
      None,
      spaced = None,
      inverted = true,
      TotalMetric,
      "Triangles in scene"
    ),
    MetricInfo(
      Vertices,
      39,
      "Vertices",
      None,
      spaced = None,
      inverted = true,
      TotalMetric,
      "Vertices in scene"
    )
  )

  val MetricsInfoMap: Map[MetricCode, MetricInfo] =
    MetricsInfo.map(m => m.metric -> m).toMap

  val Sum: StatsSelector    = "sum"
  val Max: StatsSelector    = "max"
  val P9999: StatsSelector  = "p9999"
  val P999: StatsSelector   = "p999"
  val P99: StatsSelector    = "p99"
  val P98: StatsSelector    = "p98"
  val P95: StatsSelector    = "p95"
  val P90: StatsSelector    = "p90"
  val P75: StatsSelector    = "p75"
  val Median: StatsSelector = "med"
  val Min: StatsSelector    = "min"
  val Avg: StatsSelector    = "avg"
  val StdDev: StatsSelector = "stdev"

  case class StatsInfo(
      selector: StatsSelector,
      priority: Int,
      label: String,
      name: String
  )

  implicit val statsInfoJsonWrites = Json.writes[StatsInfo]

  val StatsSelectorsInfo = Seq(
    StatsInfo(Sum, 1, "Total", "Total"),
    StatsInfo(Max, 2, "Max", "Maximum"),
    StatsInfo(P9999, 3, "99,99th", "A 99,99th percentile"),
    StatsInfo(P999, 4, "99,9th", "A 99,9th percentile"),
    StatsInfo(P99, 5, "99th", "A 99th percentile"),
    StatsInfo(P98, 6, "98th", "A 98th percentile"),
    StatsInfo(P95, 7, "95th", "A 95th percentile"),
    StatsInfo(P90, 8, "90th", "A 90th percentile"),
    StatsInfo(P75, 9, "75th", "A 75th percentile"),
    StatsInfo(Median, 10, "Median", "Median (50th percentile)"),
    StatsInfo(Min, 11, "Min", "Minimum"),
    StatsInfo(Avg, 12, "Avg", "Average"),
    StatsInfo(StdDev, 13, "StdDev", "Standard deviation")
  )

  val StatsSelectorsInfoMap: Map[StatsSelector, StatsInfo] =
    StatsSelectorsInfo.map(s => s.selector -> s).toMap

  case class Info(
      metrics: Seq[MetricInfo],
      selectors: Seq[StatsInfo],
      conditions: Seq[ConditionInfo]
  )

  object Info {
    implicit val jsonWrites = Json.writes[Info]
  }

  def apdex(rule: ApdexRule, items: Seq[Transaction]): Apdex = {
    val zones = ApdexZones(rule.t, rule.f)

    val values = items
      .filter(i => rule.group.forall(_ == i.group.getOrElse("")))
      .flatMap(t => findMetric(t.metrics, rule.metric))
    val samples = values.length

    // While metrics not in SI
    val multiplier = if (rule.metric == ResponseTime) {
      0.001
    } else 1

    val value = if (samples > 0) {
      val ranges = values.foldLeft((0, 0)) {
        case (r, v) if v * multiplier < zones.t => (r._1 + 1, r._2)
        case (r, v) if zones.t <= v * multiplier && v * multiplier <= zones.f =>
          (r._1, r._2 + 1)
        case (r, _) => (r._1, r._2)
      }
      Math.round((ranges._1 + ranges._2 / 2) / samples.toDouble * 100) / 100.0
    } else 0.0

    Apdex(value, rule.metric, samples, zones)
  }

  def transactionStatistics(
      items: Seq[Transaction],
      selectors: MetricSelectors
  ): StatisticItems = {
    val calc = new StatCalculator

    val transactions: StatisticItems =
      if (selectors.isEmpty || selectors.contains(Transactions, Sum)) {
        Map(Transactions -> Map(Sum -> items.length))
      } else Map.empty

    val throughput: StatisticItems =
      if (selectors.isEmpty || selectors.contains(Throughput, Avg)) {
        val requestsPerSeconds =
          items
            .groupBy(_.datetime.toEpochSecond(ZoneOffset.UTC))
            .values
            .map(_.length.toDouble)
            .toSeq
        Map(Throughput -> Map(Avg -> calc.avg(requestsPerSeconds)))
      } else Map.empty

    val errors: StatisticItems =
      if (selectors.isEmpty || selectors.contains(Errors, Sum)) {
        Map(Errors -> Map(Sum -> items.count(_.error.isDefined)))
      } else Map.empty

    val errorRate: StatisticItems =
      if (selectors.isEmpty || selectors.contains(ErrorRatio, Sum)) {
        Map(
          ErrorRatio -> Map(
            Sum -> calc.round(
              items
                .count(_.error.isDefined)
                .toDouble / items.length.toDouble * 100
            )
          )
        )
      } else Map.empty

    val metricStats: StatisticItems = items.headOption
      .map { transaction =>
        transaction.metrics.flatMap { metric =>
          val metricValues =
            items.flatMap(t => findMetric(t.metrics, metric._1))

          val metricSelectors =
            if (selectors.isEmpty) {
              MetricsInfoMap(metric._1).kind match {
                case TotalMetric =>
                  Seq(Sum)
                case AverageMetric =>
                  Seq(Avg)
                case PercentilesMetric =>
                  Seq(Max, P99, P95, P75, Median, Min, Avg, StdDev)
              }
            } else selectors.find(metric._1)

          val stats = metricSelectors.map { selector =>
            val value = selector match {
              case Sum    => calc.round(metricValues.sum)
              case Max    => calc.max(metricValues)
              case P9999  => calc.percentile(metricValues, 99.99)
              case P999   => calc.percentile(metricValues, 99.9)
              case P99    => calc.percentile(metricValues, 99)
              case P98    => calc.percentile(metricValues, 98)
              case P95    => calc.percentile(metricValues, 95)
              case P90    => calc.percentile(metricValues, 90)
              case P75    => calc.percentile(metricValues, 75)
              case Median => calc.percentile(metricValues, 50)
              case Min    => calc.min(metricValues)
              case Avg    => calc.avg(metricValues)
              case StdDev => calc.stdDev(metricValues)
            }
            selector -> value
          }.toMap

          Map(metric._1 -> stats)
        }
      }
      .getOrElse(Map.empty)

    transactions ++ throughput ++ errors ++ errorRate ++ metricStats
  }

  def statistics(
      items: Seq[Transaction],
      metricSelectors: MetricSelectors
  ): Statistics = {
    val globalStatistics = GlobalStatistics(
      None,
      transactionStatistics(items, metricSelectors: MetricSelectors)
    )

    val groupStatistics = items
      .filter(_.group.isDefined)
      .groupBy(_.group.get)
      .map { group =>
        GroupStatistics(
          group._1,
          None,
          transactionStatistics(group._2, metricSelectors: MetricSelectors)
        )
      }
      .toSeq

    Statistics(globalStatistics, groupStatistics)
  }

  case class GraphFilters(groups: Seq[String])

  object GraphFilters {
    implicit val jsonFormat: OFormat[GraphFilters] = Json.format[GraphFilters]
  }

  case class Series(
      selector: String,
      description: Option[String],
      x: Seq[String],
      values: Seq[MetricValue]
  )

  object Series {
    implicit val jsonFormat: OFormat[Series] = Json.format[Series]
  }

  case class Graph(
      metric: String,
      group: Option[String],
      filters: GraphFilters,
      series: Seq[Series],
      description: Option[String]
  )

  object Graph {
    implicit val jsonFormat: OFormat[Graph] = Json.format[Graph]
  }
}
