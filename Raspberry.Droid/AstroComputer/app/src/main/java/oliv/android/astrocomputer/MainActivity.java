package oliv.android.astrocomputer;

import android.content.pm.ActivityInfo;
import android.location.Location;
import android.os.Bundle;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;
//import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import oliv.android.AstroComputer;
import oliv.android.GeomUtil;
import oliv.android.SightReductionUtil;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

import static oliv.android.astrocomputer.GPSTracker.LOG_TAG;

/*
 * There is commented code in this class.
 * This is still a work in progress, mostly at the life cycle level.
 */
public class MainActivity extends AppCompatActivity {

    private final static long BW_LOOPS = 250L;
    private final static int SPEED_THRESHOLD = 40; // Ignore if speed is above this limit (im m/s)

    private TextView dateTimeHolder = null;
    private TextView gpsDataHolder = null;
    private TextView sunDataHolder = null;
    private Spinner bodySpinner = null;
    private TextView userMessageZone = null;
    private TextView progMessageZone = null;
    private Button logButton = null;

    private boolean isLogging = false;  // Logging GPS Data
    private boolean firstTimeLogging = false;
    private BufferedWriter logger = null;

    private String logFileName = "";

    private final MainActivity instance = this;
    private final SimpleDateFormat DF = new SimpleDateFormat("dd-MMM-yyyy'\n'HH:mm:ss Z z", Locale.getDefault());
    private final SimpleDateFormat DF_FILE_NAME = new SimpleDateFormat("'GPS_DATA_'dd_MMM_yyyy_HH_mm_ss'.csv'", Locale.getDefault());

