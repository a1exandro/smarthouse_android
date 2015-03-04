package ru.wo0t.smarthouse.board;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.support.v4.content.LocalBroadcastManager;

import android.support.v4.util.ArrayMap;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import ru.wo0t.smarthouse.ui.BoardsLookupActivity;
import ru.wo0t.smarthouse.common.constants;

/**
 * Created by alex on 2/17/15.
 */

public class boardsManager {
    Context mContext;

    public static final String MSG_BOARD_CONNECTED = "MSG_BOARD_CONNECTED";
    public static final String MSG_BOARD_NEW_MESSAGE = "MSG_BOARD_NEW_MESSAGE";
    public static final String MSG_BOARD_CFG_CHANGED = "MSG_BOARD_CFG_CHANGED";
    public static final String MSG_SENSOR_DATA = "MSG_SENSOR_DATA";
    public static final String MSG_FOUND_NEW_BOARD = "MSG_FOUND_NEW_BOARD";
    public static final String MSG_BOARDS_DISCOVERY_FINISHED = "MSG_BOARDS_DISCOVERY_FINISHED";
    public static final String MSG_SYSTEM_NAME = "MSG_SYSTEM_NAME";

    public static final String BOARD_ID = "BOARD_ID";
    public static final String BOARD_TYPE = "BOARD_TYPE";
    public static final String BOARD_IP_ADDR = "BOARD_IP_ADDR";
    public static final String BOARD_LOGIN = "BOARD_LOGIN";
    public static final String BOARD_PW = "BOARD_PW";
    public static final String BOARD_DESCR = "BOARD_DESCR";

    public static final String SENSOR_ADDR = "SENSOR_ADDR";
    public static final String SENSOR_TYPE = "SENSOR_TYPE";
    public static final String SENSOR_VALUE = "SENSOR_VALUE";
    public static final String SENSOR_NAME = "SENSOR_NAME";

    private ArrayMap<Integer,AbstractBoard> mActiveBoards;

    public boardsManager(Context context) {
        mContext = context;
        IntentFilter iff= new IntentFilter(MSG_BOARD_CONNECTED);
        iff.addAction(MSG_BOARD_NEW_MESSAGE);
        iff.addAction(MSG_BOARD_CFG_CHANGED);
        iff.addAction(MSG_SENSOR_DATA);

        BroadcastReceiver onNotice = new BroadcastReceiver() {

            @Override
            public void onReceive(Context context, Intent intent) {

                //Log.d(constants.APP_TAG, "BROADCAST RECV: " + intent.getAction());
            }
        };
        LocalBroadcastManager.getInstance(context).registerReceiver(onNotice, iff);

        mActiveBoards = new ArrayMap<>();
    }

    public void lookUpForBoards() {
        try {
            SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(mContext);
            String login = pref.getString("remote_login","");
            String password = pref.getString("remote_password","");
            lookUpForBoards(constants.LOCAL_BOARD_PORT, login, password);
        } catch (Exception e) {
            Log.e(constants.APP_TAG, e.toString());
        }
    }

    public void lookUpForBoards(int remotePort, String login, String password) {
        boardsDiscover brdDiscover = new boardsDiscover(mContext,boardsDiscover.LOOKUP_ALL_BOARDS);

        brdDiscover.execute(remotePort, login, password);
    }

    public void connectToLocalBoard(int board_id, String board_name, String ip_addr) {
        try {
            AbstractBoard board = new LocalBoard(mContext, AbstractBoard.BOARD_TYPE.LOCAL, board_id, board_name, ip_addr);
            mActiveBoards.put(board_id, board);
        } catch (Exception e) {
            Log.e(constants.APP_TAG, e.toString());
        }
    }

    public void connectToRemoteBoard(int board_id, String board_name, String login, String password) {
        try {
            AbstractBoard board = new RemoteBoard(mContext, AbstractBoard.BOARD_TYPE.REMOTE,board_id,board_name,login,password);
            mActiveBoards.put(board_id, board);
        } catch (Exception e) {
            Log.e(constants.APP_TAG, e.toString());
        }
    }

    public void closeBoard(int board_id) {
        AbstractBoard board = mActiveBoards.get(board_id);
        board.close();
        mActiveBoards.remove(board_id);
        Log.d(constants.APP_TAG, "Close board requested: " + board.mBoardName);
    }

    public AbstractBoard getBoard(int aBoardId) {
        return mActiveBoards.get(aBoardId);
    }
}
