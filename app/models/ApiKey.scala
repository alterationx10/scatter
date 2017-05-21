package models

import slick.lifted.{ProvenShape, Tag}
import modules.SlickPostgresProfile.api._

case class ApiKey(pub: String, priv: String)

class ApiKeyTable(tag: Tag) extends Table[ApiKey](tag, "api_keys") {

  def publicKey: Rep[String] = column("public", O.PrimaryKey)
  def privateKey: Rep[String] = column("private")

  override def * : ProvenShape[ApiKey] = (publicKey, privateKey) <> ((ApiKey.apply _).tupled, ApiKey.unapply)

}