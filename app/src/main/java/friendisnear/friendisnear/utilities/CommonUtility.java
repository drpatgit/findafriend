package friendisnear.friendisnear.utilities;

import android.content.SharedPreferences;
import android.location.Location;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import friendisnear.friendisnear.LocationService;
import friendisnear.friendisnear.SettingsActivity;

/**
 * Created by stieblj on 17.06.2017.
 */

public class CommonUtility implements SharedPreferences.OnSharedPreferenceChangeListener {

    public enum FriendAction {
        ADDED, ADD_FAILED, REMOVED, REMOVE_FAILED, STAT_CHANGED, LOCATION_CHANGED, USERNAME_CHANGED;
    }

    private static final CommonUtility ourInstance = new CommonUtility();

    private ArrayList<FriendsChangedListener> friendsListeners;
    private ArrayList<Friend> friends;
    private Friend user;
    private SharedPreferences preferences;
    private LocationService locationService;

    public static CommonUtility getInstance() {
        return ourInstance;
    }

    private CommonUtility() {
        friends = new ArrayList<>();
        friendsListeners = new ArrayList<>();
    }

    public Friend getUser() { return user; }

    public void addFriend(Friend friend) {
        FriendAction action = FriendAction.ADD_FAILED;
        if(!friends.contains(friend)) {
            friends.add(friend);
            action = FriendAction.ADDED;
        }
        fireFriendsChanged(friend, action);
    }

    public void removeFriend(Friend friend) {
        FriendAction action = FriendAction.REMOVE_FAILED;
        if(friends.contains(friend)) {
            friends.remove(friend);
            action = FriendAction.REMOVED;
        }
        fireFriendsChanged(friend, action);
    }

    public List<Friend> getFriends() {return Collections.unmodifiableList(friends);}

    public void addFriendsChangedListener(FriendsChangedListener listener) {
        friendsListeners.add(listener);
    }

    public void removeFriendsChangedListener(FriendsChangedListener listener) {
        friendsListeners.remove(listener);
    }

    public void fireFriendsChanged(final Friend friend, final FriendAction action) {
        for(int i = 0; i < friendsListeners.size(); i++) friendsListeners.get(i).onFriendsChanged(friend, action);
    }

    public void updateLocation(Location location) {
        if(user != null) {
            user.setLocation(location);
            fireFriendsChanged(user, FriendAction.LOCATION_CHANGED);
        }
    }

    public void setSharedPreferences(SharedPreferences preferences) {
        this.preferences = preferences;
        preferences.registerOnSharedPreferenceChangeListener(this);
        String userName = preferences.getString(SettingsActivity.PREF_USER_NAME,"null");
        if(userName != null) user = new Friend(userName);
    }

    public int getSyncTime() {
        if(preferences != null) {
            return Integer.parseInt(preferences.getString(SettingsActivity.SYNC_FREQUENCY, "null")) * 1000;
        }

        return 0;
    }

    public void setLocationService(LocationService locationService) { this.locationService = locationService; }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        switch (key) {
            case SettingsActivity.SYNC_FREQUENCY:
                if(locationService != null) {
                    locationService.setSyncTime(getSyncTime());
                }
                break;
            case SettingsActivity.PREF_USER_NAME:
                Friend newUser = new Friend(sharedPreferences.getString(key, "null"));
                if(user != null) newUser.setLocation(user.getLocation());
                user = newUser;
                fireFriendsChanged(user, FriendAction.USERNAME_CHANGED);
                break;
        }
    }


}
