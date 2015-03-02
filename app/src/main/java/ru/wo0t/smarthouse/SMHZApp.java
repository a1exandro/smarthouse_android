package ru.wo0t.smarthouse;

/**
 * Created by alex on 3/2/15.
 */


import android.app.Application;
import android.content.Context;
import android.util.Log;

import ru.wo0t.smarthouse.board.boardsManager;


public class SMHZApp extends Application {

    private boardsManager mBoardsManager;

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

}
