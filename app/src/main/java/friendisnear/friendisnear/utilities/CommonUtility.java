package friendisnear.friendisnear.utilities;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.location.Location;
import android.preference.PreferenceManager;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

import friendisnear.friendisnear.MainActivity;
import friendisnear.friendisnear.SettingsActivity;

/**
 * Created by stieblj on 17.06.2017.
 */

public class CommonUtility implements SharedPreferences.OnSharedPreferenceChangeListener {



    public enum CommonAction {
        FRIEND_ADDED, FRIEND_ADD_FAILED, FRIEND_REMOVED, FRIEND_REMOVE_FAILED, FRIEND_STAT_CHANGED, FRIEND_LOCATION_CHANGED, USER_LOCATION_CHANGED, USERNAME_CHANGED, SYNC_TIME_CHANGED,
        FRIEND_REQUEST, FRIEND_REQUEST_ACCEPT, FRIEND_REQUEST_DECLINE, APPOINTMENT_REQUEST, APPOINTMENT_REQUEST_ACCEPT, APPOINTMENT_REQUEST_DECLINE;
    }

    private static final CommonUtility ourInstance = new CommonUtility();

    private ArrayList<CommonActionLitener> friendsListeners;
    private HashMap<String,Friend> friends;
    private Friend user;
    private MainActivity mainActivity;
    private SharedPreferences preferences;
    private float alertDistance;
    //private LocationService locationService;

    public static CommonUtility getInstance() {
        return ourInstance;
    }

    private CommonUtility() {
        friendsListeners = new ArrayList<>();
    }

    public Friend getUser() { return user; }

    public void addFriend(Friend friend) {
        CommonAction action = CommonAction.FRIEND_ADD_FAILED;
        if(!friends.containsKey(friend.getName())) {
            friends.put(friend.getName(), friend);
            action = CommonAction.FRIEND_ADDED;
        }
        fireChangedEvent(friend, action);
    }

    public void removeFriend(Friend friend) {
        CommonAction action = CommonAction.FRIEND_REMOVE_FAILED;
        if(friends.containsKey(friend.getName())) {
            friends.remove(friend.getName());
            action = CommonAction.FRIEND_REMOVED;
        }
        fireChangedEvent(friend, action);
    }

    public Map<String,Friend> getFriends() {return Collections.unmodifiableMap(friends);}

    public void addCommonActionListener(CommonActionLitener listener) {
        friendsListeners.add(listener);
    }

    public void removeFriendsChangedListener(CommonActionLitener listener) {
        friendsListeners.remove(listener);
    }

    private void fireChangedEvent(final Friend friend, final CommonAction action) {
        for(int i = 0; i < friendsListeners.size(); i++) friendsListeners.get(i).onCommonAction(friend, action);
    }

    public void updateUserLocation(Location location) {
        if(user != null) {
            if(location != null) user.setLocation(location);
            fireChangedEvent(user, CommonAction.USER_LOCATION_CHANGED);
        }
    }

    public void updateLocation(String topic, Location location) {
        String friendName = topic.substring(Friend.TOPIC_PREFIX.length());
        Friend f = friends.get(friendName);
        if(f!= null) {
            f.setLocation(location);
            fireChangedEvent(f, CommonAction.FRIEND_LOCATION_CHANGED);
        }
    }

    public void setMainActivity(MainActivity mainActivity) {
        this.mainActivity = mainActivity;
        setSharedPreferences();
        loadFriendsFromFile();
    }

    private void setSharedPreferences() {
        preferences = PreferenceManager.getDefaultSharedPreferences(mainActivity);
        preferences.registerOnSharedPreferenceChangeListener(this);
        String userName = preferences.getString(SettingsActivity.PREF_USER_NAME,"Mustermann");
        if(userName != null) {
            user = new Friend(userName);
            fireChangedEvent(user, CommonAction.USERNAME_CHANGED);
        }
    }

    public int getSyncTime() {
        if(preferences != null) {
            String value = preferences.getString(SettingsActivity.PREF_SYNC_FREQUENCY, "60");

            return Integer.parseInt(value) * 1000;
        }

        return 0;
    }

