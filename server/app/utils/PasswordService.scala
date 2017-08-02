/*
 * AnyPlace: A free and open Indoor Navigation Service with superb accuracy!
 *
 * Anyplace is a first-of-a-kind indoor information service offering GPS-less
 * localization, navigation and search inside buildings using ordinary smartphones.
 *
 * Author(s): Constantinos Costa, Kyriakos Georgiou, Lambros Petrou
 *
 * Supervisor: Demetrios Zeinalipour-Yazti
 *
 * URL: https://anyplace.cs.ucy.ac.cy
 * Contact: anyplace@cs.ucy.ac.cy
 *
 * Copyright (c) 2016, Data Management Systems Lab (DMSL), University of Cyprus.
 * All rights reserved.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the “Software”), to deal in the
 * Software without restriction, including without limitation the rights to use, copy,
 * modify, merge, publish, distribute, sublicense, and/or sell copies of the Software,
 * and to permit persons to whom the Software is furnished to do so, subject to the
 * following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED “AS IS”, WITHOUT WARRANTY OF ANY KIND, EXPRESS
 * OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER
 * DEALINGS IN THE SOFTWARE.
 *
 */
package utils

import javax.crypto.SecretKeyFactory

import javax.crypto.spec.PBEKeySpec

import java.math.BigInteger

import java.security.NoSuchAlgorithmException

import java.security.SecureRandom

import java.security.spec.InvalidKeySpecException

object PasswordService {

  /** ******************************************************************************
    * USED FOR SECURE PASSWORD STORAGE - similar to secureEncrypt and secureDecrypt
    * in LPUtils but only up to the hashing step. No encryption is applied since
    * we only need one-direction hashing of the password and not decryption.
    * *******************************************************************************
    */

  val PBKDF2_ALGORITHM: String = "PBKDF2WithHmacSHA1"

  // The following constants may be changed without breaking existing hashes.
  val SALT_BYTE_SIZE: Int = 24

  val HASH_BYTE_SIZE: Int = 24

  val PBKDF2_ITERATIONS: Int = 1000

  val ITERATION_INDEX: Int = 0

  val SALT_INDEX: Int = 1

  val PBKDF2_INDEX: Int = 2

  /**
    * Returns a salted PBKDF2 hash of the password.
    *
    * @param   password the password to hash
    * @return a salted PBKDF2 hash of the password
    */
  def createHash(password: String): String = createHash(password.toCharArray())

  /**
    * Returns a salted PBKDF2 hash of the password.
    *
    * @param   password the password to hash
    * @return a salted PBKDF2 hash of the password
    */
  def createHash(password: Array[Char]): String = {
    // Generate a random salt
    val random: SecureRandom = new SecureRandom()
    val salt: Array[Byte] = Array.ofDim[Byte](SALT_BYTE_SIZE)
    random.nextBytes(salt)
    // Hash the password
    var hash: Array[Byte] = Array.ofDim[Byte](0)
    try hash = pbkdf2(password, salt, PBKDF2_ITERATIONS, HASH_BYTE_SIZE)
    catch {
      case e: NoSuchAlgorithmException => e.printStackTrace()

      case e: InvalidKeySpecException => e.printStackTrace()

    }
    // format iterations:salt:hash
    PBKDF2_ITERATIONS + ":" + toHex(salt) + ":" + toHex(hash)
  }

  /**
    * Validates a password using a hash.
    *
    * @param   password    the password to check
    * @param   correctHash the hash of the valid password
    * @return true if the password is correct, false if not
    */
  def validatePassword(password: String, correctHash: String): Boolean =
    validatePassword(password.toCharArray(), correctHash)

  /**
    * Validates a password using a hash.
    *
    * @param   password    the password to check
    * @param   correctHash the hash of the valid password
    * @return true if the password is correct, false if not
    */
  def validatePassword(password: Array[Char], correctHash: String): Boolean = {
    // Decode the hash into its parameters
    val params: Array[String] = correctHash.split(":")
    val iterations: Int = java.lang.Integer.parseInt(params(ITERATION_INDEX))
    val salt: Array[Byte] = fromHex(params(SALT_INDEX))
    val hash: Array[Byte] = fromHex(params(PBKDF2_INDEX))
    // iteration count, and hash length
    var testHash: Array[Byte] = Array.ofDim[Byte](0)
    try testHash = pbkdf2(password, salt, iterations, hash.length)
    catch {
      case e: NoSuchAlgorithmException => e.printStackTrace()

      case e: InvalidKeySpecException => e.printStackTrace()

    }
    // both hashes match.
    slowEquals(hash, testHash)
  }

  // Compute the hash of the provided password, using the same salt,
  // Compare the hashes in constant time. The password is correct if
  // Compute the hash of the provided password, using the same salt,
  // Compare the hashes in constant time. The password is correct if

  /**
    * Compares two byte arrays in length-constant time. This comparison method
    * is used so that password hashes cannot be extracted from an on-line
    * system using a timing attack and then attacked off-line.
    *
    * @param   a the first byte array
    * @param   b the second byte array
    * @return true if both byte arrays are the same, false if not
    */
  def slowEquals(a: Array[Byte], b: Array[Byte]): Boolean = {
    var diff: Int = a.length ^ b.length
    var i: Int = 0
    while (i < a.length && i < b.length) {
      diff |= a(i) ^ b(i)
      i += 1
    }
    diff == 0
  }

  /**
    * Computes the PBKDF2 hash of a password.
    *
    * @param   password   the password to hash.
    * @param   salt       the salt
    * @param   iterations the iteration count (slowness factor)
    * @param   bytes      the length of the hash to compute in bytes
    * @return the PBDKF2 hash of the password
    */
  def pbkdf2(password: Array[Char],
             salt: Array[Byte],
             iterations: Int,
             bytes: Int): Array[Byte] = {
    val spec: PBEKeySpec =
      new PBEKeySpec(password, salt, iterations, bytes * 8)
    val skf: SecretKeyFactory = SecretKeyFactory.getInstance(PBKDF2_ALGORITHM)
    skf.generateSecret(spec).getEncoded
  }

  /**
    * Converts a string of hexadecimal characters into a byte array.
    *
    * @param   hex the hex string
    * @return the hex string decoded into a byte array
    */
  def fromHex(hex: String): Array[Byte] = {
    val binary: Array[Byte] = Array.ofDim[Byte](hex.length / 2)
    for (i <- 0 until binary.length) {
      binary(i) =
        java.lang.Integer.parseInt(hex.substring(2 * i, 2 * i + 2), 16).toByte
    }
    binary
  }

  /**
    * Converts a byte array into a hexadecimal string.
    *
    * @param   array the byte array to convert
    * @return a length*2 character string encoding the byte array
    */
  def toHex(array: Array[Byte]): String = {
    val bi: BigInteger = new BigInteger(1, array)
    val hex: String = bi.toString(16)
    val paddingLength: Int = (array.length * 2) - hex.length
    if (paddingLength > 0) {
     val padding="%0" + paddingLength + "d"
      String.format(padding, new Integer(0)) + hex
    }else
      hex
  }

}