package friendisnear.friendisnear;

import java.lang.reflect.Array;
import java.util.ArrayList;

/**
 * Created by stieblj on 17.06.2017.
 */

class CommonUtility {
    private static final CommonUtility ourInstance = new CommonUtility();

    static CommonUtility getInstance() {
        return ourInstance;
    }

    private CommonUtility() {
        friends = new ArrayList<>();
    }

    private ArrayList<Friend> friends;

    public void addFriend(Friend friend) {
        friends.add(friend);
    }

    public ArrayList<Friend> getFriends() {return friends;}
}
