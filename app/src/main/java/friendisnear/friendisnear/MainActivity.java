package friendisnear.friendisnear;

import android.Manifest;
import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.IBinder;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.content.Intent;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import friendisnear.friendisnear.LocationService.LocationBinder;

import java.util.ArrayList;
import java.util.LinkedList;

public class MainActivity extends AppCompatActivity {

    final private int REQUEST_ADD_FRIEND = 0;
    final private int REQUEST_SETTINGS = 1;
    final private int REQUEST_CODE_ASK_PERMISSIONS = 123;

    public ArrayList<Friend> friends;
    public FriendAdapter adapter;
    private LocationService locationService;

    private Friend user;

    private Intent locationIntent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        friends = new ArrayList<>();
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        ListView listView = (ListView) findViewById(R.id.listView);
        adapter = new FriendAdapter(this, friends);
        listView.setAdapter(adapter);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent i = new Intent(MainActivity.this, FriendActivity.class);
                startActivityForResult(i, REQUEST_ADD_FRIEND);
            }
        });

    }

    @Override
    protected void onStart() {
        super.onStart();

        if(checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_CODE_ASK_PERMISSIONS);
        }

        if(locationIntent==null){
            locationIntent = new Intent(this, LocationService.class);
            bindService(locationIntent, locationConnection, Context.BIND_AUTO_CREATE);
            startService(locationIntent);
        }
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
            startActivityForResult(i, REQUEST_SETTINGS);
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
            case REQUEST_ADD_FRIEND:
                if(resultCode == Activity.RESULT_OK){
                    Friend f = (Friend)data.getSerializableExtra("result");
                    //String friend = data.getStringExtra("result");
                    if(friends.contains(f.getName())){
                        Snackbar.make(this.findViewById(android.R.id.content), "Friend already in your friendlist!", Snackbar.LENGTH_LONG).setAction("Action", null).show();
                        return;
                    }

                    friends.add(f);
                    if(locationService != null) locationService.setUpdateListener(f);
                    adapter.notifyDataSetChanged();
                    Snackbar.make(this.findViewById(android.R.id.content), "Friend added successfully!", Snackbar.LENGTH_LONG).setAction("Action", null).show();
                    return;
                }
                if(resultCode == Activity.RESULT_CANCELED){

                }
                break;
            case REQUEST_SETTINGS:

                break;
        }
    }
}
