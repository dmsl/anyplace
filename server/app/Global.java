import datasources.CouchbaseDatasource;
import datasources.DatasourceException;
import play.Application;
import play.GlobalSettings;
import play.Logger;


public class Global extends GlobalSettings {

    public void onStart(Application app) {
        Logger.info("Global::onStart():: AnyPlace Application started");
            // this is to initialize the couchbase connection
            CouchbaseDatasource.getStaticInstance();
    }

    public void onStop(Application app){
        Logger.info("Global::onStop():: AnyPlace Application stopped");
        try {
            CouchbaseDatasource.getStaticInstance().disconnect();
        } catch (DatasourceException e) {
            Logger.error("Global::onStop():: Exception while disconnecting from the couchbase server: " + e.getMessage());
        }
    }

}
