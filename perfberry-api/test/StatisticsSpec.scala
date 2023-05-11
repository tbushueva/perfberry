import java.time.LocalDateTime

import models.Metrics.{MetricSelectors, statistics}
import models.Transaction
import org.scalatestplus.play._

class StatisticsSpec extends PlaySpec {

  "A statistics function" must {

    "return only global stats with empty groups" in {
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
      val stats = statistics(transactions, MetricSelectors.empty)

      stats.global.items.nonEmpty mustBe true
      stats.groups.isEmpty mustBe true
    }

    "return group stats for grouped transactions" in {
      val transactions = Seq(
        Transaction(
          Some("g"),
          None,
          None,
          LocalDateTime.now,
          "",
          None,
          None,
          Map.empty
        )
      )
      val stats = statistics(transactions, MetricSelectors.empty)

      stats.groups.find(_.name == "g").map(_.items.nonEmpty) mustBe Some(true)
      stats.groups.length mustBe 1
    }
  }
}
