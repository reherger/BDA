package ch.hslu.herger.config;

import android.app.Application;

import java.util.List;

/**
 * Created by reto on 06.03.2015.
 */
public class Configuration extends Application {

    private List<XMLLocation> locationList;
    private XMLLocation currentLocation;

    public List<XMLLocation> getLocationList() {
        return locationList;
    }
    public void setLocationList(List<XMLLocation> locationList) {
        this.locationList = locationList;
        System.out.println("CONFIGURATION SET LOCATIONLIST");
    }
    public XMLLocation getCurrentLocation() {
        return currentLocation;
    }
    public void setCurrentLocation(XMLLocation currentLocation) {
        this.currentLocation = currentLocation;
    }
}
