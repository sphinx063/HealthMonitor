package mcgroup16.asu.com.mc_group16.model;

/**
 * Created by rinku on 3/3/2017.
 */

public class Sample {
    private double x;
    private double y;
    private double z;
    private long timestamp;

    public Sample(long timestamp,double x,double y,double z){
        this.timestamp = timestamp;
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public long getTimestamp(){
        return timestamp;
    }
    public double getX(){
        return x;
    }
    public double getY(){
        return y;
    }
    public double getZ(){
        return z;
    }


}
