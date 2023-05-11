package models

object Gatling {

  case class Request(
      name: String,
      start: Long,
      end: Long,
      status: String,
      error: Option[String],
      code: Option[Int],
      url: Option[String],
      payload: Option[String]
  )

  object Request {

    /** Successful request example row:
      * REQUEST\tMySim\t7\t\tMyReq\t1476850562555\t1476850562684\tOK\t
      *
      * Failed request example row:
      * REQUEST\tMySim\t8\t\tMyReq\t1476850562755\t1476850562785\tKO\tstatus.is(200), but actually found 404
      *
      * Extended row example:
      * REQUEST\tMySim\t7\t\tMyReq\t1476850562555\t1476850562684\tOK\t\t200\thttp://localhost/myreq?foo=bar\t{"id":1}
      */
    def apply(row: String, extended: Boolean): Request = {
      val parts = row.split("\t")

      val start = parts(5).toLong
      val end   = parts(6).toLong

      val err = if (parts(7) == "KO" && parts(8).length > 0) {
        Some(parts(8))
      } else {
        None
      }

      val code = if (extended && parts.length > 9) {
        val part = parts(9).trim
        if (part.isEmpty) {
          None
        } else {
          Some(part.toInt)
        }
      } else {
        None
      }

      val url = if (extended && parts.length > 10) {
        Some(parts(10))
      } else {
        None
      }

      val payload = if (extended && parts.length > 11) {
        Some(parts(11).trim)
      } else {
        None
      }

      Request(parts(4), start, end, parts(7), err, code, url, payload)
    }
  }
}
