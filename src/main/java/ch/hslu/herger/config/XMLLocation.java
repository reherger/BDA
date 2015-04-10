package ch.hslu.herger.config;

import java.util.List;

/**
 * Created by Reto Herger on 05.03.2015.
 */
public class XMLLocation {
    private String id;
    private String name;
    private String pathToMap;
    private List<XMLBeacon> beaconList;
    private String angleToNorth;

    public String getId() {
        return id;
    }
    public void setId(String id) {
        this.id = id;
    }
    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }
    public String getPathToMap() {
        return pathToMap;
    }
    public void setPathToMap(String pathToMap) {
        this.pathToMap = pathToMap;
    }
    public List<XMLBeacon> getBeaconList() {
        return beaconList;
    }
    public void setBeaconList(List<XMLBeacon> beaconList) {
        this.beaconList = beaconList;
    }

    public String getAngleToNorth() {
        return angleToNorth;
    }

    public void setAngleToNorth(String angleToNorth) {
        this.angleToNorth = angleToNorth;
    }
}
