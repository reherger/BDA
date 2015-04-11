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

    private static float xAcc;
    private static float yAcc;

    private static float xDistanceSensor;
    private static float yDistanceSensor;

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

        // Convert Nanosec to Sec
        System.out.println("actualTime (nano)= "+aT);
        actualTime = aT;
        System.out.println("actualTime (sec)= "+actualTime);
        lastUpdate = lU;

        float dt = actualTime - lastUpdate;
        dt = dt / 1000000000;
        System.out.println("dt= "+dt);

        if(xDirection.length == 2 && yDirection.length == 2){
            //System.out.println("XDirection : "+xDist);
            //System.out.println("X Distance : "+xDist);
            xSpeed = ((xDirection[0] + xDirection[1])/(2))*dt;
            ySpeed = ((yDirection[0] + yDirection[1])/(2))*dt;
            //xDist += xSpeed*dt;
            //yDist += ySpeed*dt;
            xAcc = xDirection[0];
            yAcc = yDirection[0];
            //xSpeed = (xDirection[1]*actualTime)-(xDirection[0]*lastUpdate);
            //ySpeed = (yDirection[1]*actualTime)-(yDirection[0]*lastUpdate);

            xDist = xSpeed*dt;
            //System.out.println("xSpeed * dt: "+Float.toString(xSpeed) +" * "+dt);
            //System.out.println("Distance calculation = "+xSpeed*dt);
            //System.out.println("X Distance covered: "+xDist );

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

    public static float getXSpeed() {
        return xSpeed;
    }

    public static void setXSpeed(float xSpeed) {
        DataHandler.xSpeed = xSpeed;
    }

    public static float getYSpeed() {
        return ySpeed;
    }

    public static void setYSpeed(float ySpeed) {
        DataHandler.ySpeed = ySpeed;
    }

    public static float getxAcc() {
        return xAcc;
    }

    public static void setxAcc(float xAcc) {
        DataHandler.xAcc = xAcc;
    }

    public static float getyAcc() {
        return yAcc;
    }

    public static void setyAcc(float yAcc) {
        DataHandler.yAcc = yAcc;
    }

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
}
