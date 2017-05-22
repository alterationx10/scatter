package modules

import javax.inject._

import com.amazonaws.auth.{AWSStaticCredentialsProvider, BasicAWSCredentials}
import com.amazonaws.services.s3.{AmazonS3, AmazonS3ClientBuilder}
import com.amazonaws.services.simpleemail.{AmazonSimpleEmailService, AmazonSimpleEmailServiceClientBuilder}
import com.amazonaws.services.sns.{AmazonSNS, AmazonSNSClientBuilder}
import com.google.inject.AbstractModule
import play.api.Configuration

case class AWSSettingsException(key: String) extends Exception(s"Missing key $key in config")

@Singleton
class AWS @Inject()(configuration: Configuration){

  private val configKey: String = "aws.accessKeyId"
  private val configSecret: String = "aws.secretAccessKey"
  private val configRegion: String = "aws.region"
  private val accessKey: String = configuration.getString(configKey).getOrElse(throw AWSSettingsException(configKey))
  private val secretKey: String = configuration.getString(configSecret).getOrElse(throw AWSSettingsException(configSecret))
  private val awsCreds: AWSStaticCredentialsProvider = new AWSStaticCredentialsProvider(new BasicAWSCredentials(accessKey, secretKey))

  val region: String = configuration.getString(configRegion).getOrElse("us-east-1")

  val s3Client: AmazonS3 = AmazonS3ClientBuilder.standard().withRegion(region).withCredentials(awsCreds).build()
  val snsClient: AmazonSNS = AmazonSNSClientBuilder.standard().withRegion(region).withCredentials(awsCreds).build()

}

class AWSProvider extends AbstractModule {
  override def configure(): Unit = {
    bind(classOf[AWS]).asEagerSingleton()
  }
}
