package models

import modules.SlickPostgresProfile.api._
import slick.lifted.ProvenShape

case class SessionUser(id: String, likedPosts: List[Long])

class SessionUserTable(tag: Tag) extends Table[SessionUser](tag, "session_users") {
  def id: Rep[String] = column("id", O.PrimaryKey)
  def likedPosts: Rep[List[Long]] = column("liked_posts")

  override def * : ProvenShape[SessionUser] = (id, likedPosts) <> ((SessionUser.apply _).tupled, SessionUser.unapply)
}