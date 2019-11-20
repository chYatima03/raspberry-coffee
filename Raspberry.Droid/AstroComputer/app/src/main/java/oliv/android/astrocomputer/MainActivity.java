package oliv.android.astrocomputer;

import android.os.Bundle;
import android.os.Looper;
import android.view.Gravity;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import oliv.android.AstroComputer;
import oliv.android.GeomUtil;
import oliv.android.SightReductionUtil;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;
import java.util.TimeZone;

public class MainActivity extends AppCompatActivity {

    private TextView timeHolder = null;
    private final MainActivity instance = this;
    private final SimpleDateFormat DF = new SimpleDateFormat("dd-MMM-yyyy'\n'HH:mm:ss Z z");

    private void setText(final TextView text, final String value) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                text.setText(value);
            }
        });
    }

    private class Chronometer implements Runnable {

        private volatile transient boolean exit = false;

        @Override
        public void run() {
            Looper.prepare(); // Mandatory for a Thread on Android.
//            Looper.loop();

            while (!this.exit) {
                String content = "";
                String gpsData = " No GPS ";
                String sunData = " - none -";
                // Current date and time
                Calendar c = Calendar.getInstance();
//                System.out.println("Current time => " + c.getTime());
                String formattedDate = DF.format(c.getTime());
                // formattedDate have current date/time

                if (gps != null) {
                    if (gps.canGetLocation()) {

                        double latitude = gps.getLatitude();
                        double longitude = gps.getLongitude();

                        float sog = gps.getSpeed();
                        float cog = gps.getBearing();

                        // \n is for new line
//                    Toast.makeText(getApplicationContext(), "Your Location is - \nLat: " + latitude + "\nLong: " + longitude, Toast.LENGTH_LONG).show();
                        gpsData = String.format("GPS Data:\n%s\n%s\n%s\n%s",
                                GeomUtil.decToSex(latitude, GeomUtil.DEFAULT_DEG, GeomUtil.NS) ,
                                GeomUtil.decToSex(longitude, GeomUtil.DEFAULT_DEG, GeomUtil.EW),
                                String.format(Locale.getDefault(), "SOG: %.02f kn", sog),  // Verify unit
                                String.format(Locale.getDefault(), "COG: %.01f\272", cog));

                        // Celestial Data
                        if (true) {
                            Calendar date = Calendar.getInstance(TimeZone.getTimeZone("Etc/UTC")); // Now
                            AstroComputer.calculate(
                                    date.get(Calendar.YEAR),
                                    date.get(Calendar.MONTH) + 1,
                                    date.get(Calendar.DAY_OF_MONTH),
                                    date.get(Calendar.HOUR_OF_DAY), // and not just HOUR !!!!
                                    date.get(Calendar.MINUTE),
                                    date.get(Calendar.SECOND));
                            SightReductionUtil sru = new SightReductionUtil();

                            sru.setL(latitude);
                            sru.setG(longitude);

                            sru.setAHG(AstroComputer.getSunGHA());
                            sru.setD(AstroComputer.getSunDecl());
                            sru.calculate();
                            double obsAlt = sru.getHe();
                            double z = sru.getZ();

                            sunData = String.format("Sun Data:\nElevation: %s\nZ: %.02f\272", GeomUtil.decToSex(obsAlt, GeomUtil.SWING, GeomUtil.NONE), z);
                        }

                    } else {
                        gpsData = "Cannot get Position";
                    }
                }

                content = String.format("%s\n%s\n%s", formattedDate, gpsData, sunData);
                Toast.makeText(instance, content, Toast.LENGTH_SHORT).show();

                setText(instance.timeHolder, content);
//                instance.timeHolder.setText(content);

                try {
                    Thread.sleep(1_000L);
                } catch (InterruptedException ie) {
                    ie.printStackTrace();
                }
            }
        }

        void stop() {
            this.exit = true;
        }
    }
    private Chronometer chronometer = null;
    GPSTracker gps = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
//        Toast.makeText(this, formattedDate, Toast.LENGTH_SHORT).show();
        // Now we display formattedDate value in TextView
        this.timeHolder = new TextView(this);
        timeHolder.setText(String.format("Current Date and Time :\n%s", "---"));
        timeHolder.setGravity(Gravity.CENTER);
        timeHolder.setTextSize(20);
        setContentView(timeHolder);
        chronometer = new Chronometer();
        Thread timer = new Thread(chronometer, "Chronometer");
        timer.start();
        gps = new GPSTracker(this);
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (chronometer != null) {
            chronometer.stop();
        }
    }
}
