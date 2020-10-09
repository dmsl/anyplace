//package utils.core
//
//import java.text.SimpleDateFormat
//import java.util.Date
//
//import com.dmurph.tracking.JGoogleAnalyticsTracker.GoogleAnalyticsVersion
//import com.dmurph.tracking.{AnalyticsConfigData, JGoogleAnalyticsTracker}
//import datasources.{CouchbaseDatasource, DatasourceException, InfluxdbDatasource}
//
//import scala.concurrent.Future
//import javax.inject._
//import play.api.inject.ApplicationLifecycle
//import utils.LPLogger
//
//// This creates an `ApplicationStart` object once at start-up and registers hook for shut-down.
//@Singleton
//class ApplicationStart @Inject() (lifecycle: ApplicationLifecycle) {
//    OnAnyplaceStart()
//
//    // Shut-down hook
//    lifecycle.addStopHook { () =>
//        OnAnyplaceStop();
//        Future.successful(())
//    }
//
//    /**
//      * When the Play application starts
//      */
//    def OnAnyplaceStart(): Unit = {
//        LPLogger.info(_date + "Anyplace Application started")
//        InfluxdbDatasource.getStaticInstance
//        CouchbaseDatasource.getStaticInstance
//        logAnalyticsInstallation()
//    }
//
//    /**
//      * When the Play application stops
//      */
//   def OnAnyplaceStop(): Unit = {
//       // CHECK use Logger
//           LPLogger.info(_date + "Anyplace Application stopped")
//           try {
//             InfluxdbDatasource.getStaticInstance.disconnect()
//             CouchbaseDatasource.getStaticInstance.disconnect()
//           } catch {
//             case e: DatasourceException => LPLogger.error("OnAnyplaceStop:: Exception while disconnecting from the couchbase server: " +
//               e.getMessage)
//           }
//   }
//
//    def _date () = {
//        val date_format = "dd/MM/YY HH:mm:ss";
//        new SimpleDateFormat(date_format).format(new Date)
//    }
//    /**
//      * Log the entry point from server installation
//      */
//    def logAnalyticsInstallation(): Unit = {
//        JGoogleAnalyticsTracker.setProxy(System.getenv("http_proxy"))
//        val config = new AnalyticsConfigData("UA-61313158-2")
//        val tracker = new JGoogleAnalyticsTracker(config, GoogleAnalyticsVersion.V_4_7_2)
//        tracker.trackEvent("Anyplace Installation", "Anyplace Server start", "Anyplace logging")
//    }
//}
