package ch.hslu.herger.config;

/**
 * Created by Reto Herger on 05.03.2015.
 */
public class XMLBeacon {
    private String uuid;
    private String major;
    private String minor;
    private String xPos;
    private String yPos;

    public String getUuid() {
        return uuid;
    }
    public void setUuid(String uuid) {
        this.uuid = uuid;
    }
    public String getMajor() {
        return major;
    }
    public void setMajor(String major) {
        this.major = major;
    }
    public String getMinor() {
        return minor;
    }
    public void setMinor(String minor) {
        this.minor = minor;
    }
    public String getxPos() { return xPos; }
    public void setxPos(String xPos) { this.xPos = xPos; }
    public String getyPos() { return yPos; }
    public void setyPos(String yPos) { this.yPos = yPos; }
}
