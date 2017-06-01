package controllers

import java.util.UUID
import javax.inject._

import models._
import modules.SlickPostgresProfile.api._
import modules.{AWS, SlickPostgres}
import play.api.cache.CacheApi
import play.api.libs.Files
import play.api.libs.crypto.CookieSigner
import play.api.libs.json.Json
import play.api.mvc._
import slick.lifted.TableQuery

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.Try


@Singleton
class ApplicationController @Inject()(
                                       postgres: SlickPostgres,
                                       cacheApi: CacheApi,
                                       aws: AWS,
                                       cookieSigner: CookieSigner
                                     ) extends Controller {


  def getPostParameter(key: String)(implicit request: Request[AnyContent]): Option[String] = {
    request.body.asFormUrlEncoded.flatMap(_.get(key).flatMap(_.headOption))
  }

  val postQuery: TableQuery[PostTable] = TableQuery[PostTable]
  val postInsertQuery= postQuery returning postQuery.map(_.id) into ((post, id) => post.copy(id = id))
  val sessionUserQuery: TableQuery[SessionUserTable] = TableQuery[SessionUserTable]
  val userQuery: TableQuery[UserTable] = TableQuery[UserTable]

  /*
  View a timeline of posts
   */
  def index = Action.async { implicit request =>

    val page: Int = request.getQueryString("page").flatMap(p => Try(p.toInt).toOption).getOrElse(0)
    val tags: Option[List[String]] = request.getQueryString("tags").map(_.split(",").toList.map(_.toLowerCase))
    val nPosts = 10

    val filteredPostQuery = tags match {
      case Some(tags) => postQuery.filter(_.tags @& tags)
      case None => postQuery
    }

    for {
      likedPosts <- postgres.db.run(sessionUserQuery.filter(_.id === request.session.get("session_id").getOrElse("")).result).map(_.headOption).map(_.map(_.likedPosts).getOrElse(List()))
      posts <- postgres.db.run(filteredPostQuery.sortBy(_.id.desc).drop(page * nPosts).take(nPosts).result)
    } yield {
      val linkedPostContent = posts.map{ post =>
        val linked = post.content.split(" ").map{ t =>
          if (t.startsWith("#")) {
            s"<a href=${routes.ApplicationController.index()}?tags=${t.replaceAll("#","")}>$t</a>"
          } else {
            t
          }
        }.mkString(" ")
        post.copy(content = linked)
      }
      Ok(views.html.index(linkedPostContent, likedPosts, page)).withSession(request.session)
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

  def login = Action {
    Ok(views.html.login())
  }

  def login_POST = Action.async { implicit request =>

    val userAuth = for {
      user <- getPostParameter("user")
      password <- getPostParameter("password")
    } yield {
      postgres.db.run(userQuery.filter(_.id === user).take(1).result).map(_.headOption.exists(_.validatePassword(password))).map {
        case true => Redirect(routes.ApplicationController.index()).withSession(request.session + ("user" -> user))
        case false => Forbidden
      }
    }.recover{
      case _: Exception => Forbidden
    }

    userAuth.getOrElse(Future.successful(Forbidden))
  }

  def logout = Action { implicit request =>
    Redirect(routes.ApplicationController.index()).withSession(request.session - "user")
  }

  /*
  Submit some content
   */
  class UserRequest[A](val user: Option[String], request: Request[A]) extends WrappedRequest[A](request)
  object UserAction extends
    ActionBuilder[UserRequest] with ActionTransformer[Request, UserRequest] {
    def transform[A](request: Request[A]) = Future.successful {
      new UserRequest(request.session.get("user"), request)
    }
  }
  object PermissionCheckAction extends ActionFilter[UserRequest] {
    def filter[A](user: UserRequest[A]) = Future.successful {
      if (user.user.isEmpty)
        Some(Forbidden)
      else
        None
    }
  }

  def submit = (UserAction andThen PermissionCheckAction) { implicit request =>
    Ok(views.html.submit())
  }

  def submit_POST = (UserAction andThen PermissionCheckAction).async { implicit request =>


    val mfd = request.body.asMultipartFormData

    val postContent: Option[String] = mfd.map(_.dataParts).flatMap(_.get("postContent")).flatMap(_.headOption)
    val mediaType: Option[Int] = mfd.map(_.dataParts).flatMap(_.get("mediaType")).flatMap(_.headOption).flatMap(i => Try(i.toInt).toOption)
    val uploadedFile: Option[MultipartFormData.FilePart[Files.TemporaryFile]] = mfd.flatMap(_.files.headOption)

    Future {
      mediaType.flatMap {
        case Post.IMAGE => uploadedFile.map(mfd => aws.uploadToS3(mfd, Option("images")))
        case Post.VIDEO => uploadedFile.map(mfd => aws.uploadToS3(mfd, Option("video")))
        case Post.AUDIO => uploadedFile.map(mfd => aws.uploadToS3(mfd, Option("audio")))
        case Post.EXT_LINK => mfd.map(_.dataParts).flatMap(_.get("mediaLink")).flatMap(_.headOption)
        case _ => None
      }
    }.map { mediaLink =>
      Post.newPost(
        postContent.getOrElse(""),
        mediaType = mediaType.getOrElse(Post.TEXT),
        mediaLink
      )
    }.flatMap {post =>
      postgres.db.run(postInsertQuery += post).map(_ => Redirect(routes.ApplicationController.index()))
    }.recover {
      case e: Exception => UnprocessableEntity(e.getMessage)
    }

  }


  def likePost(id: Long) = Action.async { implicit request =>

    val sessionId: String = request.session.get("session_id").getOrElse(UUID.randomUUID().toString)
    val updatedSession = request.session + ("session_id" -> sessionId)

    postgres.db.run(postQuery.filter(_.id === id).result).map(_.headOption).flatMap {
      case Some(post) => {
        postgres.db.run(sessionUserQuery.filter(_.id === sessionId).result).map(_.headOption).flatMap {
          case Some(sUser) => {
            if (sUser.likedPosts.contains(post.id.get)) {
              Future.successful(Ok(Json.obj("hearts"-> post.nLikes)).withSession(updatedSession))
            } else {
              postgres.db.run(sessionUserQuery.insertOrUpdate(sUser.copy(likedPosts = sUser.likedPosts :+ id)))
              postgres.db.run(postQuery.insertOrUpdate(post.copy(nLikes = post.nLikes + 1))).map(_ => Ok(Json.obj("hearts"->(post.nLikes+1))).withSession(updatedSession))
            }
          }
          case None => {
            postgres.db.run(sessionUserQuery += SessionUser(sessionId, List(id))).flatMap { _ =>
              postgres.db.run(postQuery.insertOrUpdate(post.copy(nLikes = post.nLikes + 1))).map(_ => Ok(Json.obj("hearts"->(post.nLikes+1))).withSession(updatedSession))
            }
          }
        }
      }
      case None => Future.successful(NotFound)
    }
  }

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
