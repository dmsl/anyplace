import cy.ac.ucy.cs.anyplace.*;

import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;



@Category(Buildtests.class)
public class TesterPreferences {
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
    static String aps[];
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
        aps = new String[]{"{\"bssid\":\"d4:d7:48:d8:28:b0\",\"rss\":-40}", "{\"bssid\":\"00:0e:38:7a:37:77\",\"rss\":-50}"};
    }



    @Test
    public void testPoiDetails(){
        Anyplace client = new Anyplace(preferences);

        response = client.poiDetails(pois_from);
        //System.out.println(response + "\n");

    }

    @Test
    public void testRadioHeatMapBuildingFoor() {
        Anyplace client = new Anyplace(preferences);

        response = client.radioheatMapBuildingFloor(buid, floor);
        //System.out.println(response + "\n");

    }

    @Test
    public void testRadioByBuildingFloorRange() {
        Anyplace client = new Anyplace(preferences);

        //response = client.radioByBuildingFloorRange( buid, floor, coordinates_la, coordinates_lo, range);
        //System.out.println(response + "\n");
    }

    @Test
    public void testAllBuildingFloorPOIs() {
        Anyplace client = new Anyplace(preferences);

        response = client.allBuildingFloorPOIs(buid, floor);
        //System.out.println(response + "\n");
    }

    @Test
    public void testFloorplans64() {
        Anyplace client = new Anyplace(preferences);

        response = client.floorplans64(buid, floor);
        //System.out.println(response.substring(0, 100) + "\n");
    }

    @Test
    public void testFloortiles() {
        Anyplace client = new Anyplace(preferences);

        response = client.floortiles( buid, floor);
        //System.out.println(response/* .substring(0, 100) */ + "\n");
    }

    @Test
    public void testRadioByBuildingFloor() {
        Anyplace client = new Anyplace(preferences);

        response = client.radioByBuildingFloor(buid, floor);
        //System.out.println(response/* .substring(0, 100) */ + "\n");
    }

    @Test
    public void testNavigationXY() {
        Anyplace client = new Anyplace(preferences);

        response = client.navigationXY( pois_to, buid, floor, coordinates_la, coordinates_lo);
        //System.out.println(response/* .substring(0, 100) */ + "\n");

    }

    @Test
    public void testRadioByCoordinatesFloor() {
        Anyplace client = new Anyplace(preferences);

       // response = client.radioByCoordinatesFloor(coordinates_la, coordinates_lo, floor);

    }

    @Test
    public void testAllBuildingFloors() {
        Anyplace client = new Anyplace(preferences);


        response = client.allBuildingFloors(buid);
        //System.out.println(response + "\n");

    }

    @Test
    public void testNavigationPoiToPoi() {
        Anyplace client = new Anyplace(preferences);

        response = client.navigationPoiToPoi( pois_to, pois_from);
        //System.out.println(response + "\n");
    }

    @Test
    public void testConnectionsByFloor() {
        Anyplace client = new Anyplace(preferences);

        response = client.connectionsByFloor(buid, floor);
        //System.out.println(response +"\n");
    }

    @Test
    public void testBuildingAll(){
        Anyplace client = new Anyplace(preferences);

        response = client.buildingAll();
    }

    @Test
    public void testBuildingsByCampus(){
        Anyplace client = new Anyplace(preferences);
        response = client.buildingsByCampus("1");
        //System.out.println(response);
    }

    @Test
    public void testBuildingsByBuildingCode(){
        Anyplace client = new Anyplace(preferences);

        response = client.buildingsByBuildingCode(buid);
    }

    @Test
    public void testNearbyBuildings(){
        Anyplace client = new Anyplace(preferences);

        response = client.nearbyBuildings(coordinates_la,coordinates_lo );

    }

    @Test
    public void testAllBuildingPOIs(){
        Anyplace client = new Anyplace(preferences);
        response = client.allBuildingPOIs(buid);
    }

    @Test
    public void testEstimatePosition(){
        Anyplace client = new Anyplace(preferences);
        response = client.estimatePosition(buid,floor,aps,algorithm);
        //System.out.println(response);

    }


}
