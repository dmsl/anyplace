import com.dmurph.tracking.JGoogleAnalyticsTracker.GoogleAnalyticsVersion
import com.dmurph.tracking.{AnalyticsConfigData, JGoogleAnalyticsTracker}
import datasources.{CouchbaseDatasource, MongodbDatasource}
import javax.inject.{Inject, Singleton}
import play.api.Configuration
import play.api.inject.ApplicationLifecycle
import utils.LPLogger

import scala.concurrent.Future

@Singleton
class Anyplace @Inject() (conf: Configuration, mdb: MongodbDatasource) (appLifecycle: ApplicationLifecycle) {
  // ON APPLICATION START (ctor):
  //ifxDB.getStaticInstance(conf)
  CouchbaseDatasource.getStaticInstance(conf)
  MongodbDatasource.initialize(conf)
  logAnalyticsInstallation()

  appLifecycle.addStopHook({ () =>
    Future.successful {
      LPLogger.D2("onStart")
    }
  })

  def logAnalyticsInstallation(): Unit = {
    // Log the entry point from server installation
    JGoogleAnalyticsTracker.setProxy(System.getenv("http_proxy"))
    val config = new AnalyticsConfigData("UA-61313158-2")
    val tracker = new JGoogleAnalyticsTracker(config, GoogleAnalyticsVersion.V_4_7_2)
    tracker.trackEvent("Anyplace Installation", "Anyplace Server start", "Anyplace logging")
  }
  
}