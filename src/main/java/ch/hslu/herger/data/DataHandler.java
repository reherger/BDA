package ch.hslu.herger.data;

import com.estimote.sdk.Beacon;

import java.util.HashMap;

import ch.hslu.herger.config.XMLBeacon;

/**
 * Created by Reto Herger on 20.03.2015.
 */
public class DataHandler {

    private static DataHandler instance = null;

    //Location
    private static float angleToNorth;

    //Beacons
    private static Beacon currentBeacon;
    private static XMLBeacon currentXMLBeacon;

    //Sensors
    private static float xDistanceSensor;
    private static float yDistanceSensor;

    public static DataHandler getInstance() {
        if(instance == null) {
            instance = new DataHandler();
        }
        return instance;
    }


    public static void calcPos(Float[] xD, Float[] yD, long lU, long aT){


    }

    /**
     * Getter & Setter
     */

    public static float getXDistanceSensor() {
        return xDistanceSensor;
    }

    public static void setXDistanceSensor(float xDistanceSensor) {
        DataHandler.xDistanceSensor = xDistanceSensor;
    }

    public static float getYDistanceSensor() {
        return yDistanceSensor;
    }

    public static void setYDistanceSensor(float yDistanceSensor) {
        DataHandler.yDistanceSensor = yDistanceSensor;
    }

    public static float getAngleToNorth() {
        return angleToNorth;
    }

    public static void setAngleToNorth(float angleToNorth) {
        DataHandler.angleToNorth = angleToNorth;
    }

    public static Beacon getCurrentBeacon() {
        return currentBeacon;
    }

    public static void setCurrentBeacon(Beacon currentBeacon) {
        DataHandler.currentBeacon = currentBeacon;
    }

    public static XMLBeacon getCurrentXMLBeacon() {
        return currentXMLBeacon;
    }

    public static void setCurrentXMLBeacon(XMLBeacon currentXMLBeacon) {
        DataHandler.currentXMLBeacon = currentXMLBeacon;
    }
}
