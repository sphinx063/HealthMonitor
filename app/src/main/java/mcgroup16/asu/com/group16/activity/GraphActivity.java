package mcgroup16.asu.com.group16.activity;

import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;

import java.util.List;

import mcgroup16.asu.com.group16.R;
import mcgroup16.asu.com.group16.model.GraphView;
import mcgroup16.asu.com.group16.model.Row;
import mcgroup16.asu.com.group16.utility.DatabaseUtil;


public class GraphActivity extends AppCompatActivity {

    private static final String TAG = GraphActivity.class.getCanonicalName();

    private GraphView runningGraphView;
    private GraphView defaultGraphView;
    private float defaultValues[];
    private float runningValuesX[];
    private float runningValuesY[];
    private float runningValuesZ[];
    private Button btnRun = null;
    private Button btnStop = null;
    private Handler postHandle = null;
    private LinearLayout graphLayout = null;

    // Database utility related declarations
    private String DB_NAME = null;
    private DatabaseUtil dbHelper = null;
    private final String TABLE_NAME = "training_table";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_develop_graph);

        DB_NAME = getIntent().getStringExtra("EXTRA_DB_NAME");

        // DB handler instance initialization
        dbHelper = new DatabaseUtil(this, DB_NAME);

        final String[] X_Labels = new String[]{"0", "50", "100", "150", "200", "250"};
        final String[] Y_Labels = new String[]{"50", "100", "150", "200", "250"};
        runningValuesX = new float[20];
        runningValuesY = new float[20];
        runningValuesZ = new float[20];
        defaultValues = new float[20];

        graphLayout = (LinearLayout) findViewById(R.id.develop_graph);
        defaultGraphView = new GraphView(getApplicationContext(), defaultValues, "Activity Monitoring UI", X_Labels, Y_Labels, GraphView.LINE);
        defaultGraphView.setBackgroundColor(getResources().getColor(android.R.color.background_dark));
        graphLayout.addView(defaultGraphView);

        btnRun = (Button) findViewById(R.id.btn_run);
        btnRun.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (runningGraphView != null) {
                    graphLayout.removeView(runningGraphView);
                }

                runningGraphView = new GraphView(getApplicationContext(), runningValuesX, "Health Monitoring UI", X_Labels, Y_Labels, GraphView.LINE);
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
    }

    private Runnable postDataOnGraphThread = new Runnable() {
        @Override
        public void run() {
            List<Row> sensordataList = dbHelper.getRows(TABLE_NAME, 3);
            for (int i = 0; i < sensordataList.size(); i++) {
                Row row = sensordataList.get(i);
                List<Double> xyzValues = row.getData();
                String activityLabel = row.getLabelActivity();
                for (int j = 0; j < xyzValues.size(); j++) {

                }
            }
            runningGraphView.setxValues(runningValuesX);
            graphLayout.removeView(runningGraphView);
            graphLayout.addView(runningGraphView);
            postHandle.postDelayed(this, 1000);
        }
    };
}
