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

import ru.wo0t.smarthouse.board.boardsManager;


public class SMHZApp extends Application {

    private boardsManager mBoardsManager;
    private int mBoardId = -1;
    private Context mMainActivity;
    private boolean mIsMainActivityVisible = false;

    @Override
    public void onCreate() {
        Intent intent = new Intent(this, boardsManager.class);
        bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
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
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {

        }
    };

}
