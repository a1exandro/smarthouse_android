package ru.wo0t.smarthouse.board;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.media.MediaPlayer;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Binder;
import android.os.IBinder;
import android.os.Vibrator;
import android.preference.PreferenceManager;
import android.support.v4.content.LocalBroadcastManager;

import android.support.v4.util.ArrayMap;
import android.util.Log;
import android.widget.Toast;

import ru.wo0t.smarthouse.R;
import ru.wo0t.smarthouse.SMHZApp;
import ru.wo0t.smarthouse.common.constants;
import ru.wo0t.smarthouse.ui.MainActivity;

/**
 * Created by alex on 2/17/15.
 */

public class boardsManager extends Service {

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
    private boardsDiscoverer mBrdDiscover;

    private int mNotificationsCount = 1;
    private boolean mIsInitialized = false;
    private final IBinder mBinder = new LocalBinder();

    public class LocalBinder extends Binder {
        public boardsManager getService() {
            return boardsManager.this;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        if (!mIsInitialized) InitService();
        return mBinder;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        if (!mIsInitialized) InitService();
        return super.onStartCommand(intent, flags, startId);
    }

    public void InitService()
    {
        synchronized (this) { mIsInitialized = true; }

        IntentFilter iff= new IntentFilter(MSG_BOARD_CONNECTED);
        iff.addAction(MSG_BOARD_DISCONNECTED);
        iff.addAction(SENSOR_VALUE_OUT_OF_RANGE);
        LocalBroadcastManager.getInstance(this).registerReceiver(onNotice, iff);
        connectToDefaultBoard();
    }
    public boardsManager() {
        mActiveBoards = new ArrayMap<>();
    }

