package ch.hslu.herger.config;

import java.util.List;

/**
 * Created by Reto Herger on 05.03.2015.
 */
public class XMLLocation {
    private String id;
    private String name;
    private String pathToMap;
    private String angleToNorth;
    private String ratio;
    private List<XMLRoom> roomList;
    private List<XMLBeacon> beaconList;

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

    public String getAngleToNorth() {
        return angleToNorth;
    }

    public void setAngleToNorth(String angleToNorth) {
        this.angleToNorth = angleToNorth;
    }

    public String getRatio() {
        return ratio;
    }

    public void setRatio(String ratio) {
        this.ratio = ratio;
    }

    public List<XMLRoom> getRoomList() {
        return roomList;
    }

    public void setRoomList(List<XMLRoom> roomList) {
        this.roomList = roomList;
    }

    public List<XMLBeacon> getBeaconList() {
        return beaconList;
    }

    public void setBeaconList(List<XMLBeacon> beaconList) {
        this.beaconList = beaconList;
    }
}
