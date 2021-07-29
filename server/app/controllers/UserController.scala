package controllers

import datasources.{MongodbDatasource, ProxyDataSource, SCHEMA}

import scala.concurrent.duration.Duration
import javax.inject.{Inject, Singleton}
import json.VALIDATE
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
                               conf: Configuration)
  extends AbstractController(cc) {

  def login() = Action {
    implicit request =>

      def inner(request: Request[AnyContent]): Result = {
        val anyReq: OAuth2Request = new OAuth2Request(request)
        if (!anyReq.assertJsonBody()) return RESPONSE.BAD(RESPONSE.ERROR_JSON_PARSE)
        val json = anyReq.getJsonBody()
        val checkRequirements = VALIDATE.checkRequirements(json, SCHEMA.fUsername, SCHEMA.fPassword)
        if (checkRequirements != null) return checkRequirements
        LOG.D2("login: " + json)
        val username = (json \ SCHEMA.fUsername).as[String]
        val password = (json \ SCHEMA.fPassword).as[String]
        val storedUser = pds.getIDatasource.login(SCHEMA.cUsers, username, password)
        if (storedUser == null) return RESPONSE.BAD("Incorrect username or password.")
        if (storedUser.size > 1) return RESPONSE.BAD("More than one users were found.")
        val accessToken = (storedUser(0) \ SCHEMA.fAccessToken).as[String]
        if (accessToken == null) return RESPONSE.BAD("User doesn't have access token.")

        val user = storedUser(0).as[JsObject] - SCHEMA.fPassword
        val res = Json.obj("user" -> user)
        return RESPONSE.OK(res, "Successfully found user.")
      }

    inner(request)
  }

  def refresh() = Action {
    implicit request =>

      def inner(request: Request[AnyContent]): Result = {
        val anyReq: OAuth2Request = new OAuth2Request(request)
        if (!anyReq.assertJsonBody()) return RESPONSE.BAD(RESPONSE.ERROR_JSON_PARSE)
        val json = anyReq.getJsonBody()
        val checkRequirements = VALIDATE.checkRequirements(json, SCHEMA.fAccessToken)
        if (checkRequirements != null) return checkRequirements
        LOG.D2("refresh: " + json)
        val accessToken = (json \ SCHEMA.fAccessToken).as[String]

        val storedUser = pds.getIDatasource.getUserAccount(SCHEMA.cUsers, accessToken)
        if (storedUser == null) return RESPONSE.BAD("User not found.")
        if (storedUser.size > 1) return RESPONSE.BAD("More than one users were found.")

        val user = storedUser(0).as[JsObject] - SCHEMA.fPassword
        val res = Json.obj("user" -> user)

        RESPONSE.OK(res, "Successfully found user.")
      }

      inner(request)
  }


  def register() = Action {
    implicit request =>
      def inner(request: Request[AnyContent]): Result = {
        val anyReq: OAuth2Request = new OAuth2Request(request)
        if (!anyReq.assertJsonBody()) return RESPONSE.BAD(RESPONSE.ERROR_JSON_PARSE)
        val json = anyReq.getJsonBody()
        LOG.D2("register: " + json)
        val checkRequirements = VALIDATE.checkRequirements(json, SCHEMA.fUsername, SCHEMA.fPassword, SCHEMA.fName, SCHEMA.fEmail)
        if (checkRequirements != null) return checkRequirements
        val name = (json \ SCHEMA.fName).as[String]
        val email = (json \ SCHEMA.fEmail).as[String]
        val username = (json \ SCHEMA.fUsername).as[String]
        val password = (json \ SCHEMA.fPassword).as[String]
        val external = "anyplace"
        var accType = "user"

        // if first user then assign as admin
        if (pds.getIDatasource.isAdmin(SCHEMA.cUsers))
          accType = "admin"
        // Check if the email is unique
        val storedEmail = pds.getIDatasource.getFromKeyAsJson(SCHEMA.cUsers, SCHEMA.fEmail, email)
        if (storedEmail != null) return RESPONSE.BAD("There is already an account with this email.")
        // Check if the username is unique
        val storedUsername = pds.getIDatasource.getFromKeyAsJson(SCHEMA.cUsers, SCHEMA.fUsername, username)
        if (storedUsername != null) return RESPONSE.BAD("Username is already taken.")

        val newUser = pds.getIDatasource.register(SCHEMA.cUsers, name, email, username, password, external, accType)
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
  def loginGoogle() = Action {
    implicit request =>
      def inner(request: Request[AnyContent]): Result = {
        LOG.D1("loginGoogle")
        val auth = new OAuth2Request(request)
        if (!auth.assertJsonBody()) return RESPONSE.BAD(RESPONSE.ERROR_JSON_PARSE)
        var json = auth.getJsonBody()
        if (isNullOrEmpty(json)) return RESPONSE.BAD(RESPONSE.ERROR_API_USAGE)
        json = appendUserType(json) //# TODO auth.appendUserType() // update json directly.. inside auth object..
        // auth.isGoogleUser() // and hide the below functionality....
        val external = json \ SCHEMA.fExternal

        if (external.toOption.isDefined && external.as[String] == "google") {
          authorizeGoogleAccount(auth)
        } else {
          RESPONSE.BAD("Not a google account.")
        }
      }

      inner(request)
  }

  // TODO if json has not type add type = user
  def appendUserType(json: JsValue): JsValue = {
    if ((json \ SCHEMA.fType).toOption.isDefined) {
      LOG.I("user type exists: " + (json \ SCHEMA.fType).as[String]) // Might crash
      return json
    } else {
      var userType: String = ""
      if (isFirstUser()) {
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

  def isFirstUser(): Boolean = {
    val mdb: MongoDatabase = mongoDB.getMDB
    val collection = mdb.getCollection(SCHEMA.cUsers)
    val users = collection.find()
    val awaited = Await.result(users.toFuture(), Duration.Inf)
    val res = awaited.toList

    res.isEmpty
  }

  def getUser(json: JsValue): JsValue = {
    // val mdb: MongoDatabase = mongoDB.getMDB  CHECK:NN
    // val collection = mdb.getCollection(SCHEMA.cUsers)
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
          user = mongoDB.convertJson(res.head) // CHECK:NN was (0)
        } else if (res.size > 1) {
          LOG.E("User exists. More than one user with id: " + ownerId)
        }
      case ExternalType.LOCAL => LOG.D("TODO: query unique email") // CHECK:NN ??
    }

    user
  }

  // TODO:NN explain this.... REVIEW with PM
  def authorizeGoogleAccount(auth: OAuth2Request): Result = {
    LOG.I("addGoogleAccount")
    var json = auth.getJsonBody()
    val hasExternal = JsonUtils.hasProperties(json, SCHEMA.fExternal) // TODO
    if (!hasExternal.isEmpty) return RESPONSE.MISSING_FIELDS(hasExternal)

    var id = verifyGoogleAuthentication((json \ SCHEMA.fAccessToken).as[String])
    if (id == null) return RESPONSE.UNAUTHORIZED_USER()
    id = Utils.appendGoogleIdIfNeeded(id)
    json = json.as[JsObject] + (SCHEMA.fOwnerId -> Json.toJson(id))

    // if user exists but has no anyplace access_token: create one and put on db
    var user = getUser(json)
    val hasAccessToken = !(user \ SCHEMA.fAccessToken).toOption.isEmpty
    if (hasAccessToken && user != null) { // add access_token to db if !exists
      val newAccessToken = MongodbDatasource.generateAccessToken(false)
      user = user.as[JsObject] + (SCHEMA.fAccessToken -> JsString(newAccessToken)) +
        (SCHEMA.fSchema -> JsNumber(MongodbDatasource.__SCHEMA))
      pds.getIDatasource.replaceJsonDocument(SCHEMA.cUsers, SCHEMA.fOwnerId,
        (json \ SCHEMA.fOwnerId).as[String], user.toString())
    }
    json = json.as[JsObject] + (SCHEMA.fOwnerId -> Json.toJson(id))
    if (user != null) { // user and access_token exists
      user = user.as[JsObject] + (SCHEMA.fType -> JsString((user \ SCHEMA.fType).as[String]))
      return RESPONSE.OK(user, "User Exists.") // its not AnyResponseHelperok
    } else {  // new user created CHECK:NN .. how token is on local account?
      val user = new Account(json)
      pds.getIDatasource.addJsonDocument(SCHEMA.cUsers, user.toString())
      return RESPONSE.OK(user.toJson(), "Added new google user.")
    }
  }


  /**
   * TODO:NN move from here. Maybe to UserController? (or some Util object?). Does it work with local accounts?
   * @param authToken
   * @return
   */
  def verifyGoogleAuthentication(authToken: String): String = {
    // remove the double string quotes due to json processing
    val gURL = "https://www.googleapis.com/oauth2/v3/tokeninfo?id_token=" + authToken
    var res = ""
    try {
      res = Network.GET(gURL)
    } catch {
      case e: Exception => { LOG.E("verifyId", e)}
    }
    if (res != null) {
      try {
        // CHECK:PM CHECK:NN bug on main branch (JsonObject.fromJson())
        val json = Json.parse(res)
        val uid = (json \ "user_id")
        val sub = (json \ "sub")
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


}


// CHECK:NN
/**
 * Deletes the account with the AUID passed in.
 * The result of the action is returned in the Json response.
 *
 * @return
 */
//def deleteAccount(auid_in: String) = Action {
//  implicit request =>
//
//    def inner(request: Request[AnyContent]): Result = {
//      var auid: String = auid_in
//      // create the Request and check it
//      val anyReq: OAuth2Request = new OAuth2Request(request)
//      if (!anyReq.assertJsonBody()) {
//        return RESPONSE.BAD(
//          RESPONSE.ERROR_JSON_PARSE)
//      }
//      val json = anyReq.getJsonBody()
//      LOG.I("UserController:deleteAccount: " + json.toString)
//      // check if there is any required parameter missing
//      val notFound: java.util.List[String] =
//        JsonUtils.hasProperties(json, "auid")
//      if (!notFound.isEmpty && (auid == null || auid.trim().isEmpty)) {
//        return RESPONSE.MISSING_FIELDS(notFound)
//      }
//      // if the auid in the route is empty then try to get the one from the POST json body
//      if (auid == null || auid.trim().isEmpty)
//        auid = json.\\("auid").mkString
//      try {
//        if (!pds.getIDatasource.deleteFromKey(auid)) {
//          return RESPONSE.BAD("Account could not be deleted!")
//        }
//        return RESPONSE.OK("Successfully deleted account!")
//      } catch {
//        case e: DatasourceException =>
//          return RESPONSE.internal_server_error(
//            "500: " + e.getMessage)
//
//      }
//    }
//
//    inner(request)
//}

///**
// * Updates the account specified by the AUID.
// * The result of the update is returned in the Json response.
// *
// * @return
// */
//def UpdateAccount(auid: String)(auid_in: String) = Action {
//  implicit request =>
//
//    def inner(request: Request[AnyContent]): Result = {
//      var auid: String = auid_in
//      // create the Request and check it
//      val anyReq: OAuth2Request = new OAuth2Request(request)
//      if (!anyReq.assertJsonBody()) {
//        return AnyResponseHelper.bad_request(
//          AnyResponseHelper.CANNOT_PARSE_BODY_AS_JSON)
//      }
//      val json = anyReq.getJsonBody()
//      LPLogger.info("AnyplaceAccounts::updateAccount():: " + json.toString)
//      // check if there is any required parameter missing
//      val notFound: java.util.List[String] =
//        JsonUtils.hasProperties(json, "auid")
//      if (!notFound.isEmpty && (auid == null || auid.trim().isEmpty)) {
//        return AnyResponseHelper.requiredFieldsMissing(notFound)
//      }
//      // if the auid in the route is empty then try to get the one from the POST json body
//      if (auid == null || auid.trim().isEmpty)
//        auid = json.\\("auid").mkString
//      try {
//        // fetch the stored object
//        var storedAccount: JsonObject = null
//        storedAccount = toCouchObject(CouchbaseDatasource.getStaticInstance(conf).getFromKeyAsJson(auid))
//        if (storedAccount == null) {
//          return AnyResponseHelper.bad_request(
//            "Account could not be updated! Try again...")
//        }
//        // apply any change made
//        val updateableFields: Array[String] =
//          AccountModel.getChangeableProperties()
//        for (s <- updateableFields) {
//          val value = json.\\(s)
//          if (value.asInstanceOf[Boolean]) {
//            storedAccount.put(s, value.asInstanceOf[Boolean])
//          } else {
//            val nv: String = value.mkString
//            if (nv == null || nv.trim().isEmpty) //continue
//              storedAccount.put(s, nv)
//          }
//        }
//        // save the changes
//        if (!pds.getIDatasource.replaceJsonDocument(auid, 0, storedAccount.toString)) {
//          return AnyResponseHelper.bad_request(
//            "Account could not be updated! Try again...")
//        }
//        return AnyResponseHelper.ok("Successfully updated account!")
//      } catch {
//        case e: DatasourceException =>
//          return AnyResponseHelper.internal_server_error(
//            "500: " + e.getMessage)
//
//      }
//    }
//
//    inner(request)
//}

// check if there is any required parameter missing
// check if there is any required parameter missing

///**
// * Returns the list of clients for this account
// *
// * @param auid The account for which the clients are to be returned
// * @return
// */
//def fetchAccountClients(auid: String)(auid_in: String) = Action {
//  implicit request =>
//    def inner(request: Request[AnyContent]): Result = {
//      var auid: String = auid_in
//      // create the Request and check it
//      val anyReq: OAuth2Request = new OAuth2Request(request)
//      if (!anyReq.assertJsonBody()) {
//        return AnyResponseHelper.bad_request(
//          AnyResponseHelper.CANNOT_PARSE_BODY_AS_JSON)
//      }
//      val json = anyReq.getJsonBody()
//      LPLogger.info("AnyplaceAccounts::fetchAccountClients():: " + json.toString)
//      // check if there is any required parameter missing
//      val notFound: java.util.List[String] =
//        JsonUtils.hasProperties(json, "auid")
//      if (!notFound.isEmpty && (auid == null || auid.trim().isEmpty)) {
//        return AnyResponseHelper.requiredFieldsMissing(notFound)
//      }
//      // if the auid in the route is empty then try to get the one from the POST json body
//      if (auid == null || auid.trim().isEmpty)
//        auid = json.\\("auid").mkString
//      try {
//        var storedAccount: JsonObject = null
//        storedAccount =
//          toCouchObject(CouchbaseDatasource.getStaticInstance(conf).getFromKeyAsJson(auid))
//        if (storedAccount == null) {
//          return AnyResponseHelper.bad_request("Account could not be found!")
//        }
//        val json_clients = storedAccount.getArray("clients")
//        val resp: JsonObject = JsonObject.empty()
//        resp.put("clients", json_clients)
//        return AnyResponseHelper.ok(resp, "Successfully fetched account clients!")
//      } catch {
//        case e: DatasourceException =>
//          return AnyResponseHelper.internal_server_error(
//            "500: " + e.getMessage)
//
//      }
//    }
//
//    inner(request)
//}

///**
// * Adds a new client for this account
// *
// * @param auid The account the new account belongs to
// * @return
// */
//def addAccountClient(auid: String)(auid_in: String) = Action {
//  implicit request =>
//
//    def inner(request: Request[AnyContent]): Result = {
//      var auid: String = auid_in
//      // create the Request and check it
//      val anyReq: OAuth2Request = new OAuth2Request(request)
//      if (!anyReq.assertJsonBody()) {
//        return AnyResponseHelper.bad_request(
//          AnyResponseHelper.CANNOT_PARSE_BODY_AS_JSON)
//      }
//      val json = anyReq.getJsonBody()
//      LPLogger.info("AnyplaceAccounts::addAccountClient():: " + json.toString)
//      // check if there is any required parameter missing
//      val notFound: java.util.List[String] =
//        JsonUtils.hasProperties(json, "auid", "grant_type")
//      if (!notFound.isEmpty && (auid == null || auid.trim().isEmpty)) {
//        return AnyResponseHelper.requiredFieldsMissing(notFound)
//      }
//      // if the auid in the route is empty then try to get the one from the POST json body
//      if (auid == null || auid.trim().isEmpty)
//        auid = json.\\("auid").mkString
//      val grant_type: String = json.\\("grant_type").mkString
//      val scope: String = json.\\("scope").mkString
//      val redirect_uri: String = json.\\("redirect_uri").mkString
//      if (!GrantHandlerFactory.isGrantTypeSupported(grant_type)) {
//        return AnyResponseHelper.bad_request("grant_type specified is not supported!")
//      }
//      try {
//        var storedAccount: JsonObject = null
//        storedAccount = toCouchObject(CouchbaseDatasource.getStaticInstance(conf).getFromKeyAsJson(auid))
//        if (storedAccount == null) {
//          return AnyResponseHelper.bad_request("Account could not be found!")
//        }
//        val account: AccountModel = new AccountModel(storedAccount)
//        account.addNewClient(grant_type, scope, redirect_uri)
//        // save the changes
//        if (!pds.getIDatasource.replaceJsonDocument(
//          auid,
//          0,
//          account.toJson().toString)) {
//          return AnyResponseHelper.bad_request(
//            "Account could not be updated! Try again...")
//        }
//        return AnyResponseHelper.ok("Successfully added account client!")
//      } catch {
//        case e: DatasourceException =>
//          return AnyResponseHelper.internal_server_error(
//            "500: " + e.getMessage)
//
//      }
//    }
//
//    inner(request)
//}

/**
 * Fetches the account  client with the AUID and client_id passed in.
 * The client document is returned in the Json response.
 *
 * @return
 */
//def fetchAccountClient(auid: String, client_id: String)(auid_in: String) = Action {
//  implicit request =>
//
//    def inner(request: Request[AnyContent]): Result = {
//      var auid: String = auid_in
//      // create the Request and check it
//      val anyReq: OAuth2Request = new OAuth2Request(request)
//      if (!anyReq.assertJsonBody()) {
//        return AnyResponseHelper.bad_request(
//          AnyResponseHelper.CANNOT_PARSE_BODY_AS_JSON)
//      }
//      val json = anyReq.getJsonBody()
//      LPLogger.info("AnyplaceAccounts::fetchAccount():: " + json.toString)
//      // check the arguments
//      if ((auid == null || auid.trim().isEmpty)) {
//        return AnyResponseHelper.bad_request("Invalid account id provided!")
//      }
//      if ((client_id == null || client_id.trim().isEmpty)) {
//        AnyResponseHelper.bad_request("Invalid client id provided!")
//      }
//      try {
//        var storedAccount: JsonObject = null
//        storedAccount = toCouchObject(CouchbaseDatasource.getStaticInstance(conf).getFromKeyAsJson(auid))
//        if (storedAccount == null) {
//          return AnyResponseHelper.bad_request("Account could not be found!")
//        }
//        val account: AccountModel = new AccountModel(storedAccount)
//        val client: AccountModel.ClientModel = account.getClient(client_id)
//        if (client == null) {
//          return AnyResponseHelper.bad_request("Account client could not be found!")
//        }
//        return AnyResponseHelper.ok(client.toJson(),
//          "Successfully fetched account client!")
//      } catch {
//        case e: DatasourceException =>
//          return AnyResponseHelper.internal_server_error(
//            "500: " + e.getMessage)
//
//      }
//    }
//
//    inner(request)
//}

/**
 * Fetches the account  client with the AUID and client_id passed in.
 * The client document is returned in the Json response.
 *
 * @return
 */
//def deleteAccountClient(auid: String, client_id: String)(auid_in: String) = Action {
//  implicit request =>
//
//    def inner(request: Request[AnyContent]): Result = {
//      var auid: String = auid_in
//      // create the Request and check it
//      val anyReq: OAuth2Request = new OAuth2Request(request)
//      if (!anyReq.assertJsonBody()) {
//        return AnyResponseHelper.bad_request(
//          AnyResponseHelper.CANNOT_PARSE_BODY_AS_JSON)
//      }
//      val json = anyReq.getJsonBody()
//      LPLogger.info("AnyplaceAccounts::deleteAccount():: " + json.toString)
//      // check the arguments
//      if ((auid == null || auid.trim().isEmpty)) {
//        return AnyResponseHelper.bad_request("Invalid account id provided!")
//      }
//      if ((client_id == null || client_id.trim().isEmpty)) {
//        return AnyResponseHelper.bad_request("Invalid client id provided!")
//      }
//      try {
//        var storedAccount: JsonObject = null
//        storedAccount =
//          toCouchObject(CouchbaseDatasource.getStaticInstance(conf).getFromKeyAsJson(auid))
//        if (storedAccount == null) {
//          return AnyResponseHelper.bad_request("Account could not be found!")
//        }
//        val account: AccountModel = new AccountModel(storedAccount)
//        if (!account.deleteClient(client_id)) {
//          return AnyResponseHelper.bad_request("Account client could not be found!")
//        }
//        // save the changes
//        if (!pds.getIDatasource.replaceJsonDocument(
//          auid,
//          0,
//          account.toJson().toString)) {
//          return AnyResponseHelper.bad_request(
//            "Account could not be updated! Try again...")
//        }
//        return AnyResponseHelper.ok("Successfully deleted account client!")
//      } catch {
//        case e: DatasourceException =>
//          return AnyResponseHelper.internal_server_error(
//            "500: " + e.getMessage)
//
//      }
//    }
//
//    inner(request)
//}

