import java.time.LocalDateTime

import models.Metrics._
import models.Transaction
import org.scalatestplus.play._

class TransactionStatisticsSpec extends PlaySpec {

  "A transactionStatistics function" must {

    "return zero statistics on empty transactions" in {
      val stats = transactionStatistics(Seq.empty, MetricSelectors.empty)
      findStatistic(stats, Transactions, Sum) mustBe Some(0)
      findStatistic(stats, Throughput, Avg) mustBe Some(0)
      findStatistic(stats, Errors, Sum) mustBe Some(0)
      findStatistic(stats, ErrorRatio, Sum) mustBe Some(0)
    }

    "return common statistics on transactions with empty selectors" in {
      val transactions = Seq(
        Transaction(
          None,
          None,
          None,
          LocalDateTime.now,
          "",
          None,
          None,
          Map.empty
        )
      )
      val stats = transactionStatistics(transactions, MetricSelectors.empty)
      findStatistic(stats, Transactions, Sum) mustBe Some(1)
      findStatistic(stats, Throughput, Avg) mustBe Some(1)
      findStatistic(stats, Errors, Sum) mustBe Some(0)
      findStatistic(stats, ErrorRatio, Sum) mustBe Some(0)
    }

    "return errors statistics on failed transactions" in {
      val transactions = Seq(
        Transaction(
          None,
          None,
          None,
          LocalDateTime.now,
          "",
          None,
          Some("e"),
          Map.empty
        )
      )
      val stats = transactionStatistics(transactions, MetricSelectors.empty)
      findStatistic(stats, Transactions, Sum) mustBe Some(1)
      findStatistic(stats, Throughput, Avg) mustBe Some(1)
      findStatistic(stats, Errors, Sum) mustBe Some(1)
      findStatistic(stats, ErrorRatio, Sum) mustBe Some(100)
    }

    "not return common statistics on transactions even not provided" in {
      val transactions = Seq(
        Transaction(
          None,
          None,
          None,
          LocalDateTime.now,
          "",
          None,
          None,
          Map.empty
        )
      )
      val selectors = MetricSelectors(Map(ResponseTime -> Seq(Max)))
      val stats     = transactionStatistics(transactions, selectors)
      findStatistic(stats, Transactions, Sum) mustBe None
      findStatistic(stats, Throughput, Avg) mustBe None
      findStatistic(stats, Errors, Sum) mustBe None
      findStatistic(stats, ErrorRatio, Sum) mustBe None
    }

    "return common statistics on transactions with provided selectors" in {
      val transactions = Seq(
        Transaction(
          None,
          None,
          None,
          LocalDateTime.now,
          "",
          None,
          None,
          Map.empty
        )
      )
      val selectors = MetricSelectors(
        Map(
          Transactions -> Seq(Sum),
          Throughput   -> Seq(Avg),
          Errors       -> Seq(Sum),
          ErrorRatio   -> Seq(Sum)
        )
      )
      val stats = transactionStatistics(transactions, selectors)
      findStatistic(stats, Transactions, Sum) mustBe Some(1)
      findStatistic(stats, Throughput, Avg) mustBe Some(1)
      findStatistic(stats, Errors, Sum) mustBe Some(0)
      findStatistic(stats, ErrorRatio, Sum) mustBe Some(0)
    }

    "return statistics for total metrics with empty selectors" in {
      val transactions = Seq(
        Transaction(
          None,
          None,
          None,
          LocalDateTime.now,
          "",
          None,
          None,
          Map(Ingress -> 1)
        )
      )
      val stats = transactionStatistics(transactions, MetricSelectors.empty)
      findStatistic(stats, Ingress, Sum) mustBe Some(1)
    }

    "return statistics for average metrics with empty selectors" in {
      val transactions = Seq(
        Transaction(
          None,
          None,
          None,
          LocalDateTime.now,
          "",
          None,
          None,
          Map(Latency -> 1)
        )
      )
      val stats = transactionStatistics(transactions, MetricSelectors.empty)
      findStatistic(stats, Latency, Avg) mustBe Some(1)
    }

    "return statistics for percentiles metrics with empty selectors" in {
      val transactions = Seq(
        Transaction(
          None,
          None,
          None,
          LocalDateTime.now,
          "",
          None,
          None,
          Map(ResponseTime -> 1)
        )
      )
      val stats = transactionStatistics(transactions, MetricSelectors.empty)
      findStatistic(stats, ResponseTime, Max) mustBe Some(1)
      findStatistic(stats, ResponseTime, P99) mustBe Some(1)
      findStatistic(stats, ResponseTime, P95) mustBe Some(1)
      findStatistic(stats, ResponseTime, P75) mustBe Some(1)
      findStatistic(stats, ResponseTime, Median) mustBe Some(1)
      findStatistic(stats, ResponseTime, Min) mustBe Some(1)
      findStatistic(stats, ResponseTime, Avg) mustBe Some(1)
      findStatistic(stats, ResponseTime, StdDev) mustBe Some(0)
    }

    "return statistics for metrics with provided selectors" in {
      val transactions = Seq(
        Transaction(
          None,
          None,
          None,
          LocalDateTime.now,
          "",
          None,
          None,
          Map(ResponseTime -> 1)
        )
      )
      val selectors = MetricSelectors(
        Map(
          ResponseTime -> Seq(
            Sum,
            Max,
            P9999,
            P999,
            P99,
            P98,
            P95,
            P90,
            P75,
            Median,
            Min,
            Avg,
            StdDev
          )
        )
      )
      val stats = transactionStatistics(transactions, selectors)
      findStatistic(stats, ResponseTime, Sum) mustBe Some(1)
      findStatistic(stats, ResponseTime, Max) mustBe Some(1)
      findStatistic(stats, ResponseTime, P9999) mustBe Some(1)
      findStatistic(stats, ResponseTime, P999) mustBe Some(1)
      findStatistic(stats, ResponseTime, P99) mustBe Some(1)
      findStatistic(stats, ResponseTime, P98) mustBe Some(1)
      findStatistic(stats, ResponseTime, P95) mustBe Some(1)
      findStatistic(stats, ResponseTime, P90) mustBe Some(1)
      findStatistic(stats, ResponseTime, P75) mustBe Some(1)
      findStatistic(stats, ResponseTime, Median) mustBe Some(1)
      findStatistic(stats, ResponseTime, Min) mustBe Some(1)
      findStatistic(stats, ResponseTime, Avg) mustBe Some(1)
      findStatistic(stats, ResponseTime, StdDev) mustBe Some(0)
    }
  }
}
