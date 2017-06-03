package modules

import java.io.{File, FileInputStream}
import java.security.{DigestInputStream, MessageDigest}
import javax.inject._

import com.amazonaws.auth.{AWSStaticCredentialsProvider, BasicAWSCredentials}
import com.amazonaws.services.s3.model.{ObjectMetadata, PutObjectRequest}
import com.amazonaws.services.s3.{AmazonS3, AmazonS3ClientBuilder}
import com.amazonaws.services.sns.{AmazonSNS, AmazonSNSClientBuilder}
import com.google.inject.AbstractModule
import play.api.Configuration
import play.api.libs.Files
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

  def uploadToS3(mfd: MultipartFormData.FilePart[Files.TemporaryFile], prefix: Option[String] = None): String = {
    val md5Name = s"${mfd.ref.file.md5}.${mfd.ref.file.extension}"
    val s3Key = prefix.map(p => s"files/$p/$md5Name").getOrElse(s"files/$md5Name").replaceAll("//","/")
    val por = new PutObjectRequest(s3Bucket, s3Key, mfd.ref.file)
    val omd = new ObjectMetadata()
    mfd.contentType.foreach(ct => omd.setContentType(ct))
    omd.setCacheControl("max-age=31536000")
    por.setMetadata(omd)
    s3Client.putObject(por)
    // We have a dotted bucket, so https will show insecure... Make URL the old fashioned way
    s"https://s3.amazonaws.com/$s3Bucket/$s3Key"
  }

  implicit class EnhancedFile(file: File) {

    def md5: String = {
      val md: MessageDigest = MessageDigest.getInstance("MD5")
      try {
        val is = new FileInputStream(file)
        val dis = new DigestInputStream(is, md)
        try {
          /* Read decorated stream (dis) to EOF as normal... */
        } finally {
          if (is != null) is.close()
          if (dis != null) dis.close()
        }
      }
      val digest: Array[Byte] = md.digest
      digest.map("%02x".format(_)).mkString
    }

    def extension: String = file.getName.split("\\.").toList.takeRight(1).headOption.getOrElse("")

  }

}

class AWSProvider extends AbstractModule {
  override def configure(): Unit = {
    bind(classOf[AWS]).asEagerSingleton()
  }
}
