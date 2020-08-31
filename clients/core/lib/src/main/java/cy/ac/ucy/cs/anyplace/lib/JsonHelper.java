package cy.ac.ucy.cs.anyplace.lib;

import kotlin.reflect.KType;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

public class JsonHelper {

    public static final int STATUS_ERR = 1;

    public static String printError(Exception exception, String method ) {

        JSONObject r = new JSONObject();
        try {
            r.put("status", STATUS_ERR);
            r.put("method", method);
            r.put("cause", exception.getCause());
            r.put("trace", exception.getStackTrace());
        } catch (JSONException ex) {
            return "{" + "\"" + "status" + "\"" +":0"+","+"\"" + "message" +"\""+":The JsonHelper Failed. Should not happen}";
        }

        return r.toString();
    }

    public static String jsonResponse(int status, String message){
        if (message == null){
            return "The response from server is null";
        }
        return "{" + "\""+"status" + "\"" + ":" + status +","+message.substring(1);
    }




}
