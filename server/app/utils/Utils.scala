/*
 * Anyplace: A free and open Indoor Navigation Service with superb accuracy!
 *
 * Anyplace is a first-of-a-kind indoor information service offering GPS-less
 * localization, navigation and search inside buildings using ordinary smartphones.
 *
 * Author(s): Constantinos Costa, Kyriakos Georgiou, Lambros Petrou, Paschalis Mpeis
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

import datasources.SCHEMA

import java.security.{InvalidAlgorithmParameterException, MessageDigest, NoSuchAlgorithmException, SecureRandom}
import java.util.{Date, UUID}
import javax.crypto._
import javax.crypto.spec.{IvParameterSpec, PBEKeySpec, SecretKeySpec}
import java.text.SimpleDateFormat
import java.io.{ByteArrayInputStream, ByteArrayOutputStream, File, UnsupportedEncodingException}
import org.apache.commons.codec.binary.Base64
import play.api.libs.json.{JsObject, JsValue}

import java.util.zip.GZIPOutputStream

object Utils {
    private val SECURE_ITERATIONS = 1000
    private val SECURE_KEY_LENGTH = 128
    private val PRNG_SEED = 16

    def prettyDate: String = {
        val date_format = "dd/MM/YY HH:mm:ss"
        new SimpleDateFormat(date_format).format(new Date)
    }

    def getRandomUUID(): String = UUID.randomUUID().toString

    def genErrorUniqueID(): String = {
        java.net.InetAddress.getLocalHost.getHostName.toUpperCase +
        "x" + UUID.randomUUID().toString.split("-").last.toUpperCase
    }

    def generateRandomRssLogFileName(): String =
        "rss-log-" + System.currentTimeMillis() + "-" + Utils.generateRandomToken()

    def generateRandomToken(): String = {
        var secureRandom: SecureRandom = null
        try {
            secureRandom = SecureRandom.getInstance("SHA1PRNG")
            secureRandom.setSeed(secureRandom.generateSeed(PRNG_SEED))
            val digest = MessageDigest.getInstance("SHA-1")
            val dig = digest.digest(s"$secureRandom.nextLong()".getBytes)
            binaryToHex(dig)
        } catch {
            case e: NoSuchAlgorithmException =>
                LOG.E("generateRandomToken", e)
                null
        }
    }

    def MD5(text: String) : String = {
        java.security.MessageDigest.getInstance("MD5").digest(text.getBytes()).map(0xFF & _)
          .map { "%02x".format(_) }.foldLeft(""){_ + _}
    }

    def hashStringBase64(input: String) = new String(Base64.encodeBase64(hashString(input)))
    def hashStringHex(input: String): String = binaryToHex(hashString(input))

    def hashString(input: String): Array[Byte] = {
        try {
            val md = MessageDigest.getInstance("SHA-256")
            val random = new SecureRandom()
            val salt = Array.ofDim[Byte](16)
            random.nextBytes(salt)
            md.update((input + salt).getBytes("UTF-8"))
            val byteData = md.digest()
            return byteData
        } catch {
            case e: Exception => LOG.E("hashString", e)
        }
        null
    }

    def deriveKeyPbkdf2(salt: Array[Byte], password: String): SecretKey = {
        val iterationCount = SECURE_ITERATIONS
        val keyLength = SECURE_KEY_LENGTH
        val keySpec = new PBEKeySpec(password.toCharArray, salt, iterationCount, keyLength)
        try {
            val keyFactory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1")
            val keyBytes = keyFactory.generateSecret(keySpec).getEncoded
            val key = new SecretKeySpec(keyBytes, "AES")
            return key
        } catch {
            case e: Exception => LOG.E("deriveKeyPbkdf2", e)
        }
        null
    }

    def binaryToHex(ba: Array[Byte]): String = {
        if (ba == null || ba.length == 0) { return null }
        val sb = new StringBuilder(ba.length * 2)
        var hexNumber: String = null
        for (x <- ba.indices) {
            hexNumber = "0" + java.lang.Integer.toHexString(0xff & ba(x))
            sb.append(hexNumber.substring(hexNumber.length - 1))
        }
        sb.toString
    }

    def hexToBinary(hex: String): Array[Byte] = {
        if (hex == null || hex.isEmpty) { return null }
        val ba = Array.ofDim[Byte](hex.length / 2)
        for (i <- ba.indices) {
            ba(i) = java.lang.Integer.parseInt(hex.substring(2 * i, 2 * i + 2), 16).toByte
        }
        ba
    }
    def encodeBase64String(s: String): String = try {
        val binary = s.getBytes("UTF-8")
        new String(Base64.encodeBase64(binary))
    } catch {
        case e: UnsupportedEncodingException =>
            LOG.E("encodeBase64String", e)
            null
    }

    def decodeBase64String(sb64: String): String = try {
        val binary = Base64.decodeBase64(sb64.getBytes)
        new String(binary, "UTF-8")
    } catch {
        case e: UnsupportedEncodingException =>
            LOG.E("decodeBase64String", e)
            null
    }

    def encodeFileToBase64Binary(fu: FileUtils, fileName: String): String = {
        val file = new File(fileName)
        val bytes = fu.LoadFile(file)
        val encoded = Base64.encodeBase64(bytes)
        val encodedString = new String(encoded)
        encodedString
    }

    def secureEncrypt(password: String, plaintext: String): String = {
        try {
            val keyLength = SECURE_KEY_LENGTH
            // same size as key output in bytes ( keyLength is in bits )
            val saltLength = keyLength >> 3
            val random = new SecureRandom()
            val salt = new Array[Byte](saltLength)
            random.nextBytes(salt)
            val key = deriveKeyPbkdf2(salt, password)
            val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
            val iv = new Array[Byte](cipher.getBlockSize)
            random.nextBytes(iv)
            val ivParams = new IvParameterSpec(iv)
            cipher.init(Cipher.ENCRYPT_MODE, key, ivParams)
            val ciphertext = cipher.doFinal(plaintext.getBytes("UTF-8"))
            LOG.D5("bytes: " + new String(salt) + "." + iv + "." + new String(ciphertext));
            val finalResult = new String(Base64.encodeBase64(salt)) + "." +
              new String(Base64.encodeBase64(iv)) + "." + new String(Base64.encodeBase64(ciphertext))
            LOG.D5("final: " + finalResult);
            return finalResult
        } catch {
            case e: Exception => LOG.E("secureEncrypt", e)
        }
        null
    }

    /**
      * Securely decrypts the plaintext using the password provided.
      *
      * The salt, IV and ciphertext are stored in the ciphertext separated by dot.
      *
      * base64(salt).base64(iv).base64(ciphertext)
      *
      * @param password   The password used to encrypt the text above
      * @param ciphertext The output of the secureEncrypt function above
      * @return
      */
    def secureDecrypt(password: String, ciphertext: String): String = {
        try {
            val fields = ciphertext.split("[.]")
            val salt = Base64.decodeBase64(fields(0).getBytes)
            val iv = Base64.decodeBase64(fields(1).getBytes)
            val cipherBytes = Base64.decodeBase64(fields(2).getBytes)
            //LPLogger.info( salt + "." + iv + "." + cipherBytes);
            val key = deriveKeyPbkdf2(salt, password)
            val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
            val ivParams = new IvParameterSpec(iv)
            cipher.init(Cipher.DECRYPT_MODE, key, ivParams)
            val plaintext = cipher.doFinal(cipherBytes)
            val plainrStr = new String(plaintext, "UTF-8")
            return plainrStr
        } catch {
            case e: Exception => LOG.E("secureDecrypt", e)
        }
        null
    }

    def gzip(input: String): ByteArrayOutputStream = {
        val inputStream = new ByteArrayInputStream(input.getBytes)
        val stringOutputStream = new ByteArrayOutputStream((input.length * 0.75).toInt)
        val gzipOutputStream = new GZIPOutputStream(stringOutputStream)
        val buf = Array.ofDim[Byte](5000)
        var len = 0
        len = inputStream.read(buf)
        while (len > 0) {
            gzipOutputStream.write(buf, 0, len)
            len = inputStream.read(buf)
        }
        inputStream.close()
        gzipOutputStream.close()
        stringOutputStream
    }

    def appendGoogleIdIfNeeded(id: String): String = {
        if (id.contains("_local"))  id
        else if (id.contains("_google")) id
        else id + "_google"
    }


    /**
     *   Returns a json in a string format, and strips out unnecessary fields for logging, like
     *   access_token (which is huge), username, and password.
     *
     *   Used for cleaner logging. As the code gets updated this may be eventually removed..
     *
     * @param jsVal
     * @return
     */
    def stripJson(jsVal: JsValue): String =
        (jsVal.as[JsObject] - SCHEMA.fAccessToken - "password" - "username").toString()

}




