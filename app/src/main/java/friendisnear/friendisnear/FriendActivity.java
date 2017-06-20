package friendisnear.friendisnear;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import friendisnear.friendisnear.utilities.CommonUtility;
import friendisnear.friendisnear.utilities.Friend;

/**
 * Created by Patrick on 09.06.2017.
 */

public class FriendActivity extends AppCompatActivity {

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_friend);

        final EditText editText10 = (EditText) findViewById(R.id.editText10);

        Button button4 = (Button) findViewById(R.id.button4);
        button4.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!editText10.getText().toString().equals("")){
                    Intent returnIntent = new Intent();
                    Friend newFriend = new Friend(editText10.getText().toString());
                    CommonUtility.getInstance().request(newFriend, CommonUtility.CommonAction.FRIEND_REQUEST);
                    //returnIntent.putExtra("result", newFriend);
                    //setResult(Activity.RESULT_OK, returnIntent);
                }
                finish();
            }
        });

        Button button5 = (Button) findViewById(R.id.button5);
        button5.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view){
                Intent returnIntent = new Intent();
                setResult(Activity.RESULT_CANCELED, returnIntent);
                finish();
            }
        });
    }
}
