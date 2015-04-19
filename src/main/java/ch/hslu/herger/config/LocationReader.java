package ch.hslu.herger.config;


import android.util.Log;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.FileReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Reto Herger on 05.03.2015.
 */
public class LocationReader {

    public final static String SAMPLE_XML =
    "<?xml version=\"1.0\"?>"+
    "<locations>"+
        "<location>"+
        "<id>1</id>"+
        "<name>HSLU</name>"+
        "<pathToMap>hslu.jpg</pathToMap>"+
        "<angleToNorth>138</angleToNorth>"+
        "<beaconList>"+
            // blue
            "<beacon>"+
                "<uuid>01</uuid>"+
                "<major>110</major>"+
                "<minor>1110</minor>"+
                "<xPos>280</xPos>"+
                "<yPos>12</yPos>"+
            "</beacon>"+
            // green
            "<beacon>"+
                "<uuid>02</uuid>"+
                "<major>111</major>"+
                "<minor>1120</minor>"+
                "<xPos>145</xPos>"+
                "<yPos>75</yPos>"+
            "</beacon>"+
            // lightBlue
            // test
            "<beacon>"+
            "<uuid>02</uuid>"+
            "<major>112</major>"+
            "<minor>1130</minor>"+
            "<xPos>280</xPos>"+
            "<yPos>145</yPos>"+
            "</beacon>"+
        "</beaconList>"+
        "</location>"+
    "</locations>";

    private static List<XMLLocation> locList;
    private static XMLLocation location;
    private static List<XMLBeacon> beaconList;
    private static XMLBeacon beacon;

    public static List<XMLLocation> readXML() throws XmlPullParserException, IOException {
        XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
        factory.setNamespaceAware(true);
        XmlPullParser xpp = factory.newPullParser();
        System.out.println("parser implementation class is "+xpp.getClass());

        LocationReader app = new LocationReader();

        System.out.println("Parsing simple sample XML");//:\n"+ SAMPLE_XML);
        xpp.setInput( new StringReader( SAMPLE_XML ) );
        app.processDocument(xpp);

        return locList;
    }


    public void processDocument(XmlPullParser xpp) throws XmlPullParserException, IOException {
        int eventType = xpp.getEventType();
        do {
            if(eventType == xpp.START_DOCUMENT) {
                System.out.println("Start document");
            } else if(eventType == xpp.END_DOCUMENT) {
                System.out.println("End document");
            } else if(eventType == xpp.START_TAG) {
                processStartElement(xpp);
            } else if(eventType == xpp.END_TAG) {
                processEndElement(xpp);
            } else if(eventType == xpp.TEXT) {
                processText(xpp);
            }
            eventType = xpp.next();
        } while (eventType != xpp.END_DOCUMENT);
    }


    public void processStartElement (XmlPullParser xpp) throws XmlPullParserException, IOException {
        String name = xpp.getName();
        //String uri = xpp.getNamespace();
        //if ("".equals (uri)) {
        //    System.out.println("Start element: " + name);
        //} else {
        //    System.out.println("Start element (with uri): {" + uri + "}" + name);
        //}
        if(name.equalsIgnoreCase("locations")){
            locList = new ArrayList<XMLLocation>();
        }else if(name.equalsIgnoreCase("location")){
            location = new XMLLocation();
        }else if(name.equalsIgnoreCase("id")){
            xpp.next();
            location.setId(xpp.getText());
        }else if(name.equalsIgnoreCase("name")){
            xpp.next();
            location.setName(xpp.getText());
        }else if(name.equalsIgnoreCase("pathToMap")){
            xpp.next();
            location.setPathToMap(xpp.getText());
        }else if(name.equalsIgnoreCase("angleToNorth")){
            xpp.next();
            location.setAngleToNorth(xpp.getText());
        }else if(name.equalsIgnoreCase("beaconList")){
            beaconList = new ArrayList<XMLBeacon>();
        }else if(name.equalsIgnoreCase("beacon")){
            beacon = new XMLBeacon();
        }else if(name.equalsIgnoreCase("uuid")){
            xpp.next();
            beacon.setUuid(xpp.getText());
        }else if(name.equalsIgnoreCase("major")){
            xpp.next();
            beacon.setMajor(xpp.getText());
        }else if(name.equalsIgnoreCase("minor")){
            xpp.next();
            beacon.setMinor(xpp.getText());
        }else if(name.equalsIgnoreCase("xPos")){
            xpp.next();
            beacon.setxPos(xpp.getText());
        }else if(name.equalsIgnoreCase("yPos")){
            xpp.next();
            beacon.setyPos(xpp.getText());
        }


    }


    public void processEndElement (XmlPullParser xpp) {
        String name = xpp.getName();
        //String uri = xpp.getNamespace();
        //if ("".equals (uri))
        //    System.out.println("End element: " + name);
        //else
        //    System.out.println("End element:   {" + uri + "}" + name);

        if(name.equalsIgnoreCase("beacon")){
            beaconList.add(beacon);
        }else if(name.equalsIgnoreCase("beaconList")){
            location.setBeaconList(beaconList);
        }else if(name.equalsIgnoreCase("location")){
            locList.add(location);
        }else{
            // do nothing...
        }
    }

    int holderForStartAndLength[] = new int[2];

    public void processText (XmlPullParser xpp) throws XmlPullParserException {
        char ch[] = xpp.getTextCharacters(holderForStartAndLength);
        int start = holderForStartAndLength[0];
        int length = holderForStartAndLength[1];
        System.out.print("Characters:    \"");
        for (int i = start; i < start + length; i++) {
            switch (ch[i]) {
                case '\\':
                    System.out.print("\\\\");
                    break;
                case '"':
                    System.out.print("\\\"");
                    break;
                case '\n':
                    System.out.print("\\n");
                    break;
                case '\r':
                    System.out.print("\\r");
                    break;
                case '\t':
                    System.out.print("\\t");
                    break;
                default:
                    System.out.print(ch[i]);
                    break;
            }
        }
        System.out.print("\"\n");
    }
}
