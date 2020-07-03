package cy.ac.ucy.cs.anyplace;

public class LatLng {

    public double longitude;
    public double latitude;

    public LatLng(double lat, double lng){

        this.latitude=lat;
        this.longitude=lng;
    }

    public double lng(){
        return longitude;
    }

    public double lat(){
        return latitude;
    }
    public boolean equals(LatLng x, LatLng y){

        if(x.latitude == y.latitude && x.longitude==y.longitude){
            return true;
        }
        else{
            return false;
        }
    }
    @Override
    public String toString(){
        String s = latitude +"," + longitude;
        return s;
    }
}