    private void setText(final TextView text, final String value) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                text.setText(value);
            }
        });
    }

    private long lastClick = -1;

    public void onLogButtonClick(View view) {
        long thisClick = System.currentTimeMillis();
        if ((thisClick - lastClick) > 750) { // Double-click. Each click less that 750ms apart.
            userMessageZone.setText("First click...");
            lastClick = thisClick;
            return;
        }
        lastClick = thisClick;
        isLogging = !isLogging;
        firstTimeLogging = "Log GPS Data".equals(logButton.getText()); // Compare to original button label
        logButton.setText(isLogging ? "Pause Logging" : "Resume Logging");
        if (isLogging) {
            try {
                File logFile = new File(getExternalFilesDir(null), logFileName);
                boolean exists = logFile.exists();
                if (exists && firstTimeLogging) {
                    boolean ok = logFile.delete();
                    userMessageZone.setText(String.format("Resetting data in %s: %s", logFileName, (ok ? "OK" : "failed")));
                } else {
                    userMessageZone.setText(String.format("Logging data in %s", logFileName));
                }
                logger = new BufferedWriter(new FileWriter(logFile, true)); // true: append
                if (!exists) {
                    String loggingHeader = "epoch;fmt-date;longitude;latitude;speed;heading\n";
                    logger.write(loggingHeader);
                }
                String loggingComment = String.format(Locale.getDefault(), "# Logging %s at %d\n", (exists ? "resumed" : "started"), System.currentTimeMillis());
                userMessageZone.setText(String.format(Locale.getDefault(), "%s\n%s", userMessageZone.getText(), loggingComment));
            } catch (IOException ioe) {
                userMessageZone.setText(ioe.toString());
            }
        } else {
            if (logger != null) {
                try {
                    logger.flush();
                    logger.close();
                    userMessageZone.setText("Logging was paused");
                } catch (IOException ioe) {
                    userMessageZone.setText(ioe.toString());
                }
            }
        }
    }

    protected void setProgMessage(double elapsed, String mess) {
        if (elapsed > 0) {
            progMessageZone.setText(mess);
        }
    }

    // Loops every second. The thread way for Android...
    private class Chronometer implements Runnable {

        private volatile transient boolean exit = false;

        @Override
        public void run() {
            Looper.prepare(); // Mandatory for a Thread on Android.
//            Looper.loop();

            double latitude = -Double.MAX_VALUE;
            double longitude = -Double.MAX_VALUE;
            double sog = -1f;
            double cog = -1f;

            Location lastLocation = null;

            while (!this.exit) {
                String gpsData = " No GPS ";
                String astroData = " - none -";
                // Current date and time
                Calendar c = Calendar.getInstance();
//                System.out.println("Current time => " + c.getTime());
                String formattedDate = DF.format(c.getTime());
                // formattedDate have current date/time

                boolean validLocation = false;

                if (gps != null) {
                    if (gps.canGetLocation()) {

                        Location gpsLocation = gps.getLocation(instance);
                        double elapsedTime = 0d;

                        latitude = gpsLocation.getLatitude();
                        longitude = gpsLocation.getLongitude();

                        if (gpsLocation.hasSpeed()) {
                            sog = gpsLocation.getSpeed();
                            cog = gpsLocation.getBearing();
                        } else {
                            if (lastLocation != null) {
                                elapsedTime = (gpsLocation.getTime() - lastLocation.getTime()) / 1_000; // Convert milliseconds to seconds
                                if (elapsedTime > 0) {
                                    validLocation = true;
                                    sog = lastLocation.distanceTo(gpsLocation) /* in meters */ / elapsedTime;
                                    cog = lastLocation.bearingTo(gpsLocation);
                                    if (cog < 0) {
                                        cog += 360;
                                    }
                                    if (sog > SPEED_THRESHOLD) { // Wacky point
                                        validLocation = false;
                                    }
                                }
                            }
                        }
                        lastLocation = gpsLocation;
                        progMessageZone.setText(String.format(Locale.getDefault(), "Got GPS Data: %s: %f / %f\n(elapsed %.02f s)", formattedDate, latitude, longitude, elapsedTime));
                        //if (elapsedTime > 0) {
                        //  instance.setProgMessage(elapsedTime, String.format(Locale.getDefault(), "Got GPS Data: %s: %f / %f\n(elapsed %.02f s)", formattedDate, latitude, longitude, elapsedTime));
                        //}

                        // \n is for new line
//                    Toast.makeText(getApplicationContext(), "Your Location is - \nLat: " + latitude + "\nLong: " + longitude, Toast.LENGTH_LONG).show();
                        gpsData = String.format("GPS Data:\n%s\n%s\n%s\n%s",
                                GeomUtil.decToSex(latitude, GeomUtil.DEFAULT_DEG, GeomUtil.NS) ,
                                GeomUtil.decToSex(longitude, GeomUtil.DEFAULT_DEG, GeomUtil.EW),
                                String.format(Locale.getDefault(), "SOG: %.02f m/s (%.02f km/h)", sog, (sog * 3.6)),
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

                            Object selectedBody = instance.bodySpinner.getSelectedItem();
                            if (selectedBody.toString().contains("Moon")) {
                                sru.setAHG(AstroComputer.getMoonGHA());
                                sru.setD(AstroComputer.getMoonDecl());
                            } else if (selectedBody.toString().contains("Venus")) {
                                sru.setAHG(AstroComputer.getVenusGHA());
                                sru.setD(AstroComputer.getVenusDecl());
                            } else if (selectedBody.toString().contains("Mars")) {
                                sru.setAHG(AstroComputer.getMarsGHA());
                                sru.setD(AstroComputer.getMarsDecl());
                            } else if (selectedBody.toString().contains("Jupiter")) {
                                sru.setAHG(AstroComputer.getJupiterGHA());
                                sru.setD(AstroComputer.getJupiterDecl());
                            } else if (selectedBody.toString().contains("Saturn")) {
                                sru.setAHG(AstroComputer.getSaturnGHA());
                                sru.setD(AstroComputer.getSaturnDecl());
                            } else {                                  // Sun by default
                                sru.setAHG(AstroComputer.getSunGHA());
                                sru.setD(AstroComputer.getSunDecl());
                            }

//                            sru.setAHG(AstroComputer.getSunGHA());
//                            sru.setD(AstroComputer.getSunDecl());

                            sru.calculate();
                            double obsAlt = sru.getHe();
                            double z = sru.getZ();

                            astroData = String.format(Locale.getDefault(),
                                    "%s Data:\nElevation: %s\nZ: %.02f\272",
                                    selectedBody.toString(),
                                    GeomUtil.decToSex(obsAlt,
                                            GeomUtil.SWING,
                                            GeomUtil.NONE),
                                    z);
                        }

                    } else {
                        gpsData = "Cannot get Position";
                    }
                }

//                content = String.format("%s\n%s\n%s", formattedDate, gpsData, astroData);
//                Toast.makeText(instance, content, Toast.LENGTH_SHORT).show();
//                setText(instance.dateTimeHolder, content);
//                instance.dateTimeHolder.setText(content);

//                Toast.makeText(instance, formattedDate, Toast.LENGTH_SHORT).show();
//                Toast.makeText(instance, gpsData, Toast.LENGTH_SHORT).show();
//                Toast.makeText(instance, astroData, Toast.LENGTH_SHORT).show();

                setText(instance.dateTimeHolder, formattedDate);
                setText(instance.gpsDataHolder, gpsData);
                setText(instance.sunDataHolder, astroData);

                if (instance.isLogging && validLocation) {
                    // Log data here
                    // "epoch;fmt-date;latitude;longitude;speed;heading";
                    String dataLine = String.format(
                            Locale.getDefault(),
                            "%d;%s;%f;%f;%f;%f\n",
                            c.getTimeInMillis(),
                            formattedDate.replace('\n', ' '),
                            longitude,
                            latitude,
                            sog,
                            cog
                    );
                    try {
                        logger.write(dataLine);
                    } catch (IOException ioe) {
                        userMessageZone.setText(String.format("Error logging data: %s", ioe.toString()));
                    }
                }

                try {
                    Thread.sleep(BW_LOOPS);
                } catch (InterruptedException ie) {
                    Log.d(LOG_TAG, ie.toString());
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
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT); // Force portrait, to avoid restart (there must be a better way...)
//        Toast.makeText(this, formattedDate, Toast.LENGTH_SHORT).show();
        // Now we display formattedDate value in TextView
        // Warning: the lines below create a TextView programmatically, ignoring the LayoutEditor directives
//        this.dateTimeHolder = new TextView(this);
//        dateTimeHolder.setGravity(Gravity.CENTER);
//        dateTimeHolder.setTextSize(20);
//        dateTimeHolder.setTextColor(Color.BLUE);
//        setContentView(dateTimeHolder);

        // By ID:
        this.dateTimeHolder = this.findViewById(R.id.dateTime);
        this.gpsDataHolder = this.findViewById(R.id.gpsData);
        this.sunDataHolder = this.findViewById(R.id.sunData);
        this.bodySpinner = this.findViewById(R.id.body);
        this.bodySpinner.setSelection(0, true);
        View view = this.bodySpinner.getSelectedView();
        ((TextView)view).setTextSize(20);
        this.userMessageZone = this.findViewById(R.id.userMessage);
        this.progMessageZone = this.findViewById(R.id.progMessage);
        this.logButton = this.findViewById(R.id.logButton);

        this.dateTimeHolder.setText("- No date -"); // String.format("Current Date and Time :\n%s", "---"));
        this.gpsDataHolder.setText("- No GPS -"); // String.format("Current Date and Time :\n%s", "---"));
        this.sunDataHolder.setText("- No Celestial data -"); // String.format("Current Date and Time :\n%s", "---"));

        chronometer = new Chronometer();
        Thread timer = new Thread(chronometer, "Chronometer");
        timer.start();
        gps = new GPSTracker(this);

        logFileName = DF_FILE_NAME.format(new Date());
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onRestart() {
        super.onRestart();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (chronometer != null) {
            chronometer.stop();
        }
        if (isLogging) {
            try {
                logger.flush();
                logger.close();
            } catch (IOException ioe) {
                Log.d(LOG_TAG, ioe.toString());
            }
        }
    }
}
