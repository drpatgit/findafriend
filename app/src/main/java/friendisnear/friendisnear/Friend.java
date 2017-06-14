package friendisnear.friendisnear;

import java.io.Serializable;
import java.io.StringReader;

/**
 * Created by stieblj on 14.06.2017.
 */

public class Friend implements Serializable {
    private String name;

    public Friend(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

}
