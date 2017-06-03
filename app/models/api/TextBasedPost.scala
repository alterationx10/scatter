package models.api

import play.api.libs.json.Json

case class TextBasedPost(
                   publicKey: String,
                   postContent: String,
                   extLink: Option[String]
                   )

object TextBasedPost {
  implicit val reads = Json.reads[TextBasedPost]
  implicit val writes = Json.writes[TextBasedPost]
}
