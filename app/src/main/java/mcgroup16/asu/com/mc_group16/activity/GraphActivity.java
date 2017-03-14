package mcgroup16.asu.com.mc_group16.activity;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.List;

import mcgroup16.asu.com.mc_group16.R;
import mcgroup16.asu.com.mc_group16.model.GraphView;
import mcgroup16.asu.com.mc_group16.model.Sample;
import mcgroup16.asu.com.mc_group16.task.UploadDatabaseTask;
import mcgroup16.asu.com.mc_group16.utility.DatabaseUtil;


public class GraphActivity extends AppCompatActivity implements SensorEventListener {

//    private static final String TAG = GraphActivity.class.getCanonicalName();
    private static final String SERVER_URI = "https://impact.asu.edu/CSE535Spring17Folder/UploadToServer.php";

    private GraphView runningGraphView;
    private GraphView defaultGraphView;
    private float defaultValues[];
    private float runningValues[];
    private Button btnRun = null;
    private Button btnStop = null;
    private Button btnUpload = null;
    private Button btnDownload = null;
    private Handler postHandle = null;
    private Handler insertHandle = null;
    private LinearLayout graphLayout = null;
    private String databaseUri = null;

    // Database utility related declarations
    private String DB_NAME = null;
    private String TABLE_NAME = null;
    private DatabaseUtil dbHelper = null;
    private SensorManager sensorManager = null;
    private Sensor accelerometer = null;
    private double[] sensorData = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_develop_graph);
        initiateAccelerometer();

        String patientName = getIntent().getStringExtra("EXTRA_PATIENT_NAME");
        String patientAge = getIntent().getStringExtra("EXTRA_PATIENT_AGE");
        DB_NAME = getIntent().getStringExtra("EXTRA_DB_NAME");
        TABLE_NAME = getIntent().getStringExtra("EXTRA_TABLE_NAME");

        // DB handler instance initialization
        dbHelper = new DatabaseUtil(this, DB_NAME);

        TextView txtPatientName = (TextView) findViewById(R.id.txtPatientName);
        TextView txtPatientAge = (TextView) findViewById(R.id.txtPatientAge);
        txtPatientName.setText(patientName);
        txtPatientAge.setText(patientAge);

        final String[] X_Labels = new String[]{"0", "50", "100", "150", "200", "250"};
        final String[] Y_Labels = new String[]{"50", "100", "150", "200", "250"};
        runningValues = new float[20];
        defaultValues = new float[20];

        graphLayout = (LinearLayout) findViewById(R.id.develop_graph);
        defaultGraphView = new GraphView(getApplicationContext(), defaultValues, "Health Monitoring UI", X_Labels, Y_Labels, GraphView.LINE);
        defaultGraphView.setBackgroundColor(getResources().getColor(android.R.color.background_dark));
        graphLayout.addView(defaultGraphView);

        insertHandle = new Handler();
        insertHandle.post(insertDataIntoDBThread);

        btnRun = (Button) findViewById(R.id.btn_run);
        btnRun.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (runningGraphView != null) {
                    graphLayout.removeView(runningGraphView);
                }

                runningGraphView = new GraphView(getApplicationContext(), runningValues, "Health Monitoring UI", X_Labels, Y_Labels, GraphView.LINE);
                runningGraphView.setBackgroundColor(getResources().getColor(android.R.color.background_dark));

                if (graphLayout != null && defaultGraphView != null) {
                    graphLayout.removeView(defaultGraphView);
                }
                graphLayout.addView(runningGraphView);
                postHandle = new Handler();
                postHandle.post(postDataOnGraphThread);
            }
        });

        btnStop = (Button) findViewById(R.id.btn_stop);
        btnStop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                graphLayout.removeView(runningGraphView);
                graphLayout.removeView(defaultGraphView);
                defaultGraphView = new GraphView(getApplicationContext(), defaultValues, "Health Monitoring UI", X_Labels, Y_Labels, GraphView.LINE);
                defaultGraphView.setBackgroundColor(getResources().getColor(android.R.color.background_dark));
                graphLayout.addView(defaultGraphView);
            }
        });

        btnUpload = (Button) findViewById(R.id.btn_upload);
        btnUpload.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                UploadDatabaseTask uploadTask = new UploadDatabaseTask(GraphActivity.this, SERVER_URI, DB_NAME);
                databaseUri = getApplicationContext().getDatabasePath(DB_NAME).getPath();
                uploadTask.execute(databaseUri);
            }
        });

//        btnDownload = (Button) findViewById(R.id.btn_download);
//        btnDownload.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//
//            }
//        });

    }

    private void initiateAccelerometer() {
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        sensorData = new double[4];
        if (accelerometer != null) {
            sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL);
        }
    }

    private Runnable insertDataIntoDBThread = new Runnable() {
        @Override
        public void run() {
            long timestamp = (long) sensorData[0];
            Sample sample = new Sample(timestamp, sensorData[1], sensorData[2], sensorData[3]);
            dbHelper.addSampleToDB(sample, TABLE_NAME);
            insertHandle.postDelayed(this, 1000);
        }
    };

    private Runnable postDataOnGraphThread = new Runnable() {
        @Override
        public void run() {
            List<Sample> latestSensorSamples = dbHelper.getSamplesFromDB(TABLE_NAME, 20);
            for (int i = 0; i < latestSensorSamples.size(); i++) {
                Sample sample = latestSensorSamples.get(i);
                float plotData = (float) (sample.getX() * sample.getY() * sample.getZ());
                runningValues[i] = plotData;
            }
            runningGraphView.setValues(runningValues);
            graphLayout.removeView(runningGraphView);
            graphLayout.addView(runningGraphView);
            postHandle.postDelayed(this, 1000);
        }
    };

    @Override
    public void onSensorChanged(SensorEvent event) {
        sensorData[0] = event.values[0];
        sensorData[1] = event.values[1];
        sensorData[2] = event.values[2];
        sensorData[3] = event.timestamp;
    }

    @Override
    public void onResume() {
        super.onResume();
        sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL);
    }

    @Override
    public void onPause() {
        super.onPause();
        sensorManager.unregisterListener(this, accelerometer);
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }
}
