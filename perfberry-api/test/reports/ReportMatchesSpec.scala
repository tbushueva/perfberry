package reports

import models._
import models.Reports._
import org.scalatestplus.play._

class ReportMatchesSpec extends PlaySpec {

  "A report matches function" must {

    val emptyReport = Report.empty

    "match some label" in {
      val withLabel = Report.empty.copy(label = Some("foo"))

      withLabel.matches(SearchQuery("label:foo")) mustBe true
      withLabel.matches(SearchQuery("!label:foo")) mustBe false
    }

    "match empty label" in {
      emptyReport.matches(SearchQuery("label:foo")) mustBe false
      emptyReport.matches(SearchQuery("!label:foo")) mustBe true
    }

    "match some description" in {
      val withDescription = Report.empty.copy(description = Some("foo"))

      withDescription.matches(SearchQuery("description:foo")) mustBe true
      withDescription.matches(SearchQuery("!description:foo")) mustBe false
    }

    "match empty description" in {
      emptyReport.matches(SearchQuery("description:foo")) mustBe false
      emptyReport.matches(SearchQuery("!description:foo")) mustBe true
    }

    "match some reference" in {
      val scm           = ScmInfo.empty.copy(vcs = Some(VcsInfo("ref", "rev", None)))
      val withReference = Report.empty.copy(scm = Some(scm))

      withReference.matches(SearchQuery("scm.vcs.reference:ref")) mustBe true
      withReference.matches(SearchQuery("!scm.vcs.reference:ref")) mustBe false
    }

    "match empty reference" in {
      emptyReport.matches(SearchQuery("scm.vcs.reference:ref")) mustBe false
      emptyReport.matches(SearchQuery("!scm.vcs.reference:ref")) mustBe true
    }

    "match some revision" in {
      val scm          = ScmInfo.empty.copy(vcs = Some(VcsInfo("ref", "rev", None)))
      val withRevision = Report.empty.copy(scm = Some(scm))

      withRevision.matches(SearchQuery("scm.vcs.revision:rev")) mustBe true
      withRevision.matches(SearchQuery("!scm.vcs.revision:rev")) mustBe false
    }

    "match empty revision" in {
      emptyReport.matches(SearchQuery("scm.vcs.revision:rev")) mustBe false
      emptyReport.matches(SearchQuery("!scm.vcs.revision:rev")) mustBe true
    }

    "match some title" in {
      val scm =
        ScmInfo.empty.copy(vcs = Some(VcsInfo("ref", "rev", Some("foo"))))
      val withRevision = Report.empty.copy(scm = Some(scm))

      withRevision.matches(SearchQuery("scm.vcs.title:foo")) mustBe true
      withRevision.matches(SearchQuery("!scm.vcs.title:foo")) mustBe false
    }

    "match empty title with vcs" in {
      val scm     = ScmInfo.empty.copy(vcs = Some(VcsInfo("ref", "rev", None)))
      val withVcs = Report.empty.copy(scm = Some(scm))

      withVcs.matches(SearchQuery("scm.vcs.title:foo")) mustBe false
      withVcs.matches(SearchQuery("!scm.vcs.title:foo")) mustBe true
    }

    "match empty title without vcs" in {
      emptyReport.matches(SearchQuery("scm.vcs.title:foo")) mustBe false
      emptyReport.matches(SearchQuery("!scm.vcs.title:foo")) mustBe true
    }
  }
}
