package models

import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

import modules.SlickPostgresProfile.api._
import slick.lifted.{ProvenShape, Tag}

object User {

  private val PBKDF2_ALGORITHM: String = "PBKDF2WithHmacSHA1"
  private val SALT_BYTE_SIZE: Int = 24
  private val HASH_BYTE_SIZE: Int = 24
  private val PBKDF2_ITERATIONS: Int = 1000
  private val ITERATION_INDEX: Int = 0
  private val SALT_INDEX: Int = 1
  private val PBKDF2_INDEX: Int = 2

  private def slowEquals(a: Array[Byte], b: Array[Byte]): Boolean = {
    val range = 0 until math.min(a.length, b.length)
    val diff = range.foldLeft(a.length ^ b.length) {
      case (acc, i) => acc | a(i) ^ b(i)
    }
    diff == 0
  }

  private def pbkdf2(message: Array[Char], salt: Array[Byte], iterations: Int, bytes: Int): Array[Byte] = {
    val keySpec: PBEKeySpec = new PBEKeySpec(message, salt, iterations, bytes * 8)
    val skf: SecretKeyFactory = SecretKeyFactory.getInstance(PBKDF2_ALGORITHM)
    skf.generateSecret(keySpec).getEncoded
  }

  private def fromHex(hex: String): Array[Byte] = {
    hex.sliding(2, 2).toArray.map(Integer.parseInt(_, 16).toByte)
  }

  private def toHex(array: Array[Byte]): String = {
    array.map("%02X" format _).mkString
  }

  /**
    * Creates a PBKDF2 hash
    *
    * @param str
    * @return A hash of the form nIteration:salt:hash
    *         where salt and hash are in hex form
    */
  def pbkdf2Hash(str: String, iterations: Int = PBKDF2_ITERATIONS): String = {

    val rng: SecureRandom = new SecureRandom()
    val salt: Array[Byte] = Array.ofDim[Byte](SALT_BYTE_SIZE)
    rng.nextBytes(salt)
    val hashBytes = pbkdf2(str.toCharArray, salt, iterations, HASH_BYTE_SIZE)
    s"$iterations:${toHex(salt)}:${toHex(hashBytes)}"

  }

  /**
    * Validates a PBKDF2 hash
    *
    * @param str  The plain text you are confirming
    * @param hash The hash, in form of nIteration:salt:hash
    * @return
    */
  def validatePbkdf2Hash(str: String, hash: String): Boolean = {
    val hashSegments = hash.split(":")
    val validHash = fromHex(hashSegments(PBKDF2_INDEX))
    val hashIterations = hashSegments(ITERATION_INDEX).toInt
    val hashSalt = fromHex(hashSegments(SALT_INDEX))
    val testHash = pbkdf2(str.toCharArray, hashSalt, hashIterations, HASH_BYTE_SIZE)
    slowEquals(validHash, testHash)
  }





  def generatePublicKey: String = {

    val random = new SecureRandom()

    // exclude 58-64, 91-96
    def loop(list: List[Int]): List[Int] = list.length match {
      case 16 => list
      case tl if list.length < 16 => {
        val nextInt = random.nextInt(75) + 48
        if ( (nextInt >= 58 && nextInt <= 64) || (nextInt >= 91 && nextInt<= 96)) {
          loop(list)
        } else {
          loop(list :+ nextInt)
        }
      }
      case _ => loop(List())
    }

    loop(List()).map(_.toChar).mkString
  }

  def generatePrivateKey: String = {
    val random = new SecureRandom()
    (1 to 32).map { _ =>
      (random.nextInt(75) + 48).toChar
    }.mkString.replaceAll("\\\\+", "/")
  }

  def newUser(id: String, email: String, phone: String, password: String) = User(id, email, phone, pbkdf2Hash(password))

}

case class User(
               id: String,
               email: String,
               phone: String,
               pw: String
               ) {

  def validatePassword(password: String) = {
    User.validatePbkdf2Hash(password, this.pw)
  }

}

class UserTable(tag: Tag) extends Table[User](tag, "users") {

  def id: Rep[String] = column("id", O.PrimaryKey)
  def email: Rep[String] = column("email")
  def phone: Rep[String] = column("phone")
  def password: Rep[String] = column("password")

  override def * : ProvenShape[User] = (id, email, phone, password) <> ((User.apply _).tupled, User.unapply)
}