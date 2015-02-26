package ru.wo0t.smarthouse.board;

import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import ru.wo0t.smarthouse.common.constants;

/**
 * Created by alex on 2/17/15.
 */
public class boardsManager {
    public void lookUpForBoards(int remotePort, String login, String password) {
        boardsDiscover brdDiscover = new boardsDiscover(mHandler, boardsDiscover.LOOKUP_ALL_BOARDS);

        brdDiscover.execute(remotePort, login, password);
    }

    public void connectToLocalBoard(int board_id, String board_name, String ip_addr) {
        try {
            new LocalBoard(AbstractBoard.BOARD_TYPE.LOCAL, board_id, board_name, ip_addr);
        } catch (Exception e) {
            Log.e("smhz", e.toString());
        }
    }

    public void connectToRemoteBoard(int board_id, String board_name, String login, String password) {
        try {
            new RemoteBoard(AbstractBoard.BOARD_TYPE.REMOTE,board_id,board_name,login,password);
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
