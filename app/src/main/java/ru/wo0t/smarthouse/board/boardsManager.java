package ru.wo0t.smarthouse.board;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import ru.wo0t.smarthouse.common.constants;

/**
 * Created by alex on 2/17/15.
 */
public class boardsManager {
    Context mContext;

    public static final String BROADCAST_MSG = "BROADCAST_MSG";

    public static final String BROADCAST_MSG_TYPE = "BROADCAST_MSG_TYPE";

    public static final String BOARD_ID = "BOARD_ID";
    public static final String BROADCAST_MSG_DESCR = "BROADCAST_MSG_DESCR";
    public static final String BOARD_CONNECTED = "BOARD_CONNECTED";
    public static final String BOARD_NEW_MESSAGE = "BOARD_NEW_MESSAGE";
    public static final String BOARD_CFG_CHANGED = "BOARD_CFG_CHANGED";

    public static final String BOARD_DATA = "BOARD_DATA";
    public static final String SENS_DATA = "SENS_DATA";

    public static final String SENSOR_ADDR = "SENSOR_ADDR";
    public static final String SENSOR_TYPE = "SENSOR_TYPE";
    public static final String SENSOR_VALUE = "SENSOR_VALUE";
    public static final String SENSOR_NAME = "SENSOR_NAME";

    private BroadcastReceiver onNotice= new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {

            Log.d("smhz","onReceive called: " + (intent.hasExtra(BROADCAST_MSG_TYPE)?intent.getStringExtra(BROADCAST_MSG_TYPE):"none"));

        }
    };

    public boardsManager(Context context) {
        mContext = context;
        IntentFilter iff= new IntentFilter(BROADCAST_MSG);
        LocalBroadcastManager.getInstance(context).registerReceiver(onNotice, iff);
    }

    public void lookUpForBoards(int remotePort, String login, String password) {
        boardsDiscover brdDiscover = new boardsDiscover(mHandler, boardsDiscover.LOOKUP_REMOTE_BOARD);

        brdDiscover.execute(remotePort, login, password);
    }

    public void connectToLocalBoard(int board_id, String board_name, String ip_addr) {
        try {
            new LocalBoard(mContext, AbstractBoard.BOARD_TYPE.LOCAL, board_id, board_name, ip_addr);
        } catch (Exception e) {
            Log.e("smhz", e.toString());
        }
    }

    public void connectToRemoteBoard(int board_id, String board_name, String login, String password) {
        try {
            new RemoteBoard(mContext, AbstractBoard.BOARD_TYPE.REMOTE,board_id,board_name,login,password);
        } catch (Exception e) {
            Log.e("smhz", e.toString());
        }
    }

    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {

                case constants.MESSAGE_NEW_BOARD:
                    JSONObject jObjectData = (JSONObject) msg.obj;
                    try {
                        if (jObjectData.get("board_type")== AbstractBoard.BOARD_TYPE.LOCAL) {
                            Log.i("smhz","Found board: " + jObjectData.getString("ip_addr") +", id: " + jObjectData.getString("board_id") +", type: " + (jObjectData.get("board_type")== AbstractBoard.BOARD_TYPE.LOCAL?"local":"remote"));
                            connectToLocalBoard(jObjectData.getInt("board_id"),jObjectData.getString("ip_addr"),jObjectData.getString("ip_addr"));
                        }
                        else {
                            Log.i("smhz","Found remote board" + " id: " + jObjectData.getString("board_id") +", type: " + (jObjectData.get("board_type")== AbstractBoard.BOARD_TYPE.LOCAL?"local":"remote"));
                           connectToRemoteBoard(jObjectData.getInt("board_id"),jObjectData.getString("descr"),jObjectData.getString("login"),jObjectData.getString("password") );
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    break;
                case constants.MESSAGE_DISCOVERY_FINISHED:
                    Log.i("smhz","Finishing boards discovery");
                    break;

            }
        }
    };
}
