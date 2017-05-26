package controllers

import javax.inject._

import models.{Post, PostTable}
import modules.SlickPostgres
import play.api.mvc.{Action, Controller}
import slick.lifted.TableQuery
import modules.SlickPostgresProfile.api._
import play.api.libs.json.Json

import scala.concurrent.ExecutionContext.Implicits.global

@Singleton
class ApplicationController @Inject()(postgres: SlickPostgres) extends Controller {

  val postQuery: TableQuery[PostTable] = TableQuery[PostTable]

  /*
  View a timeline of posts
   */
  def index = Action.async {

    postgres.db.run(postQuery.sortBy(_.id.desc).take(10).result).map{ posts =>
      Ok(views.html.index(posts))
    }

  }

  /*
  View a single post
   */
  def post(id: Long) = Action.async {

    postgres.db.run(postQuery.filter(_.id === id).result).map(_.headOption).map {
      case Some(post) => Ok(views.html.post(post))
      case None => NotFound
    }

  }

  /*
  Submit a new post
   */
  def submit = TODO

  /*
  Serve S3 file
   */
  def s3File(key: String) = TODO

  def postStats = Action.async {

    postgres.db.run(postQuery.map(_.nLikes).result).map { likeSeq =>
      Ok(
        Json.obj(
          "posts" -> likeSeq.length,
          "hearts" -> likeSeq.sum
        )
      )
    }

  }

}
