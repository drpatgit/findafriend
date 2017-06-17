package friendisnear.friendisnear;

import android.Manifest;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.support.v4.content.ContextCompat;

import org.eclipse.paho.client.mqttv3.MqttException;

import java.util.Map;

import friendisnear.friendisnear.utilities.CommonUtility;
import friendisnear.friendisnear.utilities.Friend;
import friendisnear.friendisnear.utilities.CommonActionLitener;
import friendisnear.friendisnear.utilities.ProtoMessager;

public class LocationService extends Service implements CommonActionLitener {

    public static int TIME_MILLIS = 1000;
    public static int LOCATION_REFRESH_TIME = 1000*10;
    public static int LOCATION_REFRESH_DISTANCE = 5;

    private Handler handler;

    private LocationBinder mLocationBinder = new LocationBinder();
    private Location mLastLocation;
    private LocationManager mLocationManager;

    private Context thisActivityContext;

    private CommonUtility commons;
    private Map<String,Friend> friends;

    private int sync_time;

    private ProtoMessager protomessager;

    public LocationService() {
        super();
        commons = CommonUtility.getInstance();
        friends = commons.getFriends();
        //commons.setLocationService(this);
        commons.addCommonActionListener(this);
        protomessager = new ProtoMessager();
    }

    public void initStartService() {
        thisActivityContext = getApplicationContext();
        sync_time = commons.getSyncTime();
        if(ContextCompat.checkSelfPermission(thisActivityContext,Manifest.permission.ACCESS_FINE_LOCATION)== PackageManager.PERMISSION_GRANTED || ContextCompat.checkSelfPermission(thisActivityContext,Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {

            mLocationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
            handler = new Handler();
            //timer.schedule(locationRequestTask, 5*60*1000);
            handler.post(locationRequest);

            //mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, LOCATION_REFRESH_TIME,
            //        LOCATION_REFRESH_DISTANCE, mLocationListener);
        }
        //friends = new ArrayList<>();
        mLastLocation = mLocationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);

    }

    public void setSyncTime(int sync_time) { this.sync_time = sync_time; }

    private Runnable locationRequest = new Runnable() {

        @Override
        public void run() {
            if(ContextCompat.checkSelfPermission(thisActivityContext,Manifest.permission.ACCESS_FINE_LOCATION)== PackageManager.PERMISSION_GRANTED || ContextCompat.checkSelfPermission(thisActivityContext,Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {

                if(mLocationManager != null) {
                    mLocationManager.requestSingleUpdate(LocationManager.GPS_PROVIDER, mLocationListener,null);
                }
            }
            handler.postDelayed(locationRequest, sync_time);
        }
    };

    private LocationListener mLocationListener = new LocationListener() {
        @Override
        public void onLocationChanged(Location location) {
            System.out.println("onLocationChanged");
            mLastLocation = location;
            updateLocation();
        }

        @Override
        public void onStatusChanged(String s, int i, Bundle bundle) {
            System.out.println("onStatusChanged");
        }

        @Override
        public void onProviderEnabled(String s) {
            System.out.println("onProviderEnabled");
        }

        @Override
        public void onProviderDisabled(String s) {
            System.out.println("onProviderDisabled");
        }
    };

    @Override
    public IBinder onBind(Intent intent) {
        return mLocationBinder;
        //return null;
        //throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public void onCommonAction(Friend f, CommonUtility.CommonAction action) {
        switch(action) {
            case SYNC_TIME_CHANGED:
                sync_time = commons.getSyncTime();
                break;
        }
    }

    public class LocationBinder extends Binder {
        LocationService getService() {return LocationService.this;}
    }

    public void updateLocation() {
        try {
            protomessager.sendLocation(mLastLocation);
        } catch (MqttException e) {
            e.printStackTrace();
        }
        commons.updateUserLocation(mLastLocation);
        //if(friends == null) return;
        //for(int i = 0; i < friends.size(); i++) friends.get(i).setLocation(mLastLocation);
    }

}
