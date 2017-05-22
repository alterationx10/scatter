package controllers

import play.api.mvc.{Action, Controller}


class ApplicationController extends Controller {

  def login = TODO
  def logout = TODO
  def index = Action {
    Ok("It works!")
  }
  def post = TODO
  def submit = TODO

  def s3File(key: String) = TODO

}
