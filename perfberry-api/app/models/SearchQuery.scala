package models

import play.api.mvc.QueryStringBindable

sealed trait SearchQueryPart {

  def isInlcusive: Boolean

  def isExlcusive: Boolean  = !isInlcusive
  val exclusiveChar: String = "!"

  def key: String
}

case class LabelSearchQueryPart(isInlcusive: Boolean, label: String) extends SearchQueryPart {
  val key = "label"
}

object LabelSearchQueryPart {

  def apply(query: String): Option[LabelSearchQueryPart] =
    query.split(":").toList match {
      case "label" :: label :: Nil =>
        Some(LabelSearchQueryPart(isInlcusive = true, label))
      case "!label" :: label :: Nil =>
        Some(LabelSearchQueryPart(isInlcusive = false, label))
      case _ => None
    }
}

case class DescriptionSearchQueryPart(isInlcusive: Boolean, description: String)
    extends SearchQueryPart {
  val key = "description"
}

object DescriptionSearchQueryPart {

  def apply(query: String): Option[DescriptionSearchQueryPart] =
    query.split(":").toList match {
      case "description" :: label :: Nil =>
        Some(DescriptionSearchQueryPart(isInlcusive = true, label))
      case "!description" :: label :: Nil =>
        Some(DescriptionSearchQueryPart(isInlcusive = false, label))
      case _ => None
    }
}

case class VcsReferenceSearchQueryPart(isInlcusive: Boolean, reference: String)
    extends SearchQueryPart {
  val key = "scm.vcs.reference"
}

object VcsReferenceSearchQueryPart {

  def apply(query: String): Option[VcsReferenceSearchQueryPart] =
    query.split(":").toList match {
      case "scm.vcs.reference" :: reference :: Nil =>
        Some(VcsReferenceSearchQueryPart(isInlcusive = true, reference))
      case "!scm.vcs.reference" :: reference :: Nil =>
        Some(VcsReferenceSearchQueryPart(isInlcusive = false, reference))
      case _ => None
    }
}

case class VcsRevisionSearchQueryPart(isInlcusive: Boolean, revision: String)
    extends SearchQueryPart {
  val key = "scm.vcs.revision"
}

object VcsRevisionSearchQueryPart {

  def apply(query: String): Option[VcsRevisionSearchQueryPart] =
    query.split(":").toList match {
      case "scm.vcs.revision" :: revision :: Nil =>
        Some(VcsRevisionSearchQueryPart(isInlcusive = true, revision))
      case "!scm.vcs.revision" :: revision :: Nil =>
        Some(VcsRevisionSearchQueryPart(isInlcusive = false, revision))
      case _ => None
    }
}

case class VcsTitleSearchQueryPart(isInlcusive: Boolean, title: String) extends SearchQueryPart {
  val key = "scm.vcs.title"
}

object VcsTitleSearchQueryPart {

  def apply(query: String): Option[VcsTitleSearchQueryPart] =
    query.split(":").toList match {
      case "scm.vcs.title" :: title :: Nil =>
        Some(VcsTitleSearchQueryPart(isInlcusive = true, title))
      case "!scm.vcs.title" :: title :: Nil =>
        Some(VcsTitleSearchQueryPart(isInlcusive = false, title))
      case _ => None
    }
}

case class SearchQuery(parts: Seq[SearchQueryPart]) extends AnyVal

object SearchQuery {

  def parse(query: String): Option[SearchQueryPart] =
    LabelSearchQueryPart(query)
      .orElse(DescriptionSearchQueryPart(query))
      .orElse(VcsReferenceSearchQueryPart(query))
      .orElse(VcsRevisionSearchQueryPart(query))
      .orElse(VcsTitleSearchQueryPart(query))

  def apply(query: String): SearchQuery = {
    val parts = query.split(" ").toSeq
    val results =
      parts.foldRight((None: Option[String], Seq[SearchQueryPart]())) { (part, prevPartAndResult) =>
        val q = prevPartAndResult._1 match {
          case Some(p) => part + " " + p
          case None    => part
        }
        parse(q) match {
          case Some(f) => (None, f +: prevPartAndResult._2)
          case None    => (Some(part), prevPartAndResult._2)
        }
      }
    SearchQuery(results._2)
  }

  val empty = SearchQuery(Seq.empty)

  implicit def queryBinder(implicit
      stringBinder: QueryStringBindable[String]
  ): QueryStringBindable[SearchQuery] = new QueryStringBindable[SearchQuery] {

    def bind(
        key: String,
        params: Map[String, Seq[String]]
    ): Option[Either[String, SearchQuery]] = {
      stringBinder.bind(key, params).map { param =>
        param match {
          case Right(q) => Right(SearchQuery(q))
          case Left(_)  => Left("Unable to bind search query")
        }
      }
    }

    def unbind(key: String, value: SearchQuery): String = {
      val vs = value.parts.map { part =>
        val condition = if (part.isExlcusive) part.exclusiveChar else ""
        val query = part match {
          case LabelSearchQueryPart(_, l)        => part.key + ":" + l
          case DescriptionSearchQueryPart(_, d)  => part.key + ":" + d
          case VcsReferenceSearchQueryPart(_, r) => part.key + ":" + r
          case VcsRevisionSearchQueryPart(_, r)  => part.key + ":" + r
          case VcsTitleSearchQueryPart(_, t)     => part.key + ":" + t
        }
        condition + query
      }
      stringBinder.unbind(key, vs.mkString(" "))
    }
  }
}
