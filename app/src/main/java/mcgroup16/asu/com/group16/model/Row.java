package mcgroup16.asu.com.group16.model;

import java.util.ArrayList;

/**
 * Created by rinku on 3/14/2017.
 */

public class Row {
    private ArrayList<Double> data = null;
    private String labelActivity;

    public Row(ArrayList<Double> data, String labelActivity) {
        this.data = data;
        this.labelActivity = labelActivity;
    }

    public ArrayList<Double> getData() {
        return data;
    }

    public String getLabelActivity() {
        return labelActivity;
    }
}
