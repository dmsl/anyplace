

        // CHECK might use normal logger
//        LPLogger.info(_date + " | Global::onStart():: AnyPlace Application started: ")
//        InfluxdbDatasource.getStaticInstance
//        CouchbaseDatasource.getStaticInstance
//        logAnalyticsInstallation()

    // XXX DO THIS!
    //play.modules.enabled += "com.example.StartModule"

//    import com.google.inject.AbstractModule

    class StartModule extends AbstractModule {
        override def configure() = {
            bind(classOf[ApplicationStart]).asEagerSingleton()
        }
    }

