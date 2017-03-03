package mcgroup16.asu.com.mc_group16;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import mcgroup16.asu.com.mc_group16.service.AccelerometerService;
import mcgroup16.asu.com.mc_group16.utility.DatabaseUtil;


public class DevelopGraph extends AppCompatActivity implements SensorEventListener {

    private GraphView runningGraphView;
    private GraphView defaultGraphView;
    private boolean hasRunPressed = false;
    private float defaultValues[];
    private float runningValues[];
    private Button btnRun = null;
    private Button btnStop = null;
    private Handler handle = null;
    private LinearLayout devGraph = null;

    private String DB_NAME = null;
    private String TABLE_NAME = null;
    private DatabaseUtil db = null;
    private SensorManager sensorManager = null;
    private Sensor accelerometer = null;

    private double[] sensorData = {5.77,9.32,1.7,5555};
    private final SensorEventListener sensorEventListener = new SensorEventListener() {
        @Override
        public void onSensorChanged(SensorEvent event) {
            sensorData[0] = event.values[0];
            sensorData[1] = event.values[1];
            sensorData[2] = event.values[2];
            sensorData[3] = event.values[3];
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {

        }
    };
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,WindowManager.LayoutParams.FLAG_FULLSCREEN);

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_develop_graph);
        String patientName = getIntent().getStringExtra("EXTRA_PATIENT_NAME");
        String patientAge = getIntent().getStringExtra("EXTRA_PATIENT_AGE");
        DB_NAME = getIntent().getStringExtra("EXTRA_DB_NAME");
        TABLE_NAME = getIntent().getStringExtra("EXTRA_TABLE_NAME");
        db = new DatabaseUtil(this,DB_NAME);
        db.addSample(sensorData,TABLE_NAME);
        db.getAllSamples(TABLE_NAME);
        TextView txtPatientName = (TextView) findViewById(R.id.txtPatientName);
        TextView txtPatientAge = (TextView) findViewById(R.id.txtPatientAge);
        txtPatientName.setText(patientName);
        txtPatientAge.setText(patientAge);

        final String[] X_Labels = new String[]{"0", "50", "100", "150", "200", "250"};
        final String[] Y_Labels = new String[]{"50", "100", "150", "200", "250"};
        runningValues = new float[20];
        defaultValues = new float[20];

        devGraph = (LinearLayout) findViewById(R.id.develop_graph);
        defaultGraphView = new GraphView(getApplicationContext(), defaultValues, "Health Monitoring UI", X_Labels, Y_Labels, GraphView.LINE);
        defaultGraphView.setBackgroundColor(getResources().getColor(android.R.color.background_dark));
        devGraph.addView(defaultGraphView);


        btnStop = (Button) findViewById(R.id.btn_stop);
        btnStop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                devGraph.removeView(runningGraphView);
                devGraph.removeView(defaultGraphView);
                defaultGraphView = new GraphView(getApplicationContext(), defaultValues, "Health Monitoring UI", X_Labels, Y_Labels, GraphView.LINE);
                defaultGraphView.setBackgroundColor(getResources().getColor(android.R.color.background_dark));
                devGraph.addView(defaultGraphView);
            }
        });

        btnRun = (Button) findViewById(R.id.btn_run);
        btnRun.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (runningGraphView != null) {
                    devGraph.removeView(runningGraphView);
                }

                //readFromDatabase();
                runningGraphView = new GraphView(getApplicationContext(), runningValues, "Health Monitoring UI", X_Labels, Y_Labels, GraphView.LINE);
                runningGraphView.setBackgroundColor(getResources().getColor(android.R.color.background_dark));

                if (devGraph != null && defaultGraphView != null) {
                    devGraph.removeView(defaultGraphView);
                }
                devGraph.addView(runningGraphView);

                if (!hasRunPressed) {
                    handle = new Handler();
                    hasRunPressed = true;
                    handle.post(postUpdate);
                }
            }
        });

    }
    private void initAccelerometer(){
        sensorManager  = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        sensorData = new double[4];
        if(accelerometer!=null){
            sensorManager.registerListener(sensorEventListener,accelerometer,SensorManager.SENSOR_DELAY_NORMAL);
        }
    }
    @Override
    public void onResume(){
        sensorManager.registerListener(sensorEventListener,accelerometer,SensorManager.SENSOR_DELAY_NORMAL);
    }
    @Override
    public void onPause(){
        sensorManager.unregisterListener(sensorEventListener,accelerometer);
    }
    public void writeToDatabase(double[] data){
//        String insertQuery = "INSERT INTO "+TABLE_NAME
//                +" VALUES("
//                +sensorData[3] +","
//                +sensorData[0] +","
//                +sensorData[1] +","
//                +sensorData[2]+");";
        //db.execSQL(insertQuery);
        //ContentValues values = new ContentValues();
        //values.put("timestamp");

    }
    public void readFromDatabase(){
//        Cursor cursor = db.rawQuery("SELECT * FROM "+TABLE_NAME,null);
//        cursor.moveToFirst();
//        String timestamp = cursor.getString(0);
//        String x_val = cursor.getString(1);
//        String y_val = cursor.getString(2);
//        String z_val = cursor.getString(3);
//        Log.d("X value = ",x_val);
//        Log.d("Y value = ",y_val);
//        Log.d("Z value = ",z_val);
//        Log.d("timestamp value = ",timestamp);
//        System.out.println("X_Y_Z Values: " + x_val +", " + y_val + "," + z_val);
    }
    private void getAccelerometer(SensorEvent event) {

        float[] values = event.values;

        // Movement
        float x = values[0];
        float y = values[1];
        float z = values[2];
    }

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {

    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }

    // This method is generating random values for plotting
    // It keeps on updating the values displayed on the graph
    private Runnable postUpdate = new Runnable() {
        @Override
        public void run() {
            for (int i = 0; i < runningValues.length; i++) {
                runningValues[i] = (float) 0 + (int) (Math.random() * 5);
            }
            runningGraphView.setValues(runningValues);
            devGraph.removeView(runningGraphView);
            devGraph.addView(runningGraphView);
            handle.postDelayed(this, 500);
        }
    };
}
