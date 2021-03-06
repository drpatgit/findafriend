package friendisnear.friendisnear;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import friendisnear.friendisnear.utilities.CommonUtility;
import friendisnear.friendisnear.utilities.Friend;

/**
 * Created by stieblj on 15.06.2017.
 */

public class FriendAdapter extends BaseAdapter {

    private Friend[] friends;
    private Friend user;
    private CommonUtility commons;
    private LayoutInflater friendInf;

    public FriendAdapter(Context c){
        //friends = CommonUtility.getInstance().getFriends();
        commons = CommonUtility.getInstance();
        updateFriends();
        updateUser();
        friendInf=LayoutInflater.from(c);
    }


    public void updateFriends() {
        friends = new Friend[commons.getFriends().size()];
        friends = commons.getFriends().values().toArray(friends);
    }

    //public void addFriend(Friend newFriend) { friends.add(newFriend); }

    @Override
    public int getCount() {
        return friends.length;
    }

    @Override
    public Object getItem(int position) {
        return null;
    }

    @Override
    public long getItemId(int position) {
        return 0;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        //map to song layout
        LinearLayout friendLay = (LinearLayout)friendInf.inflate
                (R.layout.friend, parent, false);
        //get title and artist views
        TextView friendName = (TextView)friendLay.findViewById(R.id.friend_name);
        TextView friendLocation = (TextView)friendLay.findViewById(R.id.friend_location);
        TextView friendDistance = (TextView)friendLay.findViewById(R.id.friend_distance);
        //get song using position
        Friend currFriend = friends[position];
        //get title and artist strings
        friendName.setText(currFriend.getName());
        if(currFriend.getLocation() != null) {
            friendLocation.setText(currFriend.getLocation().toString());
            friendDistance.setText(String.valueOf(currFriend.getDistanceTo(user)));
        }
        else friendLocation.setText("");
        //set position as tag
        friendLay.setTag(position);
        return friendLay;
    }


    public void updateUser() {
        this.user = commons.getUser();
    }
}
