package ru.wo0t.smarthouse;

/**
 * Created by alex on 3/2/15.
 */


import android.app.Application;
import android.content.Context;

import ru.wo0t.smarthouse.board.boardsManager;


public class SMHZApp extends Application {

    private boardsManager mBoardsManager;
    private int mBoardId = -1;
    private Context mMainActivity;

    @Override
    public void onCreate() {
        super.onCreate();
        mBoardsManager = new boardsManager(this);
    }

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
}
