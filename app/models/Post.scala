package models

import java.awt.PageAttributes.MediaType

import slick.lifted.{ProvenShape, Tag}
import modules.SlickPostgresProfile.api._

object Post {
  val TEXT: Int = 1
  val IMAGE: Int = 2
  val VIDEO: Int = 3
  val AUDIO: Int = 4
  val EXT_LINK: Int = 5

  def parseTags(content: String): List[String] = {
    content.split(" ").toList.filter(_.startsWith("#")).map(_.replaceAll("#",""))
  }

  def newPost(content: String, mediaType: Int = TEXT, mediaLink: Option[String] = None): Post = {
    Post(
      None,
      System.currentTimeMillis(),
      content,
      mediaType,
      mediaLink,
      parseTags(content),
      0
    )
  }

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

  def id: Rep[Option[Long]] = column("id", O.PrimaryKey, O.AutoInc)
  def date: Rep[Long] = column("date")
  def content: Rep[String] = column("content")
  def mediaType: Rep[Int] = column("media_type")
  def mediaLink: Rep[Option[String]] = column("media_link")
  def tags: Rep[List[String]] = column("tags")
  def nLikes: Rep[Long] = column("n_likes")

  override def * : ProvenShape[Post] = (id, date, content, mediaType, mediaLink, tags, nLikes) <> ((Post.apply _).tupled, Post.unapply)
}