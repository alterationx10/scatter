package models

import slick.lifted.{ProvenShape, Rep, Tag}
import modules.SlickPostgresProfile._

object Post {
  val TEXT: Int = 1
  val IMAGE: Int = 2
  val VIDEO: Int = 3
  val AUDIO: Int = 4
  val EXT_LINK: Int = 5
}

case class Post(
               id: Option[Long],
               date: Long,
               content: String,
               mediaType: Int,
               mediaLink: Option[String],
               tags: List[String],
               nLikes: Long
               )

class PostTable(tag: Tag) extends Table[Post](tag, "posts") {

  def id: Rep[Option[Long]] = column[Option[Long]]("id", O.PrimaryKey, O.AutoInc)
  def date: Rep[Long] = column[Long]("date")
  def content: Rep[String] = column[String]("content")
  def mediaType: Rep[Int] = column[Int]("media_type")
  def mediaLink: Rep[Option[String]] = column[Option[String]]("media_link")
  def tags: Rep[List[String]] = column[List[String]]("tags")
  def nLikes: Rep[Long] = column[Long]("n_likes")

  override def * : ProvenShape[Post] = ???
}