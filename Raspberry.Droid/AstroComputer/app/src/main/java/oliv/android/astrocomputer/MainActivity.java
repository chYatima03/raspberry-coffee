package oliv.android.astrocomputer;

import android.content.pm.ActivityInfo;
import android.location.Location;
import android.os.Bundle;
import android.os.Looper;
import android.service.voice.VoiceInteractionSession;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
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
import java.util.Locale;
import java.util.TimeZone;

public class MainActivity extends AppCompatActivity {

    private TextView dateTimeHolder = null;
    private TextView gpsDataHolder = null;
    private TextView sunDataHolder = null;
    private Spinner bodySpinner = null;
    private TextView userMessageZone = null;
    private Button logButton = null;

    private boolean isLogging = false;
    private BufferedWriter logger = null;

    private final static String LOG_FILE_NAME = "GPS_DATA.csv";

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

    public void onLogButtonClick(View view) {
        isLogging = !isLogging;
        logButton.setText(isLogging ? "Pause Logging" : "Resume Logging");
        if (isLogging) {
            try {
                File logFile = new File(getExternalFilesDir(null), LOG_FILE_NAME);
                boolean exists = logFile.exists();
                logger = new BufferedWriter(new FileWriter(logFile, true)); // true: append
                userMessageZone.setText(String.format("Logging data in %s", LOG_FILE_NAME));
                if (!exists) {
                    String loggingHeader = "epoch;fmt-date;latitude;longitude;speed;heading\n";
                    logger.write(loggingHeader);
                }
                String loggingComment = String.format("# Logging %s at %d\n", (exists ? "resumed" : "started"), System.currentTimeMillis());
                userMessageZone.setText(userMessageZone.getText() + "\n" + loggingComment);
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

    // Loops every second. The thread way for Android...
    private class Chronometer implements Runnable {

        private volatile transient boolean exit = false;

        @Override
        public void run() {
            Looper.prepare(); // Mandatory for a Thread on Android.
//            Looper.loop();

            while (!this.exit) {
                String gpsData = " No GPS ";
                String sunData = " - none -";
                // Current date and time
                Calendar c = Calendar.getInstance();
//                System.out.println("Current time => " + c.getTime());
                String formattedDate = DF.format(c.getTime());
                // formattedDate have current date/time
                double latitude = -Double.MAX_VALUE;
                double longitude = -Double.MAX_VALUE;
                float sog = -1f;
                float cog = -1f;

                if (gps != null) {
                    if (gps.canGetLocation()) {

                        Location gpsLocation = gps.getLocation(instance);

                        latitude = gpsLocation.getLatitude();
                        longitude = gpsLocation.getLongitude();

                        sog = gpsLocation.getSpeed();
                        cog = gpsLocation.getBearing();

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

                            Object selectedBody = instance.bodySpinner.getSelectedItem();
                            switch (selectedBody.toString()) {
                                case "Moon":
                                    sru.setAHG(AstroComputer.getMoonGHA());
                                    sru.setD(AstroComputer.getMoonDecl());
                                    break;
                                case "Venus":
                                    sru.setAHG(AstroComputer.getVenusGHA());
                                    sru.setD(AstroComputer.getVenusDecl());
                                    break;
                                case "Mars":
                                    sru.setAHG(AstroComputer.getMarsGHA());
                                    sru.setD(AstroComputer.getMarsDecl());
                                    break;
                                case "Jupiter":
                                    sru.setAHG(AstroComputer.getJupiterGHA());
                                    sru.setD(AstroComputer.getJupiterDecl());
                                    break;
                                case "Saturn":
                                    sru.setAHG(AstroComputer.getSaturnGHA());
                                    sru.setD(AstroComputer.getSaturnDecl());
                                    break;
                                case "Sun":
                                default:
                                    sru.setAHG(AstroComputer.getSunGHA());
                                    sru.setD(AstroComputer.getSunDecl());
                                    break;
                            }

//                            sru.setAHG(AstroComputer.getSunGHA());
//                            sru.setD(AstroComputer.getSunDecl());
                            sru.calculate();
                            double obsAlt = sru.getHe();
                            double z = sru.getZ();

                            sunData = String.format("%s Data:\nElevation: %s\nZ: %.02f\272",
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

//                content = String.format("%s\n%s\n%s", formattedDate, gpsData, sunData);
//                Toast.makeText(instance, content, Toast.LENGTH_SHORT).show();
//                setText(instance.dateTimeHolder, content);
//                instance.dateTimeHolder.setText(content);

                Toast.makeText(instance, formattedDate, Toast.LENGTH_SHORT).show();
                Toast.makeText(instance, gpsData, Toast.LENGTH_SHORT).show();
                Toast.makeText(instance, sunData, Toast.LENGTH_SHORT).show();
                setText(instance.dateTimeHolder, formattedDate);
                setText(instance.gpsDataHolder, gpsData);
                setText(instance.sunDataHolder, sunData);

                if (instance.isLogging) {
                    // Log data here
                    // "epoch;fmt-date;latitude;longitude;speed;heading";
                    String dataLine = String.format(
                            "%d;%s;%f;%f;%f;%f\n",
                            c.getTimeInMillis(),
                            formattedDate.replace('\n', ' '),
                            latitude,
                            longitude,
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
        this.logButton = this.findViewById(R.id.logButton);

        this.dateTimeHolder.setText("- No date -"); // String.format("Current Date and Time :\n%s", "---"));
        this.gpsDataHolder.setText("- No GPS -"); // String.format("Current Date and Time :\n%s", "---"));
        this.sunDataHolder.setText("- No Celestial data -"); // String.format("Current Date and Time :\n%s", "---"));

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
        if (isLogging) {
            try {
                logger.flush();
                logger.close();
            } catch (IOException ioe) {
                //
            }
        }
    }
}
