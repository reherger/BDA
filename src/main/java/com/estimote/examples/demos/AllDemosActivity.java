package com.estimote.examples.demos;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;

import ch.hslu.herger.config.Configuration;
import ch.hslu.herger.config.LocationReader;

/**
 * Shows all available demos.
 *
 * @author Reto Herger
 */
public class AllDemosActivity extends Activity {

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    setContentView(R.layout.all_demos);

    // Read configuration file
    setConfiguration();

    findViewById(R.id.distance_demo_button).setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        Intent intent = new Intent(AllDemosActivity.this, ListBeaconsActivity.class);
        intent.putExtra(ListBeaconsActivity.EXTRAS_TARGET_ACTIVITY, DistanceBeaconActivity.class.getName());
        startActivity(intent);
      }
    });
    findViewById(R.id.notify_demo_button).setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        Intent intent = new Intent(AllDemosActivity.this, ListBeaconsActivity.class);
        intent.putExtra(ListBeaconsActivity.EXTRAS_TARGET_ACTIVITY, NotifyDemoActivity.class.getName());
        startActivity(intent);
      }
    });
    findViewById(R.id.characteristics_demo_button).setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        Intent intent = new Intent(AllDemosActivity.this, ListBeaconsActivity.class);
        intent.putExtra(ListBeaconsActivity.EXTRAS_TARGET_ACTIVITY, CharacteristicsDemoActivity.class.getName());
        startActivity(intent);
      }
    });
    findViewById(R.id.ins_button).setOnClickListener(new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            Intent intent = new Intent(AllDemosActivity.this, BeaconScannerActivity.class);
            intent.putExtra(BeaconScannerActivity.EXTRAS_TARGET_ACTIVITY, InsActivity.class.getName());
            startActivity(intent);
        }
    });
    findViewById(R.id.XMLReader_button).setOnClickListener(new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            setConfiguration();
        }
    });
  }

    public void setConfiguration() {
        try {
            ((Configuration) this.getApplication()).setLocationList(LocationReader.readXML());
            Toast.makeText(this, "Configuration is set", Toast.LENGTH_LONG).show();
        }catch (XmlPullParserException e) {
            e.printStackTrace();
        }catch (IOException e) {
            e.printStackTrace();
        }
    }
}
