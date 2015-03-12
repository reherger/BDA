package ch.hslu.herger.beacon;

import com.estimote.sdk.Beacon;

import java.util.List;

import ch.hslu.herger.config.Configuration;
import ch.hslu.herger.config.LocationReader;
import ch.hslu.herger.config.XMLBeacon;
import ch.hslu.herger.config.XMLLocation;

/**
 * Created by Reto Herger on 12.03.2015.
 */
public class BeaconComparator {

    public static XMLBeacon isBeaconKnown(Beacon nearestBeacon, List<XMLLocation> locationList){
        String nBMajor = Integer.toString(nearestBeacon.getMajor());

        XMLBeacon recognizedB = new XMLBeacon();

        for(XMLLocation loc : locationList){
            for(XMLBeacon b : loc.getBeaconList()){
                if(nBMajor.equalsIgnoreCase(b.getMajor())){
                    recognizedB = b;
                }
            }
        }

        if(recognizedB.getMajor().equalsIgnoreCase(nBMajor)){
            if(nearestBeacon.getRssi()>-60){
                return recognizedB;
            }else{
                return null;
            }
        }else{
            return null;
        }

    }

}
