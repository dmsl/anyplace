package controllers

import java.security.MessageDigest
import datasources.MongodbDatasource.updateCachedModerators
import datasources.{DatasourceException, MongodbDatasource, ProxyDataSource, SCHEMA}

import scala.concurrent.duration.Duration
import javax.inject.{Inject, Singleton}
import utils.json.VALIDATE
import models.{Account, ExternalType}
import models.ExternalType.ExternalType
import models.oauth.OAuth2Request
import org.mongodb.scala.MongoDatabase
import org.mongodb.scala.model.Filters.equal
import play.api.Configuration
import play.api.libs.json._
import play.api.mvc._
import utils.JsonUtils.isNullOrEmpty
import utils.{JsonUtils, LOG, Network, RESPONSE, Utils}

import scala.concurrent.Await

@Singleton
class UserController @Inject()(cc: ControllerComponents,
                                pds: ProxyDataSource,
                               mongoDB: MongodbDatasource,
                               conf: Configuration,
                               user: helper.User)
  extends AbstractController(cc) {

  def login(): Action[AnyContent] = Action {
    implicit request =>

      def inner(request: Request[AnyContent]): Result = {
        val anyReq: OAuth2Request = new OAuth2Request(request)
        if (!anyReq.assertJsonBody()) return RESPONSE.BAD(RESPONSE.ERROR_JSON_PARSE)
        val json = anyReq.getJsonBody()
        val checkRequirements = VALIDATE.checkRequirements(json, SCHEMA.fUsername, SCHEMA.fPassword)
        if (checkRequirements != null) return checkRequirements
        LOG.D4("login: " + json)
        val username = (json \ SCHEMA.fUsername).as[String]
        val password = (json \ SCHEMA.fPassword).as[String]
        val storedUser = pds.db.login(SCHEMA.cUsers, username, encryptPwd(password))
        if (storedUser == null) return RESPONSE.BAD("Incorrect username or password.")
        if (storedUser.size > 1) return RESPONSE.BAD("More than one users were found.")
        val accessToken = (storedUser(0) \ SCHEMA.fAccessToken).as[String]
        if (accessToken == null) return RESPONSE.BAD("User doesn't have access token.")

        val user = storedUser(0).as[JsObject] - SCHEMA.fPassword
        val res = Json.obj("user" -> user)

        updateCachedModerators()
        return RESPONSE.OK(res, "Successfully found user.")
      }

    inner(request)
  }

  def refresh(): Action[AnyContent] = Action {
    implicit request =>

      def inner(request: Request[AnyContent]): Result = {
        val anyReq: OAuth2Request = new OAuth2Request(request)
        if (!anyReq.assertJsonBody()) return RESPONSE.BAD(RESPONSE.ERROR_JSON_PARSE)
        val json = anyReq.getJsonBody()
        val checkRequirements = VALIDATE.checkRequirements(json, SCHEMA.fAccessToken)
        if (checkRequirements != null) return checkRequirements
        LOG.D4("refresh: " + json)
        val accessToken = (json \ SCHEMA.fAccessToken).as[String]

        val storedUser = pds.db.getUserFromAccessToken(accessToken)
        if (storedUser == null) return RESPONSE.BAD("User not found.")
        if (storedUser.size > 1) return RESPONSE.BAD("More than one users were found.")

        val user = storedUser(0).as[JsObject] - SCHEMA.fPassword
        val res = Json.obj("user" -> user)

        updateCachedModerators()
        RESPONSE.OK(res, "Successfully found user.")
      }

      inner(request)
  }

  def register(): Action[AnyContent] = Action {
    implicit request =>
      def inner(request: Request[AnyContent]): Result = {
        val anyReq: OAuth2Request = new OAuth2Request(request)
        if (!anyReq.assertJsonBody()) return RESPONSE.BAD(RESPONSE.ERROR_JSON_PARSE)
        val json = anyReq.getJsonBody()
        LOG.D4("register: " + json)
        val checkRequirements = VALIDATE.checkRequirements(json, SCHEMA.fUsername, SCHEMA.fPassword, SCHEMA.fName, SCHEMA.fEmail)
        if (checkRequirements != null) return checkRequirements
        val name = (json \ SCHEMA.fName).as[String]
        val email = (json \ SCHEMA.fEmail).as[String]
        val username = (json \ SCHEMA.fUsername).as[String]
        val password = (json \ SCHEMA.fPassword).as[String]
        val external = "anyplace"
        var accType = "user"

        // if first user then assign as admin
        if (pds.db.isAdmin(SCHEMA.cUsers))
          accType = "admin"
        // Check if the email is unique
        val storedEmail = pds.db.getFromKeyAsJson(SCHEMA.cUsers, SCHEMA.fEmail, email)
        if (storedEmail != null) return RESPONSE.BAD("There is already an account with this email.")
        // Check if the username is unique
        val storedUsername = pds.db.getFromKeyAsJson(SCHEMA.cUsers, SCHEMA.fUsername, username)
        if (storedUsername != null) return RESPONSE.BAD("Username is already taken.")
        val newUser = pds.db.register(SCHEMA.cUsers, name, email, username, encryptPwd(password), external, accType)
        if (newUser == null) return RESPONSE.BAD("Please try again.")
        val res: JsValue = Json.obj("newUser" -> newUser)
        return RESPONSE.OK(res,"Successfully registered.")
      }
      inner(request)
  }

  /**
   *
   * @return type(admin, user, .. etc) + message
   */
  def loginGoogle(): Action[AnyContent] = Action {
    implicit request =>
      def inner(request: Request[AnyContent]): Result = {
        LOG.D2("loginGoogle")
        val auth = new OAuth2Request(request)
        if (!auth.assertJsonBody()) return RESPONSE.BAD(RESPONSE.ERROR_JSON_PARSE)
        var json = auth.getJsonBody()
        if (isNullOrEmpty(json)) return RESPONSE.BAD(RESPONSE.ERROR_API_USAGE)
        json = appendUserType(json)
        val external = json \ SCHEMA.fExternal

        if (external.toOption.isDefined && external.as[String] == "google") {
          updateCachedModerators()
          authorizeGoogleAccount(auth)
        } else {
          RESPONSE.BAD("Not a google account.")
        }
      }

      inner(request)
  }

  def updateUser(): Action[AnyContent] = Action {
    implicit request =>
      def inner(request: Request[AnyContent]): Result = {
        val anyReq = new OAuth2Request(request)
        val apiKey = anyReq.getAccessToken()
        if (apiKey == null) return anyReq.NO_ACCESS_TOKEN()
        LOG.D2("updateUser:")
        if (!anyReq.assertJsonBody()) return RESPONSE.BAD(RESPONSE.ERROR_JSON_PARSE)
        val json = anyReq.getJsonBody()
        val checkRequirements = VALIDATE.checkRequirements(json, SCHEMA.fUserId)
        if (checkRequirements != null) return checkRequirements

        val owner_id = user.authorize(apiKey)
        if (owner_id == null) return RESPONSE.UNAUTHORIZED_USER

        val userOwnerId = (json \ SCHEMA.fUserId).as[String]
        // only admin can update a user, or the user can update it self
        if (owner_id != userOwnerId && !MongodbDatasource.getAdmins.contains(owner_id) && !MongodbDatasource.getModerators.contains(owner_id))
          return RESPONSE.UNAUTHORIZED("Users can only update themselves, unless they are moderators.")

        val storedUser = pds.db.getUserFromOwnerId(userOwnerId)
        if (storedUser == null) return RESPONSE.BAD("User not found.")
        var newUser: JsValue = storedUser(0)

        if ((json \ SCHEMA.fName).toOption.isDefined) {
          newUser = newUser.as[JsObject] + (SCHEMA.fName -> JsString((json \ SCHEMA.fName).as[String]))
        }
        if ((json \ SCHEMA.fEmail).toOption.isDefined) {
          val email = (json \ SCHEMA.fEmail).as[String]
          val storedEmail = pds.db.getFromKeyAsJson(SCHEMA.cUsers, SCHEMA.fEmail, email)
          if (storedEmail != null) return RESPONSE.BAD("There is already an account with this email.")
          newUser = newUser.as[JsObject] + (SCHEMA.fEmail -> JsString(email))
        }
        if ((json \ SCHEMA.fUsername).toOption.isDefined) {
          val username = (json \ SCHEMA.fUsername).as[String]
          val storedUsername = pds.db.getFromKeyAsJson(SCHEMA.cUsers, SCHEMA.fUsername, username)
          if (storedUsername != null) return RESPONSE.BAD("Username is already taken.")
          newUser = newUser.as[JsObject] + (SCHEMA.fUsername -> JsString(username))
        }
        if ((json \ SCHEMA.fPassword).toOption.isDefined) {
          newUser = newUser.as[JsObject] + (SCHEMA.fPassword -> JsString(encryptPwd((json \ SCHEMA.fPassword).as[String])))
        }
        // Only admins can change type of user
        if ((json \ SCHEMA.fType).toOption.isDefined) {
          if (MongodbDatasource.getAdmins.contains(userOwnerId))
            return RESPONSE.FORBIDDEN("Cannot change type of an admin.")
          if (MongodbDatasource.getAdmins.contains(owner_id)) {
            val _type = (json \ SCHEMA.fType).as[String]
            if (_type.equals("moderator") || _type.equals("user")) {
              newUser = newUser.as[JsObject] + (SCHEMA.fType -> JsString(_type))
            } else {
              return RESPONSE.FORBIDDEN("Cannot change to type '" + _type + "'")
            }
          } else {
            return RESPONSE.FORBIDDEN("Unauthorized. Only admins can change user type.")
          }
        }
        if (pds.db.replaceJsonDocument(SCHEMA.cUsers, SCHEMA.fOwnerId, userOwnerId, newUser.toString()))
          return RESPONSE.OK("Successfully updated user.")
        return RESPONSE.ERROR_INTERNAL("Could not update user.")
      }

      inner(request)
  }

  def appendUserType(json: JsValue): JsValue = {
    if ((json \ SCHEMA.fType).toOption.isDefined) {
      LOG.I("appendUserType: type exists: " + (json \ SCHEMA.fType).as[String]) // Might crash
      return json
    } else {
      var userType: String = ""
      if (isFirstUser) {
        userType = "admin"
        LOG.I("Initializing admin user.")
      } else {
        LOG.D4("AppendUserType: user")
        userType = "user"
      }
      return json.as[JsObject] + (SCHEMA.fType -> JsString(userType))
    }
  }

  def getAccountType(json: JsValue): ExternalType = {
    val external = json \ SCHEMA.fExternal
    if (external.toOption.isDefined) {
      val exts = external.as[String]
      if (exts == "google") return ExternalType.GOOGLE
    }
    ExternalType.LOCAL
  }

  def isFirstUser: Boolean = {
    val mdb: MongoDatabase = mongoDB.getMDB
    val collection = mdb.getCollection(SCHEMA.cUsers)
    val users = collection.find()
    val awaited = Await.result(users.toFuture(), Duration.Inf)
    val res = awaited.toList

    res.isEmpty
  }

  def getGoogleUser(json: JsValue): JsValue = {
    var user: JsValue = null
    getAccountType(json) match {
      case ExternalType.GOOGLE =>
      val mdb: MongoDatabase = mongoDB.getMDB
        val collection = mdb.getCollection(SCHEMA.cUsers)
        val ownerId = (json \ SCHEMA.fOwnerId).as[String]
        val userLookUp = collection.find(equal(SCHEMA.fOwnerId, ownerId))
        val awaited = Await.result(userLookUp.toFuture(), Duration.Inf)
        val res = awaited.toList
        if (res.size == 1) {
          user = mongoDB.convertJson(res.head)
        } else if (res.size > 1) {
          LOG.E("User exists. More than one user with id: " + ownerId)
        }
      case _ => LOG.E("Not a Google User.")
    }

    user
  }

  /**
   * Checks that a Google user was in the database, otherwise it
   * creates a new anyplace account for that user.
   *
   * NOTE: An anyplace specific Access Token is generated for a Google User as well
   *
   * @param auth
   * @return
   */
  def authorizeGoogleAccount(auth: OAuth2Request): Result = {
    LOG.D2("User: authorizeGoogleAccount")
    var json = auth.getJsonBody()
    val hasExternal = JsonUtils.hasProperties(json, SCHEMA.fExternal) // TODO
    if (!hasExternal.isEmpty) return RESPONSE.MISSING_FIELDS(hasExternal)

    var id = verifyGoogleUser((json \ SCHEMA.fAccessToken).as[String])
    if (id == null) return RESPONSE.UNAUTHORIZED_USER
    id = Utils.appendGoogleIdIfNeeded(id)
    json = json.as[JsObject] + (SCHEMA.fOwnerId -> Json.toJson(id))

    // if user exists but has no anyplace access_token: create one and put on db
    var user = getGoogleUser(json)
    val hasAccessToken = !(user \ SCHEMA.fAccessToken).toOption.isEmpty
    if (hasAccessToken && user != null) { // add access_token to db if !exists
      val newAccessToken = MongodbDatasource.generateAccessToken(false)
      user = user.as[JsObject] + (SCHEMA.fAccessToken -> JsString(newAccessToken)) +
        (SCHEMA.fSchema -> JsNumber(MongodbDatasource.__SCHEMA))
      pds.db.replaceJsonDocument(SCHEMA.cUsers, SCHEMA.fOwnerId,
        (json \ SCHEMA.fOwnerId).as[String], user.toString())
    }
    var userType = "user"
    if (pds.db.isAdmin(SCHEMA.cUsers))
      userType = "admin"
    json = json.as[JsObject] + (SCHEMA.fOwnerId -> Json.toJson(id)) +
      (SCHEMA.fType -> JsString(userType))
    if (user != null) { // user and access_token exists
      return RESPONSE.OK(user, "User Exists.")
    } else {  // new user created
      val user = new Account(json)
      pds.db.addJson(SCHEMA.cUsers, user.toString())
      return RESPONSE.OK(user.toJson(), "Added new google user.")
    }
  }

  /**
   * Calls Google API to verify a Google users access token, which was sent by the client.
   *
   * @param authToken Google Authentication Token (OAuth)
   * @return
   */
  def verifyGoogleUser(authToken: String): String = {
    // remove the double string quotes due to json processing
    val gURL = "https://www.googleapis.com/oauth2/v3/tokeninfo?id_token=" + authToken
    var res = ""
    try {
      res = Network.GET(gURL)
    } catch {
      case e: Exception => LOG.E("verifyId", e)
    }
    if (res != null) {
      try {
        val json = Json.parse(res)
        val uid = json \ "user_id"
        val sub = json \ "sub"
        if (uid.toOption.isDefined)
          return uid.as[String]
        if (sub.toOption.isDefined)
          return sub.as[String]
      } catch {
        case iae: IllegalArgumentException => LOG.E("verifyId: " + iae.getMessage + "String: '" + res + "'");
        case e: Exception => LOG.E("verifyId", e)
      }
    }
    null
  }

  def encryptPwd(password: String): String = {
    val salt = conf.get[String]("password.salt")
    val pepper = conf.get[String]("password.pepper")

    val str = salt + password + pepper
    LOG.D("pwd: '" + str + "'")
    val encryptedPwd = encryptPassword(str)
    LOG.D("encrypted: '" + encryptedPwd + "'")
    encryptedPwd
  }

  def encryptPassword(password: String): String = {
    val algorithm: MessageDigest = MessageDigest.getInstance("SHA-256")
    val defaultBytes: Array[Byte] = password.getBytes
    algorithm.reset
    algorithm.update(defaultBytes)
    val messageDigest: Array[Byte] = algorithm.digest
    getHexString(messageDigest)
  }

  def getHexString(messageDigest: Array[Byte]): String = {
    val hexString: StringBuffer = new StringBuffer
    messageDigest foreach { digest =>
      val hex = Integer.toHexString(0xFF & digest)
      if (hex.length == 1) hexString.append('0') else hexString.append(hex)
    }
    hexString.toString
  }

}


