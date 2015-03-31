package ru.wo0t.smarthouse;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import ru.wo0t.smarthouse.board.boardsManager;
import ru.wo0t.smarthouse.common.constants;

/**
 * Created by alex on 3/26/15.
 */
public class BootCompletedReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent arg1) {
        Log.d(constants.APP_TAG, "Starting service...");
    }

}