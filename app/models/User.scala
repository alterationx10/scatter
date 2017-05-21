package models

import slick.lifted.{ProvenShape, Tag}
import modules.SlickPostgresProfile.api._

case class User(
               id: String,
               email: String,
               phone: String,
               pw: String
               )

class UserTable(tag: Tag) extends Table[User](tag, "users") {

  def id: Rep[String] = column("id", O.PrimaryKey)
  def email: Rep[String] = column("email")
  def phone: Rep[String] = column("phone")
  def password: Rep[String] = column("password")

  override def * : ProvenShape[User] = (id, email, phone, password) <> ((User.apply _).tupled, User.unapply)
}