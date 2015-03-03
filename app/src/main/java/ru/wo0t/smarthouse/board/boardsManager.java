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

    public static final String BROADCAST_MSG_DESCR = "BROADCAST_MSG_DESCR";

    public static final String MSG_BOARD_CONNECTED = "MSG_BOARD_CONNECTED";
    public static final String MSG_BOARD_NEW_MESSAGE = "MSG_BOARD_NEW_MESSAGE";
    public static final String MSG_BOARD_CFG_CHANGED = "MSG_BOARD_CFG_CHANGED";
    public static final String MSG_SENSOR_DATA = "MSG_SENSOR_DATA";

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

    private BroadcastReceiver onNotice= new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {

            Log.d(constants.APP_TAG,"BROADCAST RECV: " + intent.getAction());

        }
    };

    private void sendBroadcastMsg(Bundle args) {
        Intent intent = new Intent(MSG_BOARD_CONNECTED);
        intent.putExtras(args);
        LocalBroadcastManager.getInstance(mContext).sendBroadcast(intent);
    }

    private void sendBroadcastMsg(String event, Bundle args) {
        Intent intent = new Intent(event);
        intent.putExtras(args);
        LocalBroadcastManager.getInstance(mContext).sendBroadcast(intent);
    }

    public boardsManager(Context context) {
        mContext = context;
        IntentFilter iff= new IntentFilter(MSG_BOARD_CONNECTED);
        iff.addAction(MSG_BOARD_NEW_MESSAGE);
        iff.addAction(MSG_BOARD_CFG_CHANGED);
        iff.addAction(MSG_SENSOR_DATA);
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
        boardsDiscover brdDiscover = new boardsDiscover(mHandler, boardsDiscover.LOOKUP_ALL_BOARDS);

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
    }

    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {

                case constants.MESSAGE_NEW_BOARD:
                    JSONObject jObjectData = (JSONObject) msg.obj;
                    try {
                        Bundle args = new Bundle();
                        args.putInt(boardsManager.BOARD_ID, jObjectData.getInt("board_id"));
                        args.putString(boardsManager.BOARD_TYPE, ((AbstractBoard.BOARD_TYPE) jObjectData.get("board_type")).toString());
                        args.putString(BOARD_DESCR, jObjectData.getString("descr") );

                        if (jObjectData.get("board_type")== AbstractBoard.BOARD_TYPE.LOCAL) {
                            //Log.i(constants.APP_TAG,"Found board: " + jObjectData.getString("ip_addr") +", id: " + jObjectData.getString("board_id") +", type: " + (jObjectData.get("board_type")== AbstractBoard.BOARD_TYPE.LOCAL?"local":"remote"));
                            args.putString(BOARD_IP_ADDR, jObjectData.getString("ip_addr") );

                            //connectToLocalBoard(jObjectData.getInt("board_id"),jObjectData.getString("ip_addr"),jObjectData.getString("ip_addr"));
                        }
                        else {
                           // Log.i(constants.APP_TAG,"Found remote board" + " id: " + jObjectData.getString("board_id") +", type: " + (jObjectData.get("board_type")== AbstractBoard.BOARD_TYPE.LOCAL?"local":"remote"));
                           //connectToRemoteBoard(jObjectData.getInt("board_id"),jObjectData.getString("descr"),jObjectData.getString("login"),jObjectData.getString("password") );
                            args.putString(BOARD_LOGIN, jObjectData.getString("login") );
                            args.putString(BOARD_PW, jObjectData.getString("password") );

                        }
                        sendBroadcastMsg(BoardsLookupActivity.FOUND_NEW_BOARD,args);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    break;
                case constants.MESSAGE_DISCOVERY_FINISHED:
                    sendBroadcastMsg(BoardsLookupActivity.BOARDS_DISCOVERY_FINISHED, new Bundle());
                    break;

            }
        }
    };
}
