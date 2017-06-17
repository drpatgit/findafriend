package friendisnear.friendisnear.utilities;

import android.location.Location;
import android.os.Parcelable;

import java.io.Serializable;
import java.io.StringReader;

/**
 * Created by stieblj on 14.06.2017.
 */

public class Friend implements Serializable {
    private String name;
    private Location location;
    private Location oldLocation;
    private boolean avaliable;

    public Friend(String name) {
        this.name = name;
    }

    public void setLocation(Location location) {

        this.location = location;

    }

    public float getDistanceTo(Location l) {
        if(l != null && location != null) return location.distanceTo(l);
        return -1;
    }

    public float getDistanceTo(Friend f) {
        if(f != null && f.getLocation() != null && location != null) return location.distanceTo(f.getLocation());
        return -1;
    }

    public float getDistanceToOld() {
        float result = getDistanceTo(oldLocation);
        oldLocation = this.location;

        return result;
    }

    public Location getLocation() {return location;}

    public String getName() {
        return name;
    }

    public boolean getAvaliable() {return avaliable;}

    public void setAvaliable(boolean avaliable) {this.avaliable = avaliable;}

    @Override
    public String toString() { return name + " " +  location.toString();}


}
