package modules

import javax.inject._

import com.google.inject.AbstractModule
import play.api.Configuration

@Singleton
class Initialization @Inject()(configuration: Configuration){

  // Set up AWS

  // Set up database tables

  // set up base user

}

class InitializationProvider extends AbstractModule {
  override def configure(): Unit = {
    bind(classOf[Initialization]).asEagerSingleton()
  }
}
