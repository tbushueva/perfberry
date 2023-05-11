import models.{Assertion, AssertionResult, Assertions}
import org.scalatestplus.play._
import models.Metrics._
import models.AssertionCondition._

class AssertionsSpec extends PlaySpec {

  val baseAssert: Assertion =
    Assertion(None, ResponseTime, Some(P95), Lt, 100, None)

  "A getStatus" must {
    "return None with empty assertions" in {
      Assertions.empty.status mustBe None
    }

    "return true when only passed assertions" in {
      val passedAssertion = baseAssert
        .copy(result = Some(AssertionResult(0, passed = true)))
      Assertions(Seq(passedAssertion)).status mustBe Some(true)
    }

    "return false when failed assertions exists" in {
      val failedAssertion = baseAssert
        .copy(result = Some(AssertionResult(0, passed = false)))
      Assertions(Seq(failedAssertion)).status mustBe Some(false)
    }
  }
}
