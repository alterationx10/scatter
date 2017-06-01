package modules

import java.util.UUID
import javax.inject._

import com.amazonaws.auth.{AWSStaticCredentialsProvider, BasicAWSCredentials}
import com.amazonaws.services.s3.model.{ObjectMetadata, PutObjectRequest}
import com.amazonaws.services.s3.{AmazonS3, AmazonS3ClientBuilder}
import com.amazonaws.services.sns.{AmazonSNS, AmazonSNSClientBuilder}
import com.google.inject.AbstractModule
import play.api.Configuration
import play.api.libs.{Files, MimeTypes}
import play.api.mvc.MultipartFormData

case class AWSSettingsException(key: String) extends Exception(s"Missing key $key in config")

@Singleton
class AWS @Inject()(configuration: Configuration){

  private val configKey: String = "scatter.aws.accessKeyId"
  private val configSecret: String = "scatter.aws.secretAccessKey"
  private val configRegion: String = "scatter.aws.region"
  private val accessKey: String = configuration.getString(configKey).getOrElse(throw AWSSettingsException(configKey))
  private val secretKey: String = configuration.getString(configSecret).map(_.replaceAll("'","")).getOrElse(throw AWSSettingsException(configSecret))
  private val awsCreds: AWSStaticCredentialsProvider = new AWSStaticCredentialsProvider(new BasicAWSCredentials(accessKey, secretKey))

  val region: String = configuration.getString(configRegion).getOrElse("us-east-1")

  val s3Client: AmazonS3 = AmazonS3ClientBuilder.standard().withRegion(region).withCredentials(awsCreds).build()
  val snsClient: AmazonSNS = AmazonSNSClientBuilder.standard().withRegion(region).withCredentials(awsCreds).build()

  val s3Bucket: String = "vln.evillair.io"

  val extMap: Map[String, String] = MimeTypes.defaultTypes.filterKeys(k => !k.equals("x-png")).map(_.swap)

  def uploadToS3(mfd: MultipartFormData.FilePart[Files.TemporaryFile], prefix: Option[String] = None): String = {
    val extension: String = mfd.contentType.flatMap(ct => extMap.get(ct)).map(ext => s".$ext").getOrElse("")
    val s3Key = prefix.map(p => s"files/$p/${UUID.randomUUID().toString}$extension").getOrElse(s"files/${UUID.randomUUID().toString}$extension").replaceAll("//","/")
    val por = new PutObjectRequest(s3Bucket, s3Key, mfd.ref.file)
    val omd = new ObjectMetadata()
    mfd.contentType.foreach(ct => omd.setContentType(ct))
    omd.setCacheControl("max-age=31536000")
    s3Client.putObject(por)
    // We have a dotted bucket, so https will show insecure... Make URL the old fashioned way
    s"https://s3.amazonaws.com/$s3Bucket/$s3Key"
  }

}

class AWSProvider extends AbstractModule {
  override def configure(): Unit = {
    bind(classOf[AWS]).asEagerSingleton()
  }
}
