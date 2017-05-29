package controllers

import java.util.UUID
import javax.inject._

import com.amazonaws.services.s3.model.PutObjectRequest
import models.{Post, PostTable, SessionUser, SessionUserTable}
import modules.SlickPostgres
import play.api.mvc.{Action, AnyContent, Controller, Request}
import slick.lifted.TableQuery
import modules.SlickPostgresProfile.api._
import play.api.Logger
import play.api.cache.CacheApi
import play.api.libs.json.Json

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.Try

@Singleton
class ApplicationController @Inject()(postgres: SlickPostgres, cacheApi: CacheApi) extends Controller {

  def getPostParameter(key: String)(implicit request: Request[AnyContent]): Option[String] = {
    request.body.asFormUrlEncoded.flatMap(_.get(key).flatMap(_.headOption))
  }

  def getTextPost(implicit request: Request[AnyContent]): Option[Post] = {
    getPostParameter("post").map(p => Post.newPost(p))
  }

  def getLinkPost(implicit request: Request[AnyContent]): Option[Post] = {
    for {
      post <- getPostParameter("post")
      mediaType <- getPostParameter("mediaType").flatMap(mt => Try(mt.toInt).toOption)
      mediaLink <- getPostParameter("mediaLink")
    } yield {
      Post.newPost(post, mediaType, Option(mediaLink))
    }
  }


  def getMediaPost(implicit request: Request[AnyContent]): Future[Option[Post]] = {
    val multipart = request.body.asMultipartFormData
    Future {
      for {
        post <- multipart.flatMap(_.dataParts.get("post").flatMap(_.headOption))
        mediaType <- getPostParameter("mediaType").flatMap(mt => Try(mt.toInt).toOption)
        file <- multipart.flatMap(_.file("media"))
      } yield {
        // Store on S3
        // TODO
        Post.newPost(post, mediaType)
      }
    }
  }




  val postQuery: TableQuery[PostTable] = TableQuery[PostTable]
  val postInsertQuery= postQuery returning postQuery.map(_.id) into ((post, id) => post.copy(id = id))
  val sessionUserQuery: TableQuery[SessionUserTable] = TableQuery[SessionUserTable]

  /*
  View a timeline of posts
   */
  def index = Action.async { implicit request =>

    for {
      likedPosts <- postgres.db.run(sessionUserQuery.filter(_.id === request.session.get("session_id").getOrElse("")).result).map(_.headOption).map(_.map(_.likedPosts).getOrElse(List()))
      posts <- postgres.db.run(postQuery.sortBy(_.id.desc).take(10).result)
    } yield {
      Ok(views.html.index(posts, likedPosts)).withSession(request.session)
    }

  }

  /*
  View a single post
   */
  def post(id: Long) = Action.async { implicit request =>

    for {
      likedPosts <- postgres.db.run(sessionUserQuery.filter(_.id === request.session.get("session_id").getOrElse("")).result).map(_.headOption).map(_.map(_.likedPosts).getOrElse(List()))
      postOpt <- postgres.db.run(postQuery.filter(_.id === id).result).map(_.headOption)
    } yield {
      postOpt match {
        case Some(post) => Ok(views.html.post(post, likedPosts)).withSession(request.session)
        case None => NotFound
      }
    }

  }

  /*
  Submit a new text post
   */
  def submit = Action.async { implicit request =>
    getTextPost match {
      case Some(post) => {
        postgres.db.run(postInsertQuery += post).map(p => Created(p.id))
      }
      case None => Future.successful(UnprocessableEntity)
    }
  }

  /*
  Submit an external link post
   */
  def submitLinkPost = Action.async {

    getLinkPost match {
      case Some(post) => {
        post.mediaType match {
          case Post.EXT_LINK => postgres.db.run(postInsertQuery += post).map(p => Created(p.id))
          case _ => Future.successful(UnprocessableEntity)
        }
      }
      case None => Future.successful(UnprocessableEntity)
    }

  }

  def submitMediaPost = Action.async { implicit request =>

    ???
  }

  def likePost(id: Long) = Action.async { implicit request =>

    val sessionId: String = request.session.get("session_id").getOrElse(UUID.randomUUID().toString)

    postgres.db.run(postQuery.filter(_.id === id).result).map(_.headOption).flatMap {
      case Some(post) => {
        postgres.db.run(sessionUserQuery.filter(_.id === sessionId).result).map(_.headOption).flatMap {
          case Some(sUser) => {
            if (sUser.likedPosts.contains(post.id.get)) {
              Future.successful(Ok(Json.obj("hearts"-> post.nLikes)).withSession("session_id" -> sessionId))
            } else {
              postgres.db.run(sessionUserQuery.insertOrUpdate(sUser.copy(likedPosts = sUser.likedPosts :+ id)))
              postgres.db.run(postQuery.insertOrUpdate(post.copy(nLikes = post.nLikes + 1))).map(_ => Ok(Json.obj("hearts"->(post.nLikes+1))).withSession("session_id" -> sessionId))
            }
          }
          case None => {
            postgres.db.run(sessionUserQuery += SessionUser(sessionId, List(id))).flatMap { _ =>
              postgres.db.run(postQuery.insertOrUpdate(post.copy(nLikes = post.nLikes + 1))).map(_ => Ok(Json.obj("hearts"->(post.nLikes+1))).withSession("session_id" -> sessionId))
            }
          }
        }
      }
      case None => Future.successful(NotFound)
    }
  }

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
