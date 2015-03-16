package ru.wo0t.smarthouse.board;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.v4.content.LocalBroadcastManager;

import android.support.v4.util.ArrayMap;
import android.util.Log;
import android.widget.Toast;

import ru.wo0t.smarthouse.R;
import ru.wo0t.smarthouse.common.constants;

/**
 * Created by alex on 2/17/15.
 */

public class boardsManager {
    Context mContext;

    public static final String MSG_BOARD_CONNECTED = "MSG_BOARD_CONNECTED";
    public static final String MSG_BOARD_DISCONNECTED = "MSG_BOARD_DISCONNECTED";
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
    public static final String SENSOR_VALUE_OUT_OF_RANGE = "SENSOR_VALUE_OUT_OF_RANGE";

    private ArrayMap<Integer,AbstractBoard> mActiveBoards;
    private boardsDiscover mBrdDiscover;

    public boardsManager(Context context) {
        mContext = context;
        IntentFilter iff= new IntentFilter(MSG_BOARD_CONNECTED);
        iff.addAction(MSG_BOARD_DISCONNECTED);

        BroadcastReceiver onNotice = new BroadcastReceiver() {

            @Override
            public void onReceive(Context context, Intent intent) {

                int boardId = intent.getIntExtra(BOARD_ID,-1);
                AbstractBoard board = getBoard(boardId);

                switch (intent.getAction()) {
                    case MSG_BOARD_CONNECTED: {

                        AbstractBoard.BOARD_TYPE boardType = board.getBoardType();
                        String boardName = board.getBoardName();

                        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(mContext);

                        pref.edit().putInt(BOARD_ID, boardId).apply();
                        pref.edit().putString(BOARD_TYPE, boardType.toString()).apply();
                        pref.edit().putString(BOARD_DESCR, boardName).apply();

                        if (boardType == AbstractBoard.BOARD_TYPE.LOCAL) {
                            String ipAddr = ((LocalBoard)board).getIpAddr();
                            pref.edit().putString(BOARD_IP_ADDR, ipAddr).apply();
                        }
                        else {
                            String login = ((RemoteBoard)board).getLogin();
                            String password = ((RemoteBoard)board).getPassword();
                            pref.edit().putString(BOARD_LOGIN, login).apply();
                            pref.edit().putString(BOARD_PW, password).apply();
                        }
                        Toast.makeText(mContext, mContext.getString(R.string.successfullyConnectedToBoard) + "'" + boardName + "'", Toast.LENGTH_SHORT).show();
                    } break;
                    case MSG_BOARD_DISCONNECTED: {
                        String boardName = intent.getStringExtra(BOARD_DESCR);
                        Toast.makeText(mContext, mContext.getString(R.string.disconnectedFromBoard) + "'" + boardName+ "'", Toast.LENGTH_SHORT).show();
                    } break;
                    case SENSOR_VALUE_OUT_OF_RANGE: {

                    } break;
                }
            }
        };
        LocalBroadcastManager.getInstance(context).registerReceiver(onNotice, iff);

        mActiveBoards = new ArrayMap<>();
    }

    public void connectToDefaultBoard() {
        try {
            SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(mContext);
            int boardId = pref.getInt(boardsManager.BOARD_ID, -1);
            AbstractBoard.BOARD_TYPE boardType = AbstractBoard.BOARD_TYPE.valueOf(pref.getString(BOARD_TYPE, ""));
            String boardName = pref.getString(BOARD_DESCR,"");
            Log.d(constants.APP_TAG, "Connection to default board requested: " + boardName);
            if (boardType == AbstractBoard.BOARD_TYPE.LOCAL) {
                String ipAddr = pref.getString(BOARD_IP_ADDR,"");
                connectToLocalBoard(boardId,boardName,ipAddr);
            }
            else {
                String login = pref.getString(BOARD_LOGIN,"");
                String password = pref.getString(BOARD_PW,"");
                connectToRemoteBoard(boardId, boardName, login, password);
            }
        }
        catch (Exception e) { e.printStackTrace(); }
    }
    public void lookUpForBoards() {
        try {
            SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(mContext);
            String login = pref.getString("remote_login","");
            String password = pref.getString("remote_password","");
            lookUpForBoards(constants.LOCAL_BOARD_PORT, login, password);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void closeBoardsLookup() {
        mBrdDiscover.close();
        mBrdDiscover.cancel(false);
    }
    public void lookUpForBoards(int remotePort, String login, String password) {
        mBrdDiscover = new boardsDiscover(mContext,boardsDiscover.LOOKUP_ALL_BOARDS);
        mBrdDiscover.execute(remotePort, login, password);
    }

    public void connectToLocalBoard(int board_id, String board_name, String ip_addr) {
        try {
            AbstractBoard board = new LocalBoard(mContext, AbstractBoard.BOARD_TYPE.LOCAL, board_id, board_name, ip_addr);
            mActiveBoards.put(board_id, board);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void connectToRemoteBoard(int board_id, String board_name, String login, String password) {
        try {
            AbstractBoard board = new RemoteBoard(mContext, AbstractBoard.BOARD_TYPE.REMOTE,board_id,board_name,login,password);
            mActiveBoards.put(board_id, board);
        } catch (Exception e) {
            e.printStackTrace();
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
