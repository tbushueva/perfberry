import models._

import org.scalatestplus.play._

class SearchQuerySpec extends PlaySpec {

  "A search query apply function" must {

    "return empty seq" in {
      SearchQuery("").parts mustBe Seq.empty
    }

    "return inclusive label part" in {
      val part =
        SearchQuery("label:foo").parts.head.asInstanceOf[LabelSearchQueryPart]

      part.isInlcusive mustBe true
      part.label mustBe "foo"
    }

    "return exclusive label part" in {
      val part =
        SearchQuery("!label:foo").parts.head.asInstanceOf[LabelSearchQueryPart]

      part.isExlcusive mustBe true
      part.label mustBe "foo"
    }

    "return inclusive description part" in {
      val part = SearchQuery("description:foo").parts.head
        .asInstanceOf[DescriptionSearchQueryPart]

      part.isInlcusive mustBe true
      part.description mustBe "foo"
    }

    "return exclusive description part" in {
      val part = SearchQuery("!description:foo").parts.head
        .asInstanceOf[DescriptionSearchQueryPart]

      part.isExlcusive mustBe true
      part.description mustBe "foo"
    }

    "return inclusive reference part" in {
      val part = SearchQuery("scm.vcs.reference:foo").parts.head
        .asInstanceOf[VcsReferenceSearchQueryPart]

      part.isInlcusive mustBe true
      part.reference mustBe "foo"
    }

    "return exclusive reference part" in {
      val part = SearchQuery("!scm.vcs.reference:foo").parts.head
        .asInstanceOf[VcsReferenceSearchQueryPart]

      part.isExlcusive mustBe true
      part.reference mustBe "foo"
    }

    "return inclusive revision part" in {
      val part = SearchQuery("scm.vcs.revision:foo").parts.head
        .asInstanceOf[VcsRevisionSearchQueryPart]

      part.isInlcusive mustBe true
      part.revision mustBe "foo"
    }

    "return exclusive revision part" in {
      val part = SearchQuery("!scm.vcs.revision:foo").parts.head
        .asInstanceOf[VcsRevisionSearchQueryPart]

      part.isExlcusive mustBe true
      part.revision mustBe "foo"
    }

    "return inclusive title part" in {
      val part = SearchQuery("scm.vcs.title:foo").parts.head
        .asInstanceOf[VcsTitleSearchQueryPart]

      part.isInlcusive mustBe true
      part.title mustBe "foo"
    }

    "return exclusive title part" in {
      val part = SearchQuery("!scm.vcs.title:foo").parts.head
        .asInstanceOf[VcsTitleSearchQueryPart]

      part.isExlcusive mustBe true
      part.title mustBe "foo"
    }

    "return part with spaces" in {
      val part = SearchQuery("label:foo bar").parts.head
        .asInstanceOf[LabelSearchQueryPart]

      part.label mustBe "foo bar"
    }

    "return two parts" in {
      val parts = SearchQuery("label:foo label:bar").parts
      val part1 = parts.head.asInstanceOf[LabelSearchQueryPart]
      val part2 = parts(1).asInstanceOf[LabelSearchQueryPart]

      part1.label mustBe "foo"
      part2.label mustBe "bar"
    }
  }
}
