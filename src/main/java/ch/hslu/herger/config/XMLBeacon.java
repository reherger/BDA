package ch.hslu.herger.config;

/**
 * Created by Reto Herger on 05.03.2015.
 */
public class XMLBeacon {
    String uuid;
    String major;
    String minor;

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
}
