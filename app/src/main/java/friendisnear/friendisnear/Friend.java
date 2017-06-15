package friendisnear.friendisnear;

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

    public Friend(String name) {
        this.name = name;
    }

    public void setLocation(Location location) {

        this.location = location;

    }

    public float getDistanceToOld() {
        float result = 0;
        if(location != null && oldLocation != null) result = location.distanceTo(oldLocation);
        oldLocation = this.location;

        return result;
    }

    public Location getLocation() {return location;}

    public String getName() {
        return name;
    }

    @Override
    public String toString() { return name + " " +  location.toString();}


}
