package mcgroup16.asu.com.mc_group16.model;

import java.util.ArrayList;

/**
 * Created by rinku on 3/14/2017.
 */

public class Row {
    private ArrayList<Double> data = null;
    private String activity;

    public Row(ArrayList<Double> data, String activity) {
        this.data = data;
        this.activity = activity;
    }

    public ArrayList<Double> getData() {
        return data;
    }

    public String getActivity() {
        return activity;
    }
}
