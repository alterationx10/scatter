package modules

import com.github.tminglei.slickpg.{ExPostgresProfile, PgArraySupport}
import slick.basic.Capability


trait SlickPostgresProfile extends ExPostgresProfile with PgArraySupport {

  // Add back `capabilities.insertOrUpdate` to enable native `upsert` support; for postgres 9.5+
  override protected def computeCapabilities: Set[Capability] =
    super.computeCapabilities + slick.jdbc.JdbcCapabilities.insertOrUpdate

  object SlickAPI extends API with ArrayImplicits {
    implicit val strListTypeMapper: DriverJdbcType[List[String]] = new SimpleArrayJdbcType[String]("text").to(_.toList)
  }

  override val api = SlickAPI

}

object SlickPostgresProfile extends SlickPostgresProfile