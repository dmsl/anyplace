package cy.ac.ucy.cs.anyplace.core;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;

public class Preferences {


    private String api_key = null;
    private String host = null;
    private String port = null;
    private String cache = null;
    public String status = null;

    public static String getAnyplaceDir(){
       return getHome()+"/.anyplace/client/";
    }


    public Preferences(){
            this(openFile(getAnyplaceDir() + "config"), openFile(getAnyplaceDir() + "api_key"));
    }

    public Preferences(File settings, File apikey){
        Exception e= null;
        int set_file = 0;
        int api =0;
        if (!settings.exists() || !settings.canRead()) {
            set_file =1;
        }
        if( !apikey.exists() || !apikey.canRead()){
            api = 1;
        }

        BufferedReader reader;

       try {
           reader = new BufferedReader(new FileReader(settings));
           setHost(reader.readLine());
           setPort(reader.readLine());
           String temp = reader.readLine();
           if (temp.charAt(temp.length()-1) !='/'){
               setCache(getHome() + "/"+ temp+ "/" );
           }
           else{
               setCache(getHome() + "/"+ temp );
           }



       }
       catch (Exception ex){
           e = ex;
           set_file =2;
       }

       try {
           reader = new BufferedReader(new FileReader(apikey));
           setApi_key(reader.readLine());
       }
       catch (Exception ex){
           e = ex;
           api =2;
       }

       if(set_file == 1 || api == 1){
           this.status = "Error in opening";
       }
       else if(set_file == 2 || api == 2){
           this.status = e.getMessage();
       }
       else{
           this.status = "OK";
       }
    }

    private static File openFile(String path){
        File f = new File(path);
        if (f.exists() && f.canRead()){
            return f;
        }
        throw new RuntimeException("Config file missing on: "+ path);

    }
    private static String getHome(){
        return System.getProperty("user.home") ;
    }
    // CA: all preference related operations should come from here.  ????
    // make them private, setting, getting, reading from the file, etc.

    public static int CONNECT_TIMEOUT_SECS = 10;

    // 30 seconds is too high.. even 10 might be..
    // We'll revise this at some point. and have some defaults here,
    // which will be initialized in the file,
    public static int READ_TIMEOUT_SECS = 30;

    public String getApi_key() {
        return api_key;
    }

    public void setApi_key(String api_key) {
        this.api_key = api_key;
    }

    public String getPort() {
        return port;
    }

    public void setPort(String port) {
        this.port = port;
    }

    public String getCache() {
        return cache;
    }

    public void setCache(String cache) {
        this.cache = cache;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }
}
