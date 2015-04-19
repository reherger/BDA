package ch.hslu.herger.main;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import ch.hslu.herger.main.R;

import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;

import ch.hslu.herger.config.Configuration;
import ch.hslu.herger.config.LocationReader;
import ch.hslu.herger.sensor.LinearAccelerationActivity;
import ch.hslu.herger.sensor.SensorFusionActivity;
import ch.hslu.herger.sensor.StepDetector;

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

    findViewById(R.id.ins_button).setOnClickListener(new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            Intent intent = new Intent(AllDemosActivity.this, SensorFusionActivity.class);
            startActivity(intent);
        }
    });

      findViewById(R.id.sensorFusion_Button).setOnClickListener(new View.OnClickListener() {
          @Override
          public void onClick(View v) {
              Intent intent = new Intent(AllDemosActivity.this, InsActivity.class);
              startActivity(intent);
          }
      });

      findViewById(R.id.sensorTest_Button).setOnClickListener(new View.OnClickListener() {
          @Override
          public void onClick(View v) {
              Intent intent = new Intent(AllDemosActivity.this, LinearAccelerationActivity.class);
              intent.putExtra(BeaconScannerActivity.EXTRAS_TARGET_ACTIVITY, InsActivity.class.getName());
              startActivity(intent);
          }
      });

      findViewById(R.id.stepCounter_Button).setOnClickListener(new View.OnClickListener() {
          @Override
          public void onClick(View v) {
              Intent intent = new Intent(AllDemosActivity.this, StepDetector.class);
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