    public void connectToDefaultBoard() {
        try {
            SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(this);
            int boardId = pref.getInt(boardsManager.BOARD_ID, -1);
            if (boardId == -1) return;
            AbstractBoard.BOARD_TYPE boardType = AbstractBoard.BOARD_TYPE.valueOf(pref.getString(BOARD_TYPE, ""));
            String boardName = pref.getString(BOARD_DESCR,"");
            Log.d(constants.APP_TAG, "Connection to default board requested: " + boardName);
            if (boardType == AbstractBoard.BOARD_TYPE.LOCAL) {
                String ipAddr = pref.getString(BOARD_IP_ADDR,"");
                connectToLocalBoard(boardId,boardName,ipAddr);
            }
            else {
                String login = pref.getString(BOARD_LOGIN, "");
                String password = pref.getString(BOARD_PW, "");
                connectToRemoteBoard(boardId, boardName, login, password);
            }
        }
        catch (Exception e) { e.printStackTrace(); }
    }
    public void lookUpForBoards() {
        try {
            SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(this);
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
        mBrdDiscover = new boardsDiscoverer(this, boardsDiscoverer.LOOKUP_ALL_BOARDS);
        mBrdDiscover.execute(remotePort, login, password);
    }

    public void connectToLocalBoard(int board_id, String board_name, String ip_addr) {
        try {
            AbstractBoard board = new LocalBoard(this, AbstractBoard.BOARD_TYPE.LOCAL, board_id, board_name, ip_addr);
            mActiveBoards.put(board_id, board);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void connectToRemoteBoard(int board_id, String board_name, String login, String password) {
        try {
            AbstractBoard board = new RemoteBoard(this, AbstractBoard.BOARD_TYPE.REMOTE,board_id,board_name,login,password);
            mActiveBoards.put(board_id, board);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void closeBoard(int board_id) {
        AbstractBoard board = mActiveBoards.get(board_id);
        if (board == null) {
            Log.d(constants.APP_TAG, "Error closing board " + board_id);
            return;
        }
        board.close();
        mActiveBoards.remove(board_id);
        Log.d(constants.APP_TAG, "Board closing requested: " + board.mBoardName);
    }

    public AbstractBoard getBoard(int aBoardId) {
        return mActiveBoards.get(aBoardId);
    }

    BroadcastReceiver onNotice = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {

            int boardId = intent.getIntExtra(BOARD_ID,-1);
            AbstractBoard board = getBoard(boardId);

            switch (intent.getAction()) {
                case MSG_BOARD_CONNECTED: {

                    AbstractBoard.BOARD_TYPE boardType = board.getBoardType();
                    String boardName = board.getBoardName();

                    SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());

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
                    Toast.makeText(getApplicationContext(), getString(R.string.successfullyConnectedToBoard) + "'" + boardName + "'", Toast.LENGTH_SHORT).show();
                } break;
                case MSG_BOARD_DISCONNECTED: {
                    String boardName = intent.getStringExtra(BOARD_DESCR);
                    Toast.makeText(getApplicationContext(), getString(R.string.disconnectedFromBoard) + "'" + boardName+ "'", Toast.LENGTH_SHORT).show();
                } break;
                case SENSOR_VALUE_OUT_OF_RANGE: {
                    try {
                        String sensName = intent.getStringExtra(SENSOR_NAME);

                        final long[] SENS_OUT_OF_RANGE_VIBRATE = new long[]{1000, 1000, 1000};

                        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());

                        boolean prefAlarms = pref.getBoolean("alarms", true);
                        if (!prefAlarms) break;

                        String prefRingtone = pref.getString("alarm_sound", "not_set");
                        boolean prefVibrate = pref.getBoolean("alarm_vibro", true);

                        Uri ringURI;
                        if (prefRingtone.equals("not_set"))
                            ringURI = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
                        else
                            ringURI = Uri.parse(prefRingtone);

                        if (((SMHZApp)getApplicationContext()).isMainActivityVisible()) {
                            Toast.makeText(getApplicationContext(), getString(R.string.sensCriticalValue) + " '" + sensName + "'", Toast.LENGTH_LONG).show();

                            if (!prefRingtone.isEmpty()) {
                                final MediaPlayer mp = MediaPlayer.create(getApplicationContext(), ringURI);
                                mp.start();
                            }

                            if (prefVibrate) {
                                Vibrator v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
                                v.vibrate(SENS_OUT_OF_RANGE_VIBRATE, -1);
                            }
                        }
                        else {

                            Intent notificationIntent = new Intent(context, MainActivity.class);
                            notificationIntent.putExtra(SENSOR_NAME, sensName);

                            PendingIntent contentIntent = PendingIntent.getActivity(context,
                                    0, notificationIntent,
                                    PendingIntent.FLAG_UPDATE_CURRENT);

                            Resources res = context.getResources();
                            Notification.Builder builder = new Notification.Builder(context);

                            builder.setContentIntent(contentIntent)
                                    .setSmallIcon(R.mipmap.ic_notify_sens_out_of_range)
                                    .setLargeIcon(BitmapFactory.decodeResource(res, R.mipmap.ic_notify_sens_out_of_range));

                            if (!prefRingtone.isEmpty()) builder.setSound(ringURI);
                            if (prefVibrate) builder.setVibrate(SENS_OUT_OF_RANGE_VIBRATE);

                            builder.setTicker(context.getString(R.string.sensCriticalValue))
                                    .setWhen(System.currentTimeMillis())
                                    .setAutoCancel(true)
                                    .setLights(Color.RED, 2000, 1000)
                                    .setContentTitle(context.getString(R.string.sensCriticalValue))
                                    .setContentText(sensName + ": " + board.getSens(sensName).getStringValue(getApplicationContext()));

                            Notification notification = builder.build();

                            notification.flags |= Notification.FLAG_SHOW_LIGHTS;

                            NotificationManager notificationManager = (NotificationManager) context
                                    .getSystemService(Context.NOTIFICATION_SERVICE);
                            notificationManager.notify(mNotificationsCount++, notification);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                } break;
            }
        }
    };
}
