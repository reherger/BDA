package ch.hslu.herger.config;

import java.util.List;

/**
 * Created by Reto Herger on 02.05.2015.
 */
public class XMLRoom {
    private String roomId;
    private String xleftUpperCorner;
    private String yleftUpperCorner;
    private String width;
    private String height;
    private List<XMLDoor> doorList;

    public String getRoomId() {
        return roomId;
    }

    public void setRoomId(String roomId) {
        this.roomId = roomId;
    }

    public String getXleftUpperCorner() {
        return xleftUpperCorner;
    }

    public void setXleftUpperCorner(String xleftUpperCorner) {
        this.xleftUpperCorner = xleftUpperCorner;
    }

    public String getYleftUpperCorner() {
        return yleftUpperCorner;
    }

    public void setYleftUpperCorner(String yleftUpperCorner) {
        this.yleftUpperCorner = yleftUpperCorner;
    }

    public String getWidth() {
        return width;
    }

    public void setWidth(String width) {
        this.width = width;
    }

    public String getHeight() {
        return height;
    }

    public void setHeight(String height) {
        this.height = height;
    }

    public List<XMLDoor> getDoorList() {
        return doorList;
    }

    public void setDoorList(List<XMLDoor> doorList) {
        this.doorList = doorList;
    }
}