    public void requestReceived(long timestamp, final String sender, final int request) {
        mainActivity.runOnUiThread(new Runnable() {

            @Override
            public void run() {
                AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(mainActivity);

                final Friend f = new Friend(sender);

                switch(request) {
                    case ProtoMessager.REQUEST_FRIEND:
                        dialogBuilder.setTitle("Friend Request");
                        dialogBuilder.setMessage(sender + " would like to add you to the friendlist.");
                        dialogBuilder.setPositiveButton("Accept", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                fireChangedEvent(f, CommonAction.FRIEND_REQUEST_ACCEPT);
                                addFriend(f);
                            }
                        });

                        dialogBuilder.setNegativeButton("Decline", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                fireChangedEvent(f, CommonAction.FRIEND_REQUEST_DECLINE);
                            }
                        });
                        break;
                    case ProtoMessager.REQUEST_ACCEPT_FRIEND:
                        dialogBuilder.setTitle("Friend Request");
                        dialogBuilder.setMessage(sender + " accepted your friend request.");
                        dialogBuilder.setNeutralButton("OK", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                addFriend(f);
                            }
                        });
                        break;
                    case ProtoMessager.REQUEST_DECLINE_FRIEND:
                        dialogBuilder.setTitle("Friend Request");
                        dialogBuilder.setMessage(sender + " declined your friend request.");
                        dialogBuilder.setNeutralButton("OK", null);
                        break;

                    case ProtoMessager.REQUEST_APPOINTMENT:
                        dialogBuilder.setTitle("Appointment Request");
                        dialogBuilder.setMessage(sender + " would like to meet with you.");
                        dialogBuilder.setPositiveButton("Accept", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                fireChangedEvent(f, CommonAction.APPOINTMENT_REQUEST_ACCEPT);
                            }
                        });

                        dialogBuilder.setNegativeButton("Decline", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                fireChangedEvent(f, CommonAction.APPOINTMENT_REQUEST_DECLINE);
                            }
                        });
                        break;

                    case ProtoMessager.REQUEST_ACCEPT_APPOINTMENT:
                        dialogBuilder.setTitle("Appointment Request");
                        dialogBuilder.setMessage(sender + " acceped your appointment request.");
                        dialogBuilder.setNeutralButton("OK", null);
                        break;

                    case ProtoMessager.REQUEST_DECLINE_APPOINTMENT:
                        dialogBuilder.setTitle("Appointment Request");
                        dialogBuilder.setMessage(sender + " declined your appointment request.");
                        dialogBuilder.setNeutralButton("OK", null);
                        break;
                }
                dialogBuilder.create().show();
            }
        });

    }

    public void request(Friend f, CommonUtility.CommonAction action) {
        switch(action) {
            case FRIEND_REQUEST:
            case APPOINTMENT_REQUEST:
                fireChangedEvent(f, action);
                break;
        }

    }

    public void messageReceived(long timestamp, String sender, String text) {

    }

    public float getAlertDistance() {return alertDistance; }


    private void loadFriendsFromFile() {
        friends = new HashMap<>();
        if(true) return;
        File file = new File(mainActivity.getFilesDir(), "friendlist");

        try {
            FileInputStream f = new FileInputStream(file);
            ObjectInputStream s = new ObjectInputStream(f);
            friends = (HashMap<String,Friend>)s.readObject();
            s.close();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        if (friends == null) {
            friends = new HashMap<>();
        }
    }

    public void saveFriendsToFile() {
        File file = new File(mainActivity.getFilesDir(), "friendlist");

        try {
            FileOutputStream f = new FileOutputStream(file);
            ObjectOutputStream s = new ObjectOutputStream(f);

            s.writeObject(friends);
            s.flush();
            s.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        switch (key) {
            case SettingsActivity.PREF_SYNC_FREQUENCY:
                fireChangedEvent(null, CommonAction.SYNC_TIME_CHANGED);
                break;
            case SettingsActivity.PREF_USER_NAME:
                Friend newUser = new Friend(sharedPreferences.getString(key, "null"));
                if(user != null) newUser.setLocation(user.getLocation());
                user = newUser;
                fireChangedEvent(user, CommonAction.USERNAME_CHANGED);
                break;
            case SettingsActivity.PREF_ALERT_DISTANCE:
                try{
                    alertDistance = Float.valueOf(sharedPreferences.getString(key, "0"));
                } catch(NumberFormatException e) {
                    e.printStackTrace();
                }

        }
    }


}
