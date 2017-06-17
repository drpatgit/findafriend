package friendisnear.friendisnear.utilities;

import friendisnear.friendisnear.utilities.Friend;
import friendisnear.friendisnear.utilities.CommonUtility;
import friendisnear.friendisnear.utilities.CommonUtility.FriendAction;

/**
 * Created by stieblj on 17.06.2017.
 */

public interface FriendsChangedListener {

    public void onFriendsChanged(Friend f, FriendAction action);
}
