package controllers

import javax.inject._
import play.api.mvc.{Action, Controller}

@Singleton
class ApplicationController extends Controller {

  def login = TODO
  def logout = TODO

  /*
  View a timeline of posts
   */
  def index = Action {
    Ok("It works!")
  }

  /*
  View a single post
   */
  def post = TODO

  /*
  Submit a new post
   */
  def submit = TODO

  /*
  Serve S3 file
   */
  def s3File(key: String) = TODO

}
