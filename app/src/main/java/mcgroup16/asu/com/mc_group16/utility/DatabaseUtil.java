package mcgroup16.asu.com.mc_group16.utility;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/**
 * Created by Siddharth on 2/21/2017.
 */

public class DatabaseUtil extends SQLiteOpenHelper {

    private static final int DATABASE_VERSION = 1;
    private String TABLE_NAME = null;
    public DatabaseUtil(Context context,final String DATABASE_NAME){
        super(context,DATABASE_NAME,null,DATABASE_VERSION);
        //DatabaseUtil.TABLE_NAME = TABLE_NAME;
    }
    @Override
    public void onCreate(SQLiteDatabase db) {
        //db.execSQL("DROP TABLE IF EXISTS '" + TABLE_NAME + "'");
        /*String CREATE_TABLE_SQL = "CREATE TABLE IF NOT EXISTS "
                + TABLE_NAME
                + " ( "
                + " timestamp TEXT, "
                + " x_val REAL, "
                + " y_val REAL, "
                + " z_val REAL"
                + " );";
        db.execSQL(CREATE_TABLE_SQL);
        System.out.println("Table Created");*/
    }
    public void createTable(String TABLE_NAME){
        this.TABLE_NAME = TABLE_NAME;
        String CREATE_TABLE_SQL = "CREATE TABLE IF NOT EXISTS "
                + TABLE_NAME
                + " ( "
                + " timestamp TEXT, "
                + " x_val REAL, "
                + " y_val REAL, "
                + " z_val REAL"
                + " );";
        this.getWritableDatabase().execSQL(CREATE_TABLE_SQL);
        System.out.println("+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++Table Created");
    }
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS "+TABLE_NAME);
        this.onCreate(db);
    }
    public void addSample(double[] sensorData,String TABLE_NAME){
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("timestamp",String.valueOf(sensorData[3]));
        values.put("x_val",sensorData[0]);
        values.put("y_val",sensorData[1]);
        values.put("z_val",sensorData[2]);
        db.insert(TABLE_NAME,null,values);
        db.close();
    }
    public void getAllSamples(String TABLE_NAME){
        String query = "SELECT * FROM "+TABLE_NAME;
        SQLiteDatabase db = this.getWritableDatabase();
        Cursor cursor = db.rawQuery(query,null);
        cursor.moveToFirst();
        String timestamp = cursor.getString(0);
        double x = cursor.getDouble(1);
        double y = cursor.getDouble(2);
        double z = cursor.getDouble(3);
        System.out.println("Timestamp index: = "+cursor.getColumnIndex("timestamp"));
        System.out.println("TIMESTAMP:= "+timestamp);
        System.out.println("X:= "+x);
        System.out.println("Y:= "+y);
        System.out.println("Z:= "+z);
    }
}
