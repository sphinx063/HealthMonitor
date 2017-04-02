package mcgroup16.asu.com.group16.activity;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import mcgroup16.asu.com.group16.R;
import mcgroup16.asu.com.group16.utility.DatabaseUtil;
public class MainActivity extends AppCompatActivity {

    private static final String DB_NAME = "McGroup16";

    private Button predictButton = null;
    private Button createDataButton = null;
    private Button graphButton = null;
    DatabaseUtil dbHelper = null;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        // TODO: remove it before final submission
        getApplicationContext().deleteDatabase(DB_NAME);

        dbHelper = new DatabaseUtil(this, DB_NAME);

        createDataButton = (Button) findViewById(R.id.btnCreateData);
        createDataButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                // starting data collect activity
                Intent moveToDataCollectActivity = new Intent(getApplicationContext(), DataCollectActivity.class);
                moveToDataCollectActivity.putExtra("EXTRA_DB_NAME", DB_NAME);
                startActivity(moveToDataCollectActivity);
            }
        });

        predictButton = (Button) findViewById(R.id.btnPredict);
        predictButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                // starting predict activity
                Intent moveToPredictActivity = new Intent(getApplicationContext(), PredictActivity.class);
                moveToPredictActivity.putExtra("EXTRA_DB_NAME", DB_NAME);
                moveToPredictActivity.putExtra("EXTRA_FROM_ACTIVITY", "Main");
                startActivity(moveToPredictActivity);
            }
        });

//        graphButton = (Button) findViewById(R.id.btn_graph);
//        graphButton.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//
//                // starting plot graph activity
//                Intent moveToGraphActivity = new Intent(getApplicationContext(), GraphActivity.class);
//                moveToGraphActivity.putExtra("EXTRA_DB_NAME", DB_NAME);
//                startActivity(moveToGraphActivity);
//
//            }
//        });
    }
}
