package modules

import javax.inject._

import com.google.inject.AbstractModule
import modules.SlickPostgresProfile.api._
import play.api.Logger
import play.api.inject.ApplicationLifecycle

import scala.concurrent.Future

@Singleton
class SlickPostgres @Inject() (lifecycle: ApplicationLifecycle) {

  Logger.info("Starting up database connection...")
  val db: Database = Database.forConfig("scatter.postgres")

  lifecycle.addStopHook { () =>
    Future.successful{
      Logger.info("Shutting down database connection...")
      db.close()
    }
  }

}

class SlickPostgresProvider extends AbstractModule {
  override def configure(): Unit = {
    bind(classOf[SlickPostgres])
  }
}