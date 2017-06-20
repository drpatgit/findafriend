package friendisnear.friendisnear;

import android.Manifest;
import android.content.ComponentName;
import android.content.Context;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.content.Intent;
import android.widget.ListView;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.TreeMap;

import friendisnear.friendisnear.LocationService.LocationBinder;
import friendisnear.friendisnear.utilities.CommonUtility;
import friendisnear.friendisnear.utilities.Friend;
import friendisnear.friendisnear.utilities.CommonActionLitener;

import static friendisnear.friendisnear.SettingsActivity.PREF_USER_NAME;

public class MainActivity extends AppCompatActivity implements CommonActionLitener {

    final private int REQUEST_ADD_FRIEND = 0;
    final private int REQUEST_SETTINGS = 1;
    final private int REQUEST_CODE_ASK_PERMISSIONS = 123;
    //private ArrayList<Friend> friends;

    public FriendAdapter adapter;

    private LocationService locationService;

    private Friend user;

    private Intent locationIntent;

    private CommonUtility commons;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        commons = CommonUtility.getInstance();

        commons.setMainActivity(this); //PreferenceManager.getDefaultSharedPreferences(this));

        String userName = getSharedPreferences(PREF_USER_NAME, MODE_PRIVATE).getString(PREF_USER_NAME, "");
        //friends = new ArrayList<>();
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        ListView listView = (ListView) findViewById(R.id.listView);
        adapter = new FriendAdapter(this);
        listView.setAdapter(adapter);

        //friends = commons.getFriends();

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent i = new Intent(MainActivity.this, FriendActivity.class);
                startActivity(i);
                //startActivityForResult(i, REQUEST_ADD_FRIEND);
            }
        });

        commons.addCommonActionListener(this);
    }

    @Override
    protected void onStart() {
        super.onStart();

        if(checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED || checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, REQUEST_CODE_ASK_PERMISSIONS);
        }

        if(locationIntent==null){
            locationIntent = new Intent(this, LocationService.class);
            bindService(locationIntent, locationConnection, Context.BIND_AUTO_CREATE);
            startService(locationIntent);

        }
    }

    @Override
    protected void onDestroy() {
        locationService.stopSelf();
        //locationService.stopLocationService();
        commons.saveFriendsToFile();
        PreferenceManager.getDefaultSharedPreferences(MainActivity.this).unregisterOnSharedPreferenceChangeListener(commons);
        commons.removeFriendsChangedListener(this);
        commons = null;
        unbindService(locationConnection);
        super.onDestroy();
    }

    //connect to the service
    private ServiceConnection locationConnection = new ServiceConnection(){

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            LocationBinder binder = (LocationBinder)service;
            //get service

            locationService = binder.getService();
            locationService.initStartService();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {

        }
    };

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            Intent i = new Intent(MainActivity.this, SettingsActivity.class);
            //startActivityForResult(i, REQUEST_SETTINGS);
            startActivity(i);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    public void updateLocation(View view) {
        if(locationService != null) locationService.updateLocation();
        adapter.notifyDataSetChanged();

    }

    /*public void openSettings(View view) {
        Intent i = new Intent(MainActivity.this, SettingsActivity.class);
        startActivity(i);
    }*/

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data){
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {

            case REQUEST_SETTINGS:

                break;
        }
    }





    @Override
    public void onCommonAction(final Friend f, final CommonUtility.CommonAction action) {
        if(adapter == null) return;
        final View view = this.findViewById(android.R.id.content);
        runOnUiThread(new Runnable() {

            @Override
            public void run() {
                switch (action) {
                    case FRIEND_ADDED:
                        Snackbar.make(view, f.getName() + " added successfully to friendlist!", Snackbar.LENGTH_LONG).setAction("Action", null).show();
                        adapter.updateFriends();
                        adapter.notifyDataSetChanged();
                        break;
                    case FRIEND_ADD_FAILED:
                        Snackbar.make(view, f.getName() + " already in your friendlist!", Snackbar.LENGTH_LONG).setAction("Action", null).show();
                        break;
                    case FRIEND_REMOVED:
                        Snackbar.make(view, f.getName() + " removed successfully from friendlist!", Snackbar.LENGTH_LONG).setAction("Action", null).show();
                        adapter.updateFriends();
                        adapter.notifyDataSetChanged();
                        break;
                    case FRIEND_REMOVE_FAILED:
                        Snackbar.make(view, f.getName() + " already in your friendlist!", Snackbar.LENGTH_LONG).setAction("Action", null).show();
                        break;
                    case FRIEND_LOCATION_CHANGED:
                    case USER_LOCATION_CHANGED:
                        adapter.notifyDataSetChanged();
                        break;
                    case FRIEND_STAT_CHANGED:
                        break;
                    case USERNAME_CHANGED:
                        adapter.updateUser();
                        break;
                    case FRIEND_REQUEST:
                        Snackbar.make(view, f.getName() + " friend request sent", Snackbar.LENGTH_LONG).setAction("Action", null).show();
                        break;
                    case APPOINTMENT_REQUEST:
                        Snackbar.make(view, f.getName() + " appointment request sent", Snackbar.LENGTH_LONG).setAction("Action", null).show();
                        break;
                }
            }
        });

    }

}
