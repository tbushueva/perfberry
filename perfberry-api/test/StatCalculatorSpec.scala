import models.Metrics.MetricValue
import modules.StatCalculator
import org.scalatestplus.play._

class StatCalculatorSpec extends PlaySpec {
  val numbers: Seq[MetricValue] = Seq(15, 20, 35, 40, 50)
  val calc                      = new StatCalculator

  "A Stat Calculator" must {
    "return minimal value" in {
      calc.min(numbers) mustBe 15
    }

    "return maximum value" in {
      calc.max(numbers) mustBe 50
    }

    "return average value" in {
      calc.avg(numbers) mustBe 32
    }

    "return 50th percentile value" in {
      calc.percentile(numbers, 50) mustBe 35
    }
    "return 75th percentile value" in {
      calc.percentile(numbers, 75) mustBe 40
    }
    "return 95th percentile value" in {
      calc.percentile(numbers, 95) mustBe 48
    }
    "return 99th percentile value" in {
      calc.percentile(numbers, 99) mustBe 49.6
    }

    "return standard deviation value" in {
      calc.stdDev(numbers) mustBe 12.88
    }
  }
}
