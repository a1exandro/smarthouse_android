package ru.wo0t.smarthouse;

/**
 * Created by alex on 3/2/15.
 */


import android.app.Application;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import ru.wo0t.smarthouse.board.boardsManager;
import ru.wo0t.smarthouse.common.constants;


public class SMHZApp extends Application {

    private boardsManager mBoardsManager = null;
    private int mBoardId = -1;
    private Context mMainActivity;
    private boolean mIsMainActivityVisible = false;

    @Override
    public void onCreate() {
        Log.d(constants.APP_TAG, "Starting the program...");
        Intent pushIntent = new Intent(this, boardsManager.class);
        startService(pushIntent);
    }

    @Override
    public void onTerminate() {
        super.onTerminate();
        Intent intent = new Intent(this, boardsManager.class);
        stopService(intent);
    }

    public void bindToService() {
        if (mBoardsManager == null) {
            Intent intent = new Intent(this, boardsManager.class);
            bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
        }
        else {
            Intent intent = new Intent(boardsManager.MSG_SERVICE_BOUND);
            LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(intent);
        }
    }

    public void setMainActivityVisibility(boolean visible) { mIsMainActivityVisible = visible; }
    public boolean isMainActivityVisible() { return mIsMainActivityVisible; }

    public boardsManager getBoardsManager() {
        return mBoardsManager;
    }

    public static SMHZApp getApplication(Context context) {
        if (context instanceof SMHZApp) {
            return (SMHZApp) context;
        }
        return (SMHZApp) context.getApplicationContext();
    }

    public int getBoardId() { return mBoardId; }
    public void setBoardId(int aBoardId) { mBoardId = aBoardId; }

    public void setMainActivity(Context context) { mMainActivity = context; }
    public Context getMainActivity() { return mMainActivity; }

    private ServiceConnection mConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className,
                                       IBinder service) {
            boardsManager.LocalBinder binder = (boardsManager.LocalBinder) service;
            mBoardsManager = binder.getService();

            Intent intent = new Intent(boardsManager.MSG_SERVICE_BOUND);
            LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(intent);
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {

        }
    };

}
