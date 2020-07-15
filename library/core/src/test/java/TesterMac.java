import cy.ac.ucy.cs.anyplace.*;


import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.Arrays;


import org.junit.BeforeClass;
import org.junit.Test;

    public class TesterMac {

        static String response;
        static String buid;
        static String access_token;
        static String pois_to;
        static String coordinates_la;
        static String coordinates_lo;
        static String floor;
        static String pois_from;
        static String range;
        static String algorithm;
        static Preferences preferences;


        @BeforeClass
        public static void setUpParameters() throws Exception {
            preferences = new Preferences();
            buid = "username_1373876832005";
            access_token = "eyJhbGciOiJSUzI1NiIsImtpZCI6IjhjNThlMTM4NjE0YmQ1ODc0MjE3MmJkNTA4MGQxOTdkMmIyZGQyZjMiLCJ0eXAiOiJKV1QifQ.eyJpc3MiOiJhY2NvdW50cy5nb29nbGUuY29tIiwiYXpwIjoiNTg3NTAwNzIzOTcxLXNpOHM0cXFhdDl2NWVmZ2VtbmViaWhwaTNxZTlvbmxwLmFwcHMuZ29vZ2xldXNlcmNvbnRlbnQuY29tIiwiYXVkIjoiNTg3NTAwNzIzOTcxLXNpOHM0cXFhdDl2NWVmZ2VtbmViaWhwaTNxZTlvbmxwLmFwcHMuZ29vZ2xldXNlcmNvbnRlbnQuY29tIiwic3ViIjoiMTA0NDQxMzA0OTI3MzE2MzM5NDM2IiwiZW1haWwiOiJhY2hpbC5jaHJpc3Rvc0BnbWFpbC5jb20iLCJlbWFpbF92ZXJpZmllZCI6dHJ1ZSwiYXRfaGFzaCI6InpSVUJ4cVBjT29xejB0cVpkNEg1WnciLCJuYW1lIjoiY2hyaXN0b3MgYWNoaWxsZW9zIiwicGljdHVyZSI6Imh0dHBzOi8vbGgzLmdvb2dsZXVzZXJjb250ZW50LmNvbS8tVTVqVzlpRk9kRVEvQUFBQUFBQUFBQUkvQUFBQUFBQUFBQUEvQUNIaTNyYzZfTEEzLWV2dGFJbXVTdDU0cFJRdmd1T1BOQS9zOTYtYy9waG90by5qcGciLCJnaXZlbl9uYW1lIjoiY2hyaXN0b3MiLCJmYW1pbHlfbmFtZSI6ImFjaGlsbGVvcyIsImxvY2FsZSI6ImVuIiwiaWF0IjoxNTcwMDIzNDE2LCJleHAiOjE1NzAwMjcwMTYsImp0aSI6ImMxMWY2YzIwMjgwZjc1YmMxZjE4NDMzM2QyZGM5NWY4MTYxYTZkNWUifQ.W_8IsTty5D7UdbcHkjrHyhNkEOyFc1r8fluvnd3kpV5wmK9Z4Tb0zv-W9DOr6mOGZUbaLvHR0Hncbqgec_iN9YNV281O3NRd-XERsn-Gf3oZ2z0Nbm5-_4NRg-WkLER4Ouo-upCd9TvXZwWqK0NNZm1Ka8N_JCzU0vb29T7lASZAZQ5POLtg3Z7PoAIk-h1HoO8Wb8acb-fkVaoLd-WR4sEhC93mxEaKe3DycXT0QtaO27GAYypz6HfWM3PsyPHio9nGr-GSt7ZNZuJYjnzqyRhXnx-H2dRggWbS6EAREWmBH2sdWe7fzMBFt_GNCl9q3yGVJQht5IOTmPDG9gixsw";
            pois_to = "poi_064f4a01-07bd-45fa-9579-63fa197d3d90";
            coordinates_la = "35.14414934169342";
            coordinates_lo = "33.41130472719669";
            floor = "-1";
            pois_from = "poi_88a34fd5-75bd-4601-81dc-fe5aef69bd3c";
            range = "100";
            algorithm = "1";
        }
        public static boolean isNumeric(String str) {
            try {
                Double.parseDouble(str);
                return true;
            } catch (NumberFormatException e) {
                return false;
            }
        }
        @Test
        public void testMacFingerprints() {
            String cmd[] = new String[3];
            cmd[0] = "/bin/sh";
            cmd[1] = "-c";
            cmd[2] = "/System/Library/PrivateFrameworks/Apple80211.framework/Versions/A/Resources/airport -s | grep ':' | tr -s ' ' | cut -d' ' -f3 -f4| tr ' ' '\n'";

            String aps[] = new String[200];
            Process p;
            String s, temp;
            int counter = 0;
            try {
                p = Runtime.getRuntime().exec(cmd);

                BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()));
                while ((s = br.readLine()) != null && counter <= 20) {
                    temp = "{\"bssid\":\"";
                    temp += s;
                    temp += "\",\"rss\":";
                    s = br.readLine();
                    if (!isNumeric(s)) {
                        continue;
                    }
                    temp += s;
                    temp += "}";
                    temp = temp.toLowerCase();
                    aps[counter++] = temp;
                }
                p.destroy();
                br.close();
            } catch (Exception ex) {
                ex.printStackTrace();
            }
            aps = Arrays.copyOf(aps, counter);
            for (int j = 0; j < counter; j++) {

                //System.out.println(aps[j]);
            }
            Anyplace client2 = new Anyplace("ap-dev.cs.ucy.ac.cy", "443", "res/");

            response = client2.estimatePosition(buid, floor, aps, algorithm);
            //System.out.println(response + "\n");

        }
}
