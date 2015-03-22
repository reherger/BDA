package ch.hslu.herger.data;

import java.util.HashMap;

/**
 * Created by Reto Herger on 20.03.2015.
 */
public class DataHandler {

    private static DataHandler instance = null;

    private static float currXPos;
    private static float currYPos;

    private static long lastUpdate;
    private static long actualTime;

    private static Float[] xDirection = new Float[2];
    private static Float[] yDirection = new Float[2];

    private static float xSpeed;
    private static float ySpeed;

    private static float xDist;
    private static float yDist;

    private float compass;

    public static DataHandler getInstance() {
        if(instance == null) {
            instance = new DataHandler();
        }
        return instance;
    }

    /**
     * Getter & Setter
     */
    public static void calcPos(Float[] xD, Float[] yD, long lU, long aT){
        xDirection = xD;
        yDirection = yD;
        lastUpdate = lU;
        actualTime = aT;

        long dt = actualTime - lastUpdate;

        if(xDirection.length == 2 && yDirection.length == 2){
            xSpeed += ((xDirection[0] + xDirection[1])/(2.0))*dt;
            ySpeed += ((yDirection[0] + yDirection[1])/(2.0))*dt;
            xDist = xSpeed*dt;
            yDist = ySpeed*dt;
        }

    }


    public static float getCurrXPos() {
        return currXPos;
    }

    public static void setCurrXPos(float currXPos) {
        DataHandler.currXPos = currXPos;
    }

    public static float getCurrYPos() {
        return currYPos;
    }

    public static void setCurrYPos(float currYPos) {
        DataHandler.currYPos = currYPos;
    }

    public long getLastUpdate() {
        return lastUpdate;
    }

    public void setLastUpdate(long lastUpdate) {
        this.lastUpdate = lastUpdate;
    }

    public long getActualTime() {
        return actualTime;
    }

    public void setActualTime(long actualTime) {
        this.actualTime = actualTime;
    }

    public Float[] getxDirection() {
        return xDirection;
    }

    public void setxDirection(Float[] xDirection) {
        this.xDirection = xDirection;
    }

    public Float[] getyDirection() {
        return yDirection;
    }

    public void setyDirection(Float[] yDirection) {
        this.yDirection = yDirection;
    }

    public float getCompass() {
        return compass;
    }

    public void setCompass(float compass) {
        this.compass = compass;
    }

    public static float getXDist() {
        return xDist;
    }

    public static void setXDist(float xDist) {
        DataHandler.xDist = xDist;
    }

    public static float getYDist() {
        return yDist;
    }

    public static void setYDist(float yDist) {
        DataHandler.yDist = yDist;
    }
}
